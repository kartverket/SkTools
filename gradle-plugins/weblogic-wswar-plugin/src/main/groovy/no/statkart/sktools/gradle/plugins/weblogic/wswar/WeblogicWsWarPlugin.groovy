package no.statkart.sktools.gradle.plugins.weblogic.wswar

import no.statkart.sktools.gradle.plugins.weblogic.WeblogicBasePlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.War
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
    public static final String WEBLOGIC_SOURCE_SET_NAME = 'weblogic'
    public static final String WEBLOGIC_WAR_TASK_NAME = 'warWeblogic'
    public static final String WEBLOGIC_GEN_TASK_NAME = 'genWeblogic'

    @Override
    void apply(Project project) {
        project.apply plugin: JavaBasePlugin.class
        project.apply plugin: WeblogicBasePlugin.class

        project.getLogger().warn("WARNING: WeblogicWsWarPlugin is deprecated and is scheduled for removal!")

        // wswar har alltid trengt tools.jar (weblogic 10.3.5, 10.3.6, 12.1.x)
        project.getDependencies().add(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME, WeblogicBasePlugin.toolsJar(project));
        project.getDependencies().add(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME, conventionalWeblogicDependencies(project));

        project.getConfigurations().maybeCreate(WEBLOGIC_CONFIGURATION_NAME);
        final SourceSet weblogicSourceSet = createSourceSet(project);

        configureConfigurations(project, weblogicSourceSet);
        configureIdea(project, weblogicSourceSet)


        WeblogicWsCompileTask genTask = (WeblogicWsCompileTask) configureGenTask(project, weblogicSourceSet).dependsOn(
                project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME), //tvinger rekompilering ved endring i weblogicClasspath
        )

        //task for bygging av war artifakt
        War war = configureArchives(project, weblogicSourceSet);
    }

    public static FileCollection conventionalWeblogicDependencies(Project project) {
        if (project.hasProperty("WEBLOGIC_HOME")) {
            if (project.hasProperty("WEBLOGIC_VERSION")) {
                def wlsVersion = project.property("WEBLOGIC_VERSION");
                def wlsJars = weblogicJarsFor(wlsVersion as String, project);
                return wlsJars;
            }
        }
        return project.files();
    }

    static FileCollection weblogicJarsFor(String wlsVersion, Project project) {
        def WEBLOGIC_HOME = project.property("WEBLOGIC_HOME");

        if (wlsVersion.startsWith("12.")) {
            if (wlsVersion.startsWith("12.1")) {
                if (JavaVersion.current().isJava9Compatible()) {
                    project.getLogger().warn('WARNING: Weblogic 12.1.x does not support java ' + JavaVersion.current())
                    return project.files()
                }
                return project.files(
                        "${WEBLOGIC_HOME}/wlserver/modules/databinding.override_1.2.0.0.jar",
                        "${WEBLOGIC_HOME}/wlserver/server/lib/weblogic.jar",
                );
            }

            if (wlsVersion.startsWith("12.2")) {
                return project.files(
                        "${WEBLOGIC_HOME}/wlserver/modules/databinding.override.jar",
                        "${WEBLOGIC_HOME}/wlserver/server/lib/weblogic.jar",
                );
            }

            project.logger.warn('WARNING: no optimization found for Weblogic version ' + wlsVersion);
            return project.fileTree(dir: WEBLOGIC_HOME, includes: [
                    "wlserver/modules/databinding.override*.jar",
                    "wlserver/server/lib/weblogic.jar",
            ]);
        }

        throw new Exception("Unsupported Weblogic version found - please add support for " + wlsVersion);
    }

    private static void configureIdea(final Project project, final SourceSet weblogicSourceSet) {
        project.plugins.withType(IdeaPlugin.class) {
            project.afterEvaluate { // Så vi vet at det ikke blir lagt på noe JavaPlugin senere. Hvis det skjer, så overskriver IdeaPlugin scope-greiene
                project.idea.module {
                    sourceDirs += weblogicSourceSet.allSource.srcDirs

                    def weblogicConfiguration = project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME)


                    ['COMPILE', 'RUNTIME', 'TEST', 'PROVIDED'].each { scopeName ->
                        scopes[scopeName] = scopes[scopeName] ?: [plus: [], minus: []] //SKTOOLS-133: oppretter scopes selv dersom ikke JavaPlugin er aktivert...
                    }

                    scopes.COMPILE.plus += [project.configurations[weblogicSourceSet.compileConfigurationName], weblogicConfiguration]
                    scopes.RUNTIME.plus += [project.configurations[weblogicSourceSet.runtimeConfigurationName]]
                    scopes.RUNTIME.minus += [project.configurations[weblogicSourceSet.compileConfigurationName]]
                    scopes.TEST.plus += [project.configurations[weblogicSourceSet.runtimeConfigurationName]]

                }
            }
        }
    }

    private static War configureArchives(final Project project, final SourceSet weblogicSourceSet) {
        final WeblogicWsCompileTask genTask = (WeblogicWsCompileTask) project.getTasks().getByName(WEBLOGIC_GEN_TASK_NAME);

        //late evaluate if java plugin is applied anytome after...
        project.afterEvaluate {
            if (project.getTasks().findByName(JavaPlugin.TEST_TASK_NAME) != null) {
                project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(JavaPlugin.TEST_TASK_NAME);
            }
        }

        final War war = project.getTasks().create(WEBLOGIC_WAR_TASK_NAME, War.class);
        war.setDescription("Assembles a war archive containing the main classes.");
        war.dependsOn(genTask);

        war.setAppendix(WEBLOGIC_SOURCE_SET_NAME);
        war.setGroup(BasePlugin.BUILD_GROUP);

        // following duplicate files will be excluded (first one into archive stays)
        war.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE); //SKTOOLS-121

        // Må ta inn output fra genTask eksplisitt
        war.into('WEB-INF/classes') {
            from genTask.classesDir
        }

        war.from(genTask.getDestinationDir())

        war.into('WEB-INF/classes') {
            from weblogicSourceSet.output
        }


        ArchivePublishArtifact artifact = new ArchivePublishArtifact(war)
        project.getExtensions().getByType(DefaultArtifactPublicationSet.class).addCandidate(artifact);
        project.getArtifacts().add(WEBLOGIC_CONFIGURATION_NAME, artifact);

        return war;
    }

    /**
     * Legger til task for generering av webservice implementasjon
     * @see WeblogicWsCompileTask
     */
    private static WeblogicWsCompileTask configureGenTask(final Project project, final SourceSet weblogicSourceSet) {

        WeblogicWsCompileTask genTask = project.tasks.create(WEBLOGIC_GEN_TASK_NAME, WeblogicWsCompileTask.class)
        genTask.dependsOn weblogicSourceSet.compileJavaTaskName

        genTask.description = 'Generates the web service implementation on server using Weblogic jwsc'
        genTask.group = BasePlugin.BUILD_GROUP

        genTask.source = weblogicSourceSet.java
        genTask.classpath = weblogicSourceSet.runtimeClasspath //avhenger av kompilerte filer ifra weblogicSourceSet

        genTask.destinationDir = project.file("${project.buildDir}/${weblogicSourceSet.name}/webapp")
        genTask.classesDir = project.file("${project.buildDir}/${weblogicSourceSet.name}/classes")
        genTask.genSourcesDir = project.file("gen/weblogic/jwsc")

        genTask.genDir = project.file("${project.buildDir}/weblogic/jwsc")

        weblogicSourceSet.allSource.srcDir { genTask.genSourcesDir }

        // Kan ikke legge til genTasks output som weblogicSourceSet.output, siden genTask har weblogicSourceSet.output, via
        // weblogicSourceSet.runtimeClasspath, som input

        return genTask
    }

    /**
     * Konfigurerer source set for weblogic kode
     *
     * Legger til filer for weblogic provided til classpath
     */
    private static SourceSet createSourceSet(Project project) {
        final JavaPluginConvention javaConvention = (JavaPluginConvention) project.getConvention().getPlugins().get("java");
        final SourceSet weblogicSourceSet = javaConvention.getSourceSets().create(WEBLOGIC_SOURCE_SET_NAME);

        final Configuration weblogicProvidedConfiguration = project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME);
        final Configuration weblogicCompileConfiguration = project.getConfigurations().getByName(weblogicSourceSet.getCompileConfigurationName());
        final Configuration weblogicRuntimeConfiguration = project.getConfigurations().getByName(weblogicSourceSet.getRuntimeConfigurationName());

        final SourceSet mainSourceSet = javaConvention.getSourceSets().findByName(SourceSet.MAIN_SOURCE_SET_NAME);

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
    private static void configureConfigurations(Project project, final SourceSet weblogicSourceSet) {
        final JavaPluginConvention javaConvention = (JavaPluginConvention) project.getConvention().getPlugins().get("java");

        final Configuration weblogicConfiguration = project.getConfigurations().getByName(WEBLOGIC_CONFIGURATION_NAME);
        final Configuration weblogicCompileConfiguration = project.getConfigurations().getByName(weblogicSourceSet.getCompileConfigurationName());
        final Configuration weblogicRuntimeConfiguration = project.getConfigurations().getByName(weblogicSourceSet.getRuntimeConfigurationName());

        weblogicConfiguration.extendsFrom(weblogicRuntimeConfiguration);
        weblogicRuntimeConfiguration.extendsFrom(weblogicCompileConfiguration);

        //dersom javaplugin er aktivert..
        SourceSet mainSourceSet = javaConvention.getSourceSets().findByName(SourceSet.MAIN_SOURCE_SET_NAME);
        if (mainSourceSet != null) {
            Configuration compileConfiguration = project.getConfigurations().findByName(mainSourceSet.getCompileClasspathConfigurationName())
            Configuration runtimeConfiguration = project.getConfigurations().findByName(mainSourceSet.getRuntimeClasspathConfigurationName())

            if (compileConfiguration != null) {
                weblogicCompileConfiguration.extendsFrom(compileConfiguration);     //compile configuration arver fra main sin compile
            }
            if (runtimeConfiguration != null) {
                weblogicRuntimeConfiguration.extendsFrom(runtimeConfiguration);     //runtime configuration arver fra main sin runtime
            }
        }

    }

}
