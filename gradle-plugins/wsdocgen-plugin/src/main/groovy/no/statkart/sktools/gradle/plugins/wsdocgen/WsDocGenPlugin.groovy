package no.statkart.sktools.gradle.plugins.wsdocgen

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.util.GUtil

import java.util.concurrent.Callable

/**
 * Dokumentasjon-generering av {@code *WSBean.java} - JAX-WS implementasjon på server.
 *
 *
 * @since 1.0
 * @author Leif Lislegård
 */
class WsDocGenPlugin implements Plugin<Project> {

    final public static String CONVENTION_NAME = 'wsdoc'
    final public static String GEN_TASK_NAME = 'genWsDoc'
    final public static String ARCHIVE_TASK_NAME = 'packWsDoc'


    @Override
    void apply(Project project) {
        project.apply plugin: JavaBasePlugin.class

        final WsDocGenConvention wsDocGenConvention = new WsDocGenConvention(project)
        project.convention.plugins.put(CONVENTION_NAME, wsDocGenConvention);

        //common super task
        configureGenTask(project)

        configureArchives(wsDocGenConvention)

        //setting defaults if not already configured
        project.afterEvaluate {
            setConventionalDefaults(wsDocGenConvention)
            addTasks(wsDocGenConvention)
        }
    }


    /**
     * Legger til evt defaultverdier.
     *
     * Dersom ikke noen konfigurasjon angis, opprettes en tom {@link Group} med standard verdier.
     */
    private static setConventionalDefaults(WsDocGenConvention wsDocGenConvention) {
        final Project project = wsDocGenConvention.project

        if (wsDocGenConvention.groups.isEmpty()) {
            wsDocGenConvention.docGroup {}
        }

        if (wsDocGenConvention.sourceSetName == null) {
            JavaPluginConvention javaConvention = project.getConvention().getPlugins().get("java") as JavaPluginConvention;
            SortedMap<String, SourceSet> sourceSetMap = javaConvention.getSourceSets().getAsMap();

            if (sourceSetMap.isEmpty()) {
                throw new GradleException('No source set found for plugin ' + WsDocGenPlugin.class.getSimpleName());
            }
            //defaults to 'main' source set - or the first one found.
            SourceSet sourceSet = sourceSetMap.containsKey(SourceSet.MAIN_SOURCE_SET_NAME) ? sourceSetMap.get(SourceSet.MAIN_SOURCE_SET_NAME) : sourceSetMap.values().iterator().next();
            wsDocGenConvention.sourceSetName = sourceSet.getName();
        }

        wsDocGenConvention.groups.each {
            if (it.includes == null) {
                it.include('**/*Bean.java')
            }
            if (it.targetDir == null) {
                it.targetPath("${project.relativePath(project.buildDir)}/${wsDocGenConvention.sourceSetName}/docs/wsdoc")
            }
        }

    }

    private static void addTasks(final WsDocGenConvention wsDocGenConvention) {
        final Project project = wsDocGenConvention.project
        final AbstractArchiveTask archiveTask = (AbstractArchiveTask) project.getTasks().getByName(ARCHIVE_TASK_NAME);


        //samletask for alle grupper for dette source settet
        final Task sourceSetTask = project.task(String.format(WsDocGenConvention.GEN_TASK_NAME_PATTERN, GUtil.toCamelCase(wsDocGenConvention.sourceSetName), ''))
        project.getTasks().getByName(GEN_TASK_NAME).dependsOn(sourceSetTask.name)


        final JavaPluginConvention javaConvention = (JavaPluginConvention) project.getConvention().getPlugins().get("java");
        final SourceSet sourceSet = javaConvention.getSourceSets().getByName(wsDocGenConvention.sourceSetName)

        wsDocGenConvention.groups.eachWithIndex { Group group, int i ->
            if (group.name == null) {
                group.name = "Group${i+1}"
            }

            AbstractCompile task = createWsDocGenForGroupTask(group)
            //setting conventional properties
            task.getConventionMapping().with {
                map("source", new Callable() {   //tildeler en dynamisk default verdi
                    public Object call() {
                        return sourceSet.getAllJava();  //default source
                    }
                });
                map("classpath", new Callable() {
                    public Object call() {
                        return sourceSet.getCompileClasspath();
                    }
                });
                map("destinationDir", new Callable() {
                    public Object call() {
                        return group.targetDir;
                    }
                });
            }

            sourceSetTask.dependsOn(task)
            archiveTask.from(task)
        }

    }


    public static FileCollection findPluginClasspath(final Project project) {

        Closure<Boolean> pluginDependencyMatcher = {Dependency dependency -> dependency.getGroup() == 'no.statkart.sktools.gradle' && dependency.getName() == 'wsdocgen-plugin'}
        Closure<Boolean> samlepomDependencyMatcher = {Dependency dependency -> dependency.getGroup() == 'no.statkart.sktools.gradle' && dependency.getName() == 'gradle-plugins'}

        List<FileCollection> candidateFileCollections = [project, project.getRootProject()].collect {
            FileCollection resolvedFiles = project.files();
            Configuration buildConfiguration = it.getBuildscript().getConfigurations().getByName(ScriptHandler.CLASSPATH_CONFIGURATION);

            Dependency pluginDependency = buildConfiguration.dependencies.find (pluginDependencyMatcher);
            if (!pluginDependency.is(null)) {
                resolvedFiles = buildConfiguration.fileCollection(pluginDependency);
            } else {
                //forsøker å finne 'samlepom' [SKIF-154]
                Dependency samlepomDependency = buildConfiguration.dependencies.find (samlepomDependencyMatcher);
                if (!samlepomDependency.is(null)) {
                    resolvedFiles = buildConfiguration.fileCollection(samlepomDependency);
                }
            }
//            println "resolved files: ${resolvedFiles.files}"
            return resolvedFiles;
        }

        FileCollection candidate = candidateFileCollections.find { !it.isEmpty() }

        return candidate;
    }


    static def void configureGenTask(Project project) {
        project.task(GEN_TASK_NAME)
    }

    private static Zip configureArchives(final WsDocGenConvention wsDocGenConvention) {
        final Project project = wsDocGenConvention.project

        Zip zip = (Zip) project.tasks.create(ARCHIVE_TASK_NAME, Zip.class)
        zip.setClassifier(CONVENTION_NAME)

        project.getArtifacts().add(Dependency.ARCHIVES_CONFIGURATION, new ArchivePublishArtifact(zip));
        return zip;
    }


    def static AbstractCompile createWsDocGenForGroupTask(final Group docGroup) {
        final Project project = docGroup.project
        final String taskName = String.format(WsDocGenConvention.GEN_TASK_NAME_PATTERN, GUtil.toCamelCase(docGroup.convention.sourceSetName), docGroup.name)
        final WsDocCompileTask task = project.tasks.create(taskName, WsDocCompileTask.class)
        task.setDocGroup(docGroup);

        return task
    }

}
