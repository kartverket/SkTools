package no.statkart.sktools.gradle.plugins.wsdocgen

import no.statkart.sktools.gradle.plugins.wsdocgen.tasks.WsDocCompileTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact

import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.artifacts.Configuration
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.util.GUtil

import org.gradle.api.GradleException
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.artifacts.Dependency

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
    final public static String GEN_TASK_NAME = String.format(WsDocGenConvention.GEN_TASK_NAME_PATTERN, '', '')
    final public static String ARCHIVE_TASK_NAME = 'packWsDoc'


    @Override
    void apply(Project project) {
        project.apply plugin: JavaBasePlugin.class

        final WsDocGenConvention wsDocGenConvention = new WsDocGenConvention(project)
        project.convention.plugins.put(WsDocGenPlugin.CONVENTION_NAME, wsDocGenConvention);

        //creates the task
        Task genWsDocTask = project.task(WsDocGenPlugin.GEN_TASK_NAME)


        configureArchives(wsDocGenConvention)

        //setting defaults if not already configured
        project.afterEvaluate {
            setConventionalDefaults(wsDocGenConvention)
            addTasks(wsDocGenConvention, project.getTasks().getByName(WsDocGenPlugin.GEN_TASK_NAME))
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
            JavaPluginConvention javaConvention = project.getConvention().getPlugins().get("java");
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

    private static void addTasks(final WsDocGenConvention wsDocGenConvention, Task genWsDocTask) {
        final Project project = wsDocGenConvention.project

        String genWsDocTaskNameForSourceSet = String.format(WsDocGenConvention.GEN_TASK_NAME_PATTERN, GUtil.toCamelCase(wsDocGenConvention.sourceSetName), '')
        wsDocGenConvention.genDocTaskName = genWsDocTaskNameForSourceSet

        //samletask for alle grupper for dette source settet
        final Task sourceSetTask = project.task(wsDocGenConvention.genDocTaskName)
        genWsDocTask.dependsOn(genWsDocTaskNameForSourceSet)


        JavaPluginConvention javaConvention = project.getConvention().getPlugins().get("java");
        final SourceSet sourceSet = javaConvention.getSourceSets().getByName(wsDocGenConvention.sourceSetName)



        wsDocGenConvention.groups.eachWithIndex { Group group, int i ->
            if (group.name == null) {
                group.name = "Group${i+1}"
            }

            String taskName = String.format(WsDocGenConvention.GEN_TASK_NAME_PATTERN, GUtil.toCamelCase(wsDocGenConvention.sourceSetName), group.name)
            WsDocCompileTask task = (WsDocCompileTask) project.task(type: WsDocCompileTask.class, taskName)

            //setting conventional properties
            task.getConventionMapping().with {
                map("source", new Callable() {   //tildeler en dynamisk default verdi
                    public Object call() {
                        return sourceSet.getAllJava();  //default source
                    }
                });
                map("docGroup", new Callable() {
                    public Object call() {
                        return group;
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


    private static void configureArchives(final WsDocGenConvention wsDocGenConvention) {
        final Project project = wsDocGenConvention.project


        Zip zip = (Zip) project.task(type: Zip, ARCHIVE_TASK_NAME)
        zip.setClassifier(WsDocGenPlugin.CONVENTION_NAME)
        zip.from(project.getTasks().withType(WsDocCompileTask.class)) //legger dynamiskt til alle definerte output kataloger for 'genWsDocTask'

        ArchivePublishArtifact artifact = new ArchivePublishArtifact(zip)
        project.getArtifacts().add(Dependency.ARCHIVES_CONFIGURATION, artifact);

    }


}
