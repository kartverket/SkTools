package no.statkart.sktools.gradle.plugins.weblogic.wswar

import no.statkart.sktools.gradle.plugins.weblogic.WeblogicBasePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.plugins.ide.idea.IdeaPlugin

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
 *   <li><code>weblogicCompile</code> - evt exstra jar libs
 *   <li><code>weblogicRuntime</code> - evt exstra jar libs
 *
 *   <li><code>weblogic</code> - configuration for war artifakt (weblogicRuntime og weblogicCompile arver ifra denne).
 *   <li><code>weblogicProvided</code> - legg alle weblogic jar avhengingheter for bygging/debug her.
 *
 * </ul>
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 * @author Tor Egil R. Strand
 */
class WeblogicWsWarPlugin implements Plugin<Project> {

    public static final String CONVENTION_NAME = 'weblogicWsWar'
    public static final String WEBLOGIC_CONFIGURATION_NAME = 'weblogic'
    public static final String WEBLOGIC_COMPILE_CONFIGURATION_NAME = 'weblogicCompile'
    public static final String WEBLOGIC_RUNTIME_CONFIGURATION_NAME = 'weblogicRuntime'
    public static final String WEBLOGIC_SOURCE_SET_NAME = 'weblogic'
    public static final String WEBLOGIC_WAR_TASK_NAME = 'warWeblogic'
    public static final String WEBLOGIC_GEN_TASK_NAME = 'genWeblogic'

    @Override
    void apply(Project project) {
        project.apply plugin: JavaBasePlugin.class
        project.apply plugin: WeblogicBasePlugin.class

        JavaPluginConvention javaConvention = project.getConvention().getPlugins().get("java");

        Configuration weblogicConfiguration = createWeblogicConfiguration(project);
        SourceSet weblogicSourceSet = javaConvention.getSourceSets().add(WeblogicWsWarPlugin.WEBLOGIC_SOURCE_SET_NAME)

        configureSourceSet(weblogicSourceSet, javaConvention);
        configureConfigurations(weblogicSourceSet, javaConvention);
        configureIdea(project, weblogicSourceSet)

        WeblogicWsWarConvention convention = new WeblogicWsWarConvention(javaConvention);
        project.convention.plugins.put(WeblogicWsWarPlugin.CONVENTION_NAME, convention);


        WeblogicWsCompileTask genTask = (WeblogicWsCompileTask) configureGenTask(project, weblogicSourceSet).dependsOn(
                project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME), //tvinger rekompilering ved endring i weblogicClasspath
        )

        //task for bygging av war artifakt
        WeblogicWarTask war = (WeblogicWarTask) configureArchives(javaConvention, weblogicSourceSet, genTask).dependsOn(
                genTask,
        )


    }

    private void configureIdea(final Project project, final SourceSet sourceSet) {
        project.plugins.withType(IdeaPlugin.class) {
            project.afterEvaluate { // Så vi vet at det ikke blir lagt på noe JavaPlugin senere. Hvis det skjer, så overskriver IdeaPlugin scope-greiene
                project.idea.module {
                    sourceDirs += sourceSet.allSource.srcDirs

                    def weblogicConfiguration = project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME)

                    // Dersom Java
                    if (project.plugins.hasPlugin(JavaPlugin.class)) {
                        scopes.COMPILE.plus += [project.configurations[sourceSet.compileConfigurationName], weblogicConfiguration]
                        scopes.RUNTIME.minus += project.configurations[sourceSet.compileConfigurationName]
                        scopes.RUNTIME.plus += project.configurations[sourceSet.runtimeConfigurationName]
                    } else {
                        scopes.PROVIDED = [plus: [], minus: []]
                        scopes.COMPILE = [plus: [project.configurations[sourceSet.compileConfigurationName], weblogicConfiguration], minus: []]
                        scopes.RUNTIME = [plus: [project.configurations[sourceSet.runtimeConfigurationName]], minus: [project.configurations[sourceSet.compileConfigurationName]]]
                        scopes.TEST = [plus: [], minus: []]
                    }
                }
            }
        }
    }

    private Task configureArchives(final JavaPluginConvention javaConvention, final SourceSet sourceSet, final WeblogicWsCompileTask genTask) {
        Project project = javaConvention.project;

        if (project.getTasks().findByName(JavaPlugin.TEST_TASK_NAME) != null) {
            project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(JavaPlugin.TEST_TASK_NAME);
        }

        WeblogicWarTask war = project.getTasks().add(WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME, WeblogicWarTask.class);
        war.setDescription("Assembles a war archive containing the main classes.");
        war.setAppendix(WeblogicWsWarPlugin.WEBLOGIC_SOURCE_SET_NAME)
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
        project.getArtifacts().add(WeblogicWsWarPlugin.WEBLOGIC_CONFIGURATION_NAME, artifact);

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
        genTask.classpath = sourceSet.runtimeClasspath //avhenger av kompilerte filer ifra sourceSet

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
     * Konfigurerer source set for weblogic kode
     *
     * Legger til filer for weblogic provided til classpath
     */
    private SourceSet configureSourceSet(SourceSet weblogicSourceSet, JavaPluginConvention javaConvention) {
        Project project = javaConvention.project
        Configuration weblogicProvidedConfiguration = project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME);
        Configuration weblogicCompileConfiguration = project.getConfigurations().getByName(weblogicSourceSet.getCompileConfigurationName());
        Configuration weblogicRuntimeConfiguration = project.getConfigurations().getByName(weblogicSourceSet.getRuntimeConfigurationName());

        SourceSet mainSourceSet = javaConvention.getSourceSets().findByName(SourceSet.MAIN_SOURCE_SET_NAME);

        //legger til weblogicProvided til compile og runtime classpath
        //legger til main sourceset til compile og runtime classpath
        weblogicSourceSet.setCompileClasspath(project.files(
                { mainSourceSet?.getOutput() },
                weblogicCompileConfiguration,
                weblogicProvidedConfiguration
        ));
        weblogicSourceSet.setRuntimeClasspath(project.files(
                { mainSourceSet?.getOutput() },
                weblogicSourceSet.getOutput(),
                weblogicRuntimeConfiguration,
                weblogicProvidedConfiguration
        ));

        return weblogicSourceSet;
    }

    /**
     * Konfigurerer avhengigheter slik at <br/>
     * <ul>
     *  <li><code>weblogic</code> arver ifra <code>weblogicRuntime</code>
     *  <li><code>weblogicRuntime</code> arver ifra <code>weblogicCompile</code> (default behaviour)
     *  <li><code>weblogicCompile</code> arver ifra <code>compile</code> (dersom definert)
     *  <li><code>weblogicRuntime</code> arver ifra <code>runtime</code> (dersom definert)
     * </ul>
     */
    private void configureConfigurations(SourceSet weblogicSourceSet, JavaPluginConvention javaConvention) {
        Project project = javaConvention.project

        Configuration weblogicConfiguration = project.getConfigurations().getByName(WeblogicWsWarPlugin.WEBLOGIC_CONFIGURATION_NAME);
        Configuration weblogicCompileConfiguration = project.getConfigurations().getByName(weblogicSourceSet.getCompileConfigurationName());
        Configuration weblogicRuntimeConfiguration = project.getConfigurations().getByName(weblogicSourceSet.getRuntimeConfigurationName());

        weblogicConfiguration.extendsFrom(weblogicRuntimeConfiguration);
        weblogicRuntimeConfiguration.extendsFrom(weblogicCompileConfiguration);

        //dersom javaplugin er aktivert..
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

    private Configuration createWeblogicConfiguration(Project project) {
        Configuration weblogicConfiguration = project.getConfigurations().findByName(WEBLOGIC_CONFIGURATION_NAME);
        if (weblogicConfiguration == null) {
            weblogicConfiguration = project.getConfigurations().add(WeblogicWsWarPlugin.WEBLOGIC_CONFIGURATION_NAME);
        }
        return weblogicConfiguration;
    }


}
