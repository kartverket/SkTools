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
import org.gradle.api.tasks.compile.AbstractCompile

import org.gradle.api.tasks.bundling.War
import java.util.concurrent.Callable
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
 * Pluginen konfigurerer opp konfigurasjoner og task med navn ihht standard javalpugin konvensjon. Se bla {@link SourceSet}.
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
    public static final String WEBLOGIC_WAR_TASK_NAME = 'weblogicWar'
    public static final String COMPILE_WEBLOGIC_TASK_NAME = 'compileWeblogicJava'
    public static final String PROCESS_WEBLOGIC_RESOURCES_TASK_NAME = 'processWeblogicResources'
    public static final String WEBLOGIC_CLASSES_TASK_NAME = 'weblogicClasses'

    @Override
    void apply(Project project) {
        project.apply plugin: JavaBasePlugin.class
        project.apply plugin: WeblogicBasePlugin.class

        JavaPluginConvention javaConvention = project.getConvention().getPlugins().get("java");

        SourceSet sourceSet = configureSourceSet(javaConvention)

        WeblogicWsWarConvention convention = new WeblogicWsWarConvention(javaConvention)
        project.convention.plugins.put(WeblogicWsWarPlugin.CONVENTION_NAME, convention);

        configureConfigurations(javaConvention);

        Task compileTask = configureCompileTask(project, sourceSet).dependsOn(
                project.getConfigurations().getByName(sourceSet.getCompileConfigurationName()), //tvinger rekompilering ved endring i classpath (feks dersom weblogic classpath endrer seg)
        )

        //task for bygging av war artifakt
        Task war = configureArchives(javaConvention, sourceSet).dependsOn(
                WeblogicWsWarPlugin.WEBLOGIC_CLASSES_TASK_NAME,
        )


    }

    private Task configureArchives(final JavaPluginConvention javaConvention, final SourceSet sourceSet) {
        Project project = javaConvention.project;

        if (project.getTasks().findByName(JavaPlugin.TEST_TASK_NAME) != null) {
            project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(JavaPlugin.TEST_TASK_NAME);
        }
        War war = project.getTasks().add(WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME, War.class);
        war.setDescription("Assembles a war archive containing the main classes.");
        war.setClassifier(WeblogicWsWarPlugin.WEBLOGIC_SOURCE_SET_NAME)
        war.setGroup(BasePlugin.BUILD_GROUP);
        war.from(sourceSet.getOutput()) {
            exclude 'WEB-INF/web.xml'
            exclude '**/*.java'
        }
        war.getMetaInf().from(new Callable() {
            public Object call() throws Exception {
                return javaConvention.getMetaInf();
            }
        });

        ArchivePublishArtifact artifact = new ArchivePublishArtifact(war)
        project.getExtensions().getByType(DefaultArtifactPublicationSet.class).addCandidate(artifact);
        project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_CONFIGURATION_NAME).getArtifacts().add(artifact);

        return war;
    }

    /**
     * Erstatter compile task for source-sett.
     */
    private Task configureCompileTask(final Project project, final SourceSet sourceSet) {

        //erstatter compileTask
        // - dette da det per gradle versjon 1.0 ikke finnes noen option for setting av implementasjonsklasse for compileTask task.
        JavaBasePlugin javaBasePlugin = project.getPlugins().getPlugin(JavaBasePlugin.class);
        AbstractCompile compileTask = project.task(sourceSet.getCompileJavaTaskName(), type: WeblogicWsCompileTask, overwrite: true)
        javaBasePlugin.configureForSourceSet(sourceSet, compileTask);

        return compileTask
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
     *  <li><code>weblogicCompile</code> arver ifra <code>compile</code> (dersom definert).
     *  <li><code>weblogicRuntime</code> arver ifra <code>runtime</code> (dersom definert).
     * </ul>
     */
    private void configureConfigurations(final JavaPluginConvention javaConvention) {
        Project project = javaConvention.project

        SourceSet weblogicSourceSet = javaConvention.getSourceSets().getByName(WeblogicWsWarPlugin.WEBLOGIC_SOURCE_SET_NAME);
        Configuration weblogicConfiguration = project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_CONFIGURATION_NAME);
        Configuration weblogicCompileConfiguration = project.getConfigurations().getByName(weblogicSourceSet.getCompileConfigurationName());
        Configuration weblogicRuntimeConfiguration = project.getConfigurations().getByName(weblogicSourceSet.getRuntimeConfigurationName());


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
