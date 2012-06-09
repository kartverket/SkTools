package no.statkart.sktools.gradle.plugins.weblogic.wswar

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin

import no.statkart.sktools.gradle.plugins.weblogic.WeblogicBasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.artifacts.Configuration

import org.gradle.api.tasks.bundling.War

import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.plugins.BasePlugin

/**
 * Baserer seg på WeblogicBasePlugin og JavaBasePlugin.
 *
 * <p>
 * Dersom JavaPlugin er aktivert vil det arves ifra main.java konfigurasjonen.
 *  - Dette vil da si at alle dependencies for main vil bli arvet og lagt på weblogic sin.
 *
 * <p>
 * Pluginen konfigurerer opp source set med kjente konfigurasjoner. Se {@link SourceSet} for dokumentasjon.
 *
 * <p>
 * Følgende kofigurasjoner defineres:
 * <ul>
 *   <li><code>weblogic</code> - legg alle weblogic jar avhengingheter for bygging/debug her.  
 *   <li><code>weblogicCompile</code> - legg kompile time avhengigheter her. 
 *   <li><code>weblogicRuntime</code> - legg evt runtime avhengigheter her. 
 * </ul>
 *
 *
 * <p> todo: finne ut hvordan en fikser dependency/classpath for evt koblet 'main' sourceSet
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class WeblogicWsWarPlugin implements Plugin<Project> {

    public static final String CONVENTION_NAME = 'weblogicWsWar'
    public static final String WEBLOGIC_SOURCE_SET_NAME = 'weblogic'
    public static final String WEBLOGIC_WAR_TASK_NAME = 'warWeblogic'
    public static final String WEBLOGIC_GEN_TASK_NAME = 'genWeblogic'

    @Override
    void apply(Project project) {
        project.apply plugin: JavaBasePlugin.class
        project.apply plugin: WeblogicBasePlugin.class

        JavaPluginConvention javaConvention = project.getConvention().getPlugins().get("java");

        SourceSet sourceSet = configureSourceSet(javaConvention);
        configureConfigurations(javaConvention);

        WeblogicWsWarConvention convention = new WeblogicWsWarConvention(javaConvention);
        project.convention.plugins.put(WeblogicWsWarPlugin.CONVENTION_NAME, convention);


        WeblogicWsCompileTask genTask = (WeblogicWsCompileTask) configureGenTask(project, sourceSet).dependsOn(
                project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_CONFIGURATION_NAME), //tvinger rekompilering ved endring i weblogicClasspath
        )

        //task for bygging av war artifakt
        WeblogicWarTask war = (WeblogicWarTask) configureArchives(javaConvention, sourceSet, genTask).dependsOn(
                genTask,
        )


    }

    private Task configureArchives(final JavaPluginConvention javaConvention, final SourceSet sourceSet, final WeblogicWsCompileTask genTask) {
        Project project = javaConvention.project;

        if (project.getTasks().findByName(JavaPlugin.TEST_TASK_NAME) != null) {
            project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(JavaPlugin.TEST_TASK_NAME);
        }

        WeblogicWarTask war = project.getTasks().add(WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME, WeblogicWarTask.class);
        war.setDescription("Assembles a war archive containing the main classes.");
        war.setClassifier(WeblogicWsWarPlugin.WEBLOGIC_SOURCE_SET_NAME)
        war.setGroup(BasePlugin.BUILD_GROUP);

        //evt duplikate entries blir forkastes.. vær derfor obs på rekkefølgen!

        war.into('WEB-INF/classes') {
            from genTask.classesDir
        }

        war.from(genTask.getDestinationDir())

        war.into('WEB-INF/classes') {
            from sourceSet.output.classesDir
        }


        ArchivePublishArtifact artifact = new ArchivePublishArtifact(war)
        project.getExtensions().getByType(DefaultArtifactPublicationSet.class).addCandidate(artifact);
        project.getArtifacts().add(WeblogicBasePlugin.WEBLOGIC_CONFIGURATION_NAME, artifact);

        return war;
    }

    /**
     * Legger til task for generering av webservice implementasjon
     * @see WeblogicWsCompileTask
     */
    private WeblogicWsCompileTask configureGenTask(final Project project, final SourceSet sourceSet) {

        WeblogicWsCompileTask genTask = (WeblogicWsCompileTask) project.task(WEBLOGIC_GEN_TASK_NAME, type: WeblogicWsCompileTask.class)
        genTask.dependsOn sourceSet.compileJavaTaskName

        genTask.description = 'Generates the web service implementation on server using Weblogic jwsc'
        genTask.group = BasePlugin.BUILD_GROUP

        genTask.source = sourceSet.java
        genTask.classpath = project.configurations[sourceSet.runtimeConfigurationName]

        genTask.destinationDir = project.file("${project.buildDir}/${sourceSet.name}/webapp")
        genTask.classesDir = project.file("${project.buildDir}/${sourceSet.name}/classes")
        genTask.genSourcesDir = project.file("gen/weblogic/jwsc")

        genTask.genDir = project.file("${project.buildDir}/weblogic/jwsc")


        //registrerer mapper til sourceSet
        sourceSet.output.dir { genTask.classesDir }
        sourceSet.allSource.srcDir { genTask.genSourcesDir }

        return genTask
    }

    /**
     * Oppretter og registrerer nytt source sett i javaconvention
     */
    private SourceSet configureSourceSet(final JavaPluginConvention javaConvention) {
        SourceSet weblogicSourceSet = javaConvention.getSourceSets().add(WeblogicWsWarPlugin.WEBLOGIC_SOURCE_SET_NAME)

        return weblogicSourceSet;
    }

    /**
     * Konfigurerer avhengigheter slik at <br/>
     * <ul>
     *  <li><code>weblogicCompile</code> arver ifra <code>weblogic</code>
     *  <li><code>weblogicCompile</code> arver ifra <code>compile</code> (dersom definert)
     *  <li><code>weblogicRuntime</code> arver ifra <code>runtime</code> (dersom definert)
     * </ul>
     */
    private void configureConfigurations(final JavaPluginConvention javaConvention) {
        Project project = javaConvention.project

        SourceSet weblogicSourceSet = javaConvention.getSourceSets().getByName(WeblogicWsWarPlugin.WEBLOGIC_SOURCE_SET_NAME);
        Configuration weblogicConfiguration = project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_CONFIGURATION_NAME);
        Configuration weblogicCompileConfiguration = project.getConfigurations().getByName(weblogicSourceSet.getCompileConfigurationName());
        Configuration weblogicRuntimeConfiguration = project.getConfigurations().getByName(weblogicSourceSet.getRuntimeConfigurationName());

        weblogicCompileConfiguration.extendsFrom(weblogicConfiguration)

        SourceSet mainSourceSet = javaConvention.getSourceSets().findByName(SourceSet.MAIN_SOURCE_SET_NAME);
        if (mainSourceSet != null) {
            Configuration compileConfiguration = project.getConfigurations().findByName(mainSourceSet.getCompileConfigurationName());
            Configuration runtimeConfiguration = project.getConfigurations().findByName(mainSourceSet.getRuntimeConfigurationName());

            if (compileConfiguration != null) {
                weblogicCompileConfiguration.extendsFrom(compileConfiguration);     //compile configuration arver fra main sin compile
            }
            if (runtimeConfiguration != null) {
                weblogicRuntimeConfiguration.extendsFrom(runtimeConfiguration);     //runtime configuration arver fra main sin runtime
            }
        }

    }

}
