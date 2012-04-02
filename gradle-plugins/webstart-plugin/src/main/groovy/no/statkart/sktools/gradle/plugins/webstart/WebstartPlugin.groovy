package no.statkart.sktools.gradle.plugins.webstart

import org.apache.commons.io.FileUtils
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.ConventionTask
import org.gradle.api.internal.IConventionAware
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.ConventionValue
import org.gradle.api.tasks.bundling.War
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * For generering av webstart klienter og distribusjoner. <br />
 * Har funksjonalitet for generering av jnlp-filer, jar-ressurser, signering og enkel war distribuering.
 * <p><p>
 *
 * <h3>War</h3>
 * For enkel war distribusjon kan man konfigurere opp pluginen via {@link WebstartConvention#warTasks }. <br>
 * Se forøvrigt {@link WebstartPlugin#integrateWithWars(Project, WebstartConvention)} hvordan dette kan gjøres.
 *
 * <h5>Dependencies</h5>
 * For hver klient kan man legge til dependencies. Disse blir lagt t
 *
 * <h3></h3>
 *
 * <h3></h3>
 *
 *
 * <h3></h3>
 *
 * @since 1.0
 * @author Thor Åge Eldby
 */
class WebstartPlugin implements Plugin<Project> {
    private static Logger logger = LoggerFactory.getLogger(WebstartPlugin.class)

    public static final String CONVENTION_NAME = 'webstart'
    public static final String CONFIGURATION_NAME = 'webstart'
    public static final String GEN_WEBSTART_TASK_NAME = 'genWebstart'
    /**
     * @see JnlpServletWarTask
     */
    public static final String JNLP_SERVLET_JARS_TASK_NAME = 'webstartJnlpServletJars'

    @Override
    void apply(Project project) {
        project.plugins.apply(BasePlugin.class);

        final WebstartConvention webstartConvention = new WebstartConvention(project)
        project.getConvention().getPlugins().put(CONVENTION_NAME, webstartConvention)

        final Configuration webstartConfiguration = project.configurations.add(CONFIGURATION_NAME).setDescription("Classpath for jars to be included in all webstart applications (common)");

        WebstartTask genWebstartTask = project.getTasks().add(WebstartPlugin.GEN_WEBSTART_TASK_NAME, WebstartTask.class)
        genWebstartTask.setGroup(WarPlugin.WEB_APP_GROUP)
        genWebstartTask.dependsOn(webstartConfiguration)

        project.getTasks().add(WebstartPlugin.JNLP_SERVLET_JARS_TASK_NAME, JnlpServletWarTask.class);

        configureConventionalValuesForGenWebstartTask(project, webstartConvention)

        project.afterEvaluate(new Action<Project>() {
            public void execute(Project configuredProject) {
                configureConventionDefaults(configuredProject, webstartConvention)
                configureGenWebstartTask(configuredProject, webstartConvention)
                integrateWithWars(project, webstartConvention)
            }
        });


    }

    /**
     * Integrasjon med {@link War} og {@code WarPlugin}
     */
    private void integrateWithWars(final Project project, final WebstartConvention webstartConvention) {
        if (webstartConvention.hasWarTasks()) {
            boolean allTasks = webstartConvention.warTasks.isEmpty()
            Task jnlpServletJarsTask = project.getTasks().getByName(WebstartPlugin.JNLP_SERVLET_JARS_TASK_NAME);

            //configuring war tasks
            project.tasks.withType(War) { War warTask ->
                if (allTasks || webstartConvention.warTasks.contains(warTask.getName())) {

                    //adding jnlp servlet jars
                    warTask.classpath(jnlpServletJarsTask)
                    warTask.dependsOn(WebstartPlugin.JNLP_SERVLET_JARS_TASK_NAME)

                    //adding files generated from webstart task
                    warTask.from(project.tasks.getByName(WebstartPlugin.GEN_WEBSTART_TASK_NAME))
                }
            }
        }
    }

    /**
     * Assigning default values to convention (if not already set by user)
     */
    private void configureConventionDefaults(final Project project, final WebstartConvention webstartConvention) {

        webstartConvention.clients.each { WebstartClientConfiguration clientConfiguration ->

            if (clientConfiguration.outputDir == null) {
                clientConfiguration.outputPath(project.getBuildDirName() + "/generated/webstart");
            }

            if (clientConfiguration.jnlpFilePath == null) {
                clientConfiguration.jnlpFile("${project.name}.jnlp");
            }


            JnlpConfiguration jnlp = clientConfiguration.jnlp()
            jnlp.with() {
                if (title == null) {
                    title("${project.name} v${project.version}".toString());
                }
                if (description == null) {
                    description("");
                    WebstartPlugin.logger.warn("Description missing from ${clientConfiguration.getJnlpFile()}");
                }
                if (vendor == null) {
                    vendor("Statens Kartverk");
                    WebstartPlugin.logger.debug("Assigning default value for vendor (${vendor})");
                }

                if (!hasApplication()) {
                    WebstartPlugin.logger.error("Application not set!")
                } else if (getApplication().mainClass == null) {
                    WebstartPlugin.logger.error("Application.manClass not set!")
                }

                if (resources.collect() {it.jarDependencies}.isEmpty()) {
                    WebstartPlugin.logger.error("No resources defined for ${clientConfiguration.getJnlpFile()}!");
                }

                if (resources.isEmpty()) {
                    resources {}    //adding a empty resource when no one exists
                }

                if (resources.collectMany() {it.runtimes}.isEmpty()) {
                    WebstartPlugin.logger.error("No runtimes defined. Assigning default runtime, not recomended for production deployment!");
                    resources.each { ResourcesConfiguration resourcesConfiguration ->
                        resourcesConfiguration.runtime().version("1.6+").href("http://java.sun.com/products/autodl/j2se").xmx("512");
                    }
                }

                resources.each {
                    if (it.libPath == null) {
                        it.libPath('lib');
                    }

                    //legger alle deklarerte dependencies til 'webstart' konfigurasjon.
                    DependencyHandler dependencyHandler = project.getDependencies();
                    it.jarDependencies.getDependencies().each {
                        dependencyHandler.add(WebstartPlugin.CONFIGURATION_NAME, it);
                    }

                    //setter jnlp.versionEnabled=true
                    //Dette er funksjonalitet introdusert i java 6u10
                    // - for dokumentasjon, søk etter "Avoiding Unnecessary Update Checks JNLP"
                    it.systemProperties('jnlp.versionEnabled': true)
                }

            }
        }

    }

    /**
     * Registrerer output og depends on basert på konfigurasjon.
     */
    private void configureGenWebstartTask(final Project project, final WebstartConvention webstartConvention) {
        Task genWebstartTask = project.getTasks().getByName(WebstartPlugin.GEN_WEBSTART_TASK_NAME);
        webstartConvention.clients.each {
            Configuration configuration = project.getConfigurations().getByName(WebstartPlugin.CONFIGURATION_NAME)
            genWebstartTask.dependsOn(configuration)

            //registrerer output slik at enhver tukling med disse filer vil trigge ny bygging.
            genWebstartTask.outputs.dir(it.outputDir)
        }

    }


    private configureConventionalValuesForGenWebstartTask(final Project project, final WebstartConvention webstartConvention) {
        project.tasks.withType(WebstartTask.class).all{ ConventionTask task ->
            ConventionMapping conventionMapping = task.getConventionMapping()

            /** {@link WebstartTask#clients} **/
            conventionMapping.map("clients", new ConventionValue() {
                public Object getValue(Convention conventionManager, IConventionAware conventionAwareObject) {
                    return webstartConvention.clients
                }
            });

            /** {@link WebstartTask#keystoreFile} **/
            conventionMapping.map("keystoreFile", new ConventionValue() {
                File tempFile = null;

                public Object getValue(Convention conventionManager, IConventionAware conventionAwareObject) {
                    if (project.hasProperty('webstart.sign.keystore')) {
                        return project.file(project.hasProperty('webstart.keystore'))
                    } else {
                        if (tempFile == null) {
                            tempFile = File.createTempFile("kodesignering", "jks")
                            tempFile.deleteOnExit()
                            FileUtils.copyURLToFile(getClass().getResource("kodesignering.jks"), tempFile)
                        }
                        return tempFile
                    }
                }
            });

            /** {@link WebstartTask#alias} **/
            conventionMapping.map("alias", new ConventionValue() {
                public Object getValue(Convention conventionManager, IConventionAware conventionAwareObject) {
                    project.getProperties().get('webstart.sign.alias', 'statenskartverk')
                }
            });

            /** {@link WebstartTask#password} **/
            conventionMapping.map("password", new ConventionValue() {
                public Object getValue(Convention conventionManager, IConventionAware conventionAwareObject) {
                    project.getProperties().get('webstart.sign.password', 'SagZ45_p1')
                }
            });


        }
    }

}


