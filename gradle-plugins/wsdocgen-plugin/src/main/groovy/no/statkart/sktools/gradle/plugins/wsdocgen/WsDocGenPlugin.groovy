package no.statkart.sktools.gradle.plugins.wsdocgen

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact

import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceTask
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
import org.gradle.api.internal.file.collections.SimpleFileCollection
import java.util.concurrent.Callable

/**
 * Dokumentasjon-generering av WSBean.java - JAX-WS implemntasjon på server.
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 */
class WsDocGenPlugin implements Plugin<Project> {

    final public static String CONVENTION_NAME = 'wsdoc'
    final public static String CONFIGURATION_NAME = 'wsdoc'
    final public static String GEN_TASK_NAME = String.format(WsDocGenConvention.GEN_TASK_NAME_PATTERN, '')
    final public static String ARCHIVE_TASK_NAME = 'packWsDoc'


    @Override
    void apply(Project project) {
        project.apply plugin: JavaBasePlugin.class

        final WsDocGenConvention wsDocGenConvention = new WsDocGenConvention(project)
        project.convention.plugins.put(WsDocGenPlugin.CONVENTION_NAME, wsDocGenConvention);

        final Configuration configuration = createConfiguration(project)

        //creates the task
        Task genWsDocTask = project.task(WsDocGenPlugin.GEN_TASK_NAME)


        configureArchives(wsDocGenConvention)

        //setting defaults if not already configured
        project.afterEvaluate {
            setConventionalDefaults(wsDocGenConvention)
            addTasks(wsDocGenConvention, project.getTasks().getByName(WsDocGenPlugin.GEN_TASK_NAME))
        }
    }


    private Configuration createConfiguration(Project project) {

        //Trenger denne classpathen for innkobling av {@link no.statkart.grunnbok.tools.docgen.ws.WebserviceAnnotationProcessorFactory}
        return project.configurations.add(WsDocGenPlugin.CONFIGURATION_NAME).setTransitive(false).setDescription('Webservice documentation artifact');
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

        Configuration configuration = project.configurations.getByName(WsDocGenPlugin.CONFIGURATION_NAME)

        //default classpath for apt task - benytter jar dependency for hvor denne pluginen befinner seg
        if (configuration.getDependencies().isEmpty()) {
            project.getDependencies().add(WsDocGenPlugin.CONFIGURATION_NAME, wsDocGenConvention.project.buildscript.configurations.getByName(ScriptHandler.CLASSPATH_CONFIGURATION).getAsFileTree())
        }

    }

    private static void addTasks(final WsDocGenConvention wsDocGenConvention, Task genWsDocTask) {
        final Project project = wsDocGenConvention.project

        String genWsDocTaskNameForSourceSet = String.format(WsDocGenConvention.GEN_TASK_NAME_PATTERN, GUtil.toCamelCase(wsDocGenConvention.sourceSetName))
        wsDocGenConvention.genDocTaskName = genWsDocTaskNameForSourceSet


        JavaPluginConvention javaConvention = project.getConvention().getPlugins().get("java");
        final SourceSet sourceSet = javaConvention.getSourceSets().getByName(wsDocGenConvention.sourceSetName)


        SourceTask task = (SourceTask) project.task(type: WsDocGenTask.class, genWsDocTaskNameForSourceSet)

        //setting conventional properties
        task.getConventionMapping().with {
            map("source", new Callable() {   //tildeler en dynamisk default verdi
                public Object call() {
                    return sourceSet.getAllJava();  //default source
                }
            });
            map("groups", new Callable() {
                public Object call() {
                    return wsDocGenConvention.groups;
                }
            });
            map("classpath", new Callable() {
                public Object call() {
                    return new UnionFileCollection(findPluginClasspath(project), project.getConfigurations().getByName(WsDocGenPlugin.CONFIGURATION_NAME)).getAsFileTree();
                }
            });
        }

        wsDocGenConvention.groups.each {
            task.outputs.dir(it.targetDir);
        }

        genWsDocTask.dependsOn(genWsDocTaskNameForSourceSet)

    }

    private static FileCollection findPluginClasspath(final Project project) {

        Closure<Boolean> pluginDependencyMatcher = {Dependency dependency -> dependency.getGroup() == 'no.statkart.sktools.gradle' && dependency.getName() == 'wsdocgen-plugin'}
        Closure<Boolean> samlepomDependencyMatcher = {Dependency dependency -> dependency.getGroup() == 'no.statkart.sktools.gradle' && dependency.getName() == 'gradle-plugins'}

        List<FileCollection> candidateFileCollections = [project, project.getRootProject()].collect {
            Configuration buildConfiguration = it.getBuildscript().getConfigurations().getByName(ScriptHandler.CLASSPATH_CONFIGURATION);

            Dependency pluginDependency = buildConfiguration.dependencies.find (pluginDependencyMatcher);
            if (!pluginDependency.is(null)) {
                return buildConfiguration.fileCollection(pluginDependency);
            } else {
                //forsøker å finne 'samlepom' [SKIF-154]
                Dependency samlepomDependency = buildConfiguration.dependencies.find (samlepomDependencyMatcher);
                if (!samlepomDependency.is(null)) {
                    return buildConfiguration.fileCollection(samlepomDependency);
                }
            }
            return [];
        }

        FileCollection candidate = candidateFileCollections.find { !it.isEmpty() }
        if (candidate == null) candidate = project.files(); //ok under eksekvering av test

        return candidate;
    }


    private static void configureArchives(final WsDocGenConvention wsDocGenConvention) {
        final Project project = wsDocGenConvention.project


        Zip zip = (Zip) project.task(type: Zip, ARCHIVE_TASK_NAME)
        zip.setClassifier(WsDocGenPlugin.CONFIGURATION_NAME)
        zip.doFirst {
            from(getProject().getTasks().getByName(wsDocGenConvention.genDocTaskName)) //legger dynamiskt til alle definerte output kataloger for 'genWsDocTask'
        }

        ArchivePublishArtifact artifact = new ArchivePublishArtifact(zip)
        project.getArtifacts().add(WsDocGenPlugin.CONFIGURATION_NAME, artifact);

    }


}
