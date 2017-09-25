package no.statkart.sktools.gradle.plugins.weblogic.wsclient

import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import no.statkart.sktools.gradle.plugins.weblogic.WeblogicBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.compile.AbstractCompile

import org.gradle.api.plugins.BasePlugin
import org.gradle.api.internal.ConventionMapping

import java.util.concurrent.Callable
import org.gradle.api.tasks.SourceSetContainer

/**
 * Baserer seg på {@code JavaBasePlugin} og integrerer med {@code JavaPlugin} dersom denne aktiveres.
 *
 * @since 1.1
 * @author Leif Lislegård
 * @author Tor Egil R. Strand
 */
class WeblogicWsClientPlugin implements Plugin<Project> {

    public static final String CONVENTION_NAME = 'weblogicWsClient'
    public static final String GEN_CLIENT_TASK_NAME = 'genWeblogicWsClient'


    @Override
    void apply(Project project) {
        project.apply plugin: JavaBasePlugin.class;
        project.apply plugin: WeblogicBasePlugin.class;

        // SKTOOLS-17: weblogic 10.3.5 eller nyerer avhenger av tools.jar på classpath for wsclient
        WeblogicBasePlugin.addToolsJarToWeblogicProvidedClasspath(project)
        conditionallyAddWeblogicDependencies(project)

        final JavaPluginConvention javaConvention = project.getConvention().getPlugins().get("java") as JavaPluginConvention;
        final Configuration weblogicProvidedConfiguration = project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME)

        //konfigurerer opp et source sett
        final SourceSet sourceSet = configureSourceSet(javaConvention);

        WeblogicWsClientConvention wsClientConvention = new WeblogicWsClientConvention(project);
        project.convention.plugins.put(CONVENTION_NAME, wsClientConvention);
        wsClientConvention.genDir = "gen/${sourceSet.name}/wsclient"

        def compileTask = createCompileTask(wsClientConvention, sourceSet, javaConvention)

        wsClientConvention.webService.all { WebServiceConfig webService ->
            Task collectSchemaTask = createCollectSchemaTask(project, webService)
            WeblogicGenClientTask genClientSourceTask = createGenerateSourceTask(project, webService, collectSchemaTask)
            genClientSourceTask.dependsOn weblogicProvidedConfiguration //tvinger up to date check (evt recompile) av weblogic avhengigheter...

            compileTask.source(genClientSourceTask)
        }

        //hekter inn genClient ved kjøring av 'resources' task.
        Task processWeblogicResources = project.tasks.getByName(sourceSet.processResourcesTaskName).dependsOn(
                GEN_CLIENT_TASK_NAME,
        );

        //hekter inn genClient ved kjøring av 'compile' task.
        Task compileWeblogicResources = project.tasks.getByName(sourceSet.compileJavaTaskName).dependsOn(
                GEN_CLIENT_TASK_NAME,
        );

    }

    private Task createCollectSchemaTask(Project project, WebServiceConfig webService) {

        String taskName = String.format("collect%sSchema", StringUtils.capitalize(webService.name));

        Sync task = project.tasks.replace(taskName, Sync);
        task.setFileMode(0755);  //SKTOOLS-123 no read only generated files i linux
        task.setDirMode(0755); //SKTOOLS-123 no read only generated files i linux

        task.destinationDir = new File(new File(project.buildDir, 'wsclient'), taskName);
        task.inputs.files webService.baseWars, webService.schemaFiles
        task.includeEmptyDirs = false

        task.from {
            Collection<File> warFiles = webService.baseWars.files.findAll {
                it.getName().toLowerCase().endsWith(".war")
            }
            return warFiles.collect { File file ->
                project.zipTree(file).matching {
                    include '**/*.wsdl'
                    include '**/*.xsd'
                }
            }
        }

        task.from webService.schemaFiles

        return task;
    }

    /**
     * Oppretter task for kodegenerering av klient-stubber.
     *
     * TaskOutput blir lagt til som javasource for sourceSet. NB: denne FileCollection kan kun inneholde Dirs (ikke filer osv.)
     */
    private WeblogicGenClientTask createGenerateSourceTask(Project project, WebServiceConfig webServiceConfig, Task collectSchemaTask) {
        String taskName = String.format('gen%sWsClientSource', StringUtils.capitalize(webServiceConfig.name))
        WeblogicGenClientTask genTask = (WeblogicGenClientTask) project.task(type: WeblogicGenClientTask.class, taskName)
        genTask.setDescription(String.format("Generates WS-client source based on Weblogic tools for " + webServiceConfig.name));
        genTask.setGroup(BasePlugin.BUILD_GROUP);

        genTask.source(collectSchemaTask)
        genTask.webServiceConfig = webServiceConfig

        ConventionMapping conventionMapping = genTask.getConventionMapping();

        //setter ikke classpath da denne ikke trengs

        conventionMapping.map("destinationDir", new Callable<Object>() {
            public Object call() throws Exception {
                return project.file("${project.buildDir}/wsclient/${taskName}");
            }
        });


        return genTask;
    }

    /**
     * Oppretter task for kompilering og kopiering av ressursfiler.
     *
     * Setter classpath og destinationDir som defaults via wsClientConvention.
     *
     * Registrerer genDir til sourceset.output
     * Registrerer også kildekode til sourceSet.allSource
     *
     * @see WeblogicWsClientPlugin#GEN_CLIENT_TASK_NAME
     */
    private AbstractCompile createCompileTask(final WeblogicWsClientConvention wsClientConvention, final SourceSet sourceSet, final JavaPluginConvention javaConvention) {
        final Project project = wsClientConvention.project

        final AbstractCompile compile = (AbstractCompile) project.tasks.create(GEN_CLIENT_TASK_NAME, WeblogicWsClientCompileTask.class);
        compile.description = String.format("Compiles the %s.%s.", sourceSet.name, 'wsclient')

        ConventionMapping conventionMapping = compile.conventionMapping
        conventionMapping.map("classpath", new Callable<Object>() {
            public Object call() throws Exception {
                return sourceSet.getCompileClasspath();
            }
        });
        conventionMapping.map("dependencyCacheDir", new Callable<Object>() {
            public Object call() throws Exception {
                return javaConvention.getDependencyCacheDir();
            }
        });
        conventionMapping.map("destinationDir", new Callable<Object>() {
            public Object call() throws Exception {
                return project.file("${project.buildDir}/wsclient/${sourceSet.name}");
            }
        });

        compile.doLast {
            // Kopier ressurser generert av genTask inn i compile sin output.
            // Java-kildekode skal ikke med, ei heller class-filene weblogic har generert fra upatchede java-filer.
            project.copy {
                into compile.getDestinationDir()
                from { project.tasks.withType(WeblogicGenClientTask).collect { WeblogicGenClientTask genTask -> genTask.destinationDir } }
                exclude '**/*.java'
                exclude '**/*.class'
            }

            // Kopier ressurser og patchede java-filer inn i gen-katalogen slik at IntelliJ IDEA ser dem.
            project.delete(wsClientConvention.genDir) //no dirty files
            project.copy { //source files
                into wsClientConvention.genDir
                from { project.tasks.withType(WeblogicGenClientTask).collect { WeblogicGenClientTask genTask -> genTask.destinationDir } }
                exclude '**/*.class'
            }
        }

        //registrerer output til sourceSet
        sourceSet.output.dir {
            compile.getDestinationDir()
        }

        //registrerer generert source til sourceSet
        sourceSet.allSource.srcDir {
            project.file(wsClientConvention.genDir)
        }


        // clean
        compile.doFirst {
            project.delete(compile.getDestinationDir())
        }

        return compile;
    }



    private SourceSet configureSourceSet(final JavaPluginConvention javaConvention) {
        SourceSetContainer sourceSets = javaConvention.getSourceSets()

        SourceSet sourceSet = sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)
        if (!sourceSet) {
            sourceSet = sourceSets.create(SourceSet.MAIN_SOURCE_SET_NAME) //legger til nytt sourcesett
        }

        return sourceSet;
    }

    static void conditionallyAddWeblogicDependencies(Project project) {
        if (project.hasProperty("WEBLOGIC_HOME")) {
            if (project.hasProperty("WEBLOGIC_VERSION")) {
                def wlsVersion = project.property("WEBLOGIC_VERSION");
                def wlsJars = weblogicJarsFor(wlsVersion as String, project);

                project.getDependencies().add(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME, wlsJars);
            }
        }

    }

    static FileCollection weblogicJarsFor(String wlsVersion, Project project) {
        def WEBLOGIC_HOME = project.property("WEBLOGIC_HOME");

        if (wlsVersion.startsWith("12.")) {
            if (wlsVersion.startsWith("12.1.2")) {
                return project.files(
                        "${WEBLOGIC_HOME}/wlserver/modules/databinding.override_1.2.0.0.jar",
                        "${WEBLOGIC_HOME}/wlserver/server/lib/weblogic.jar",
                );
            }
            if (wlsVersion.startsWith("12.1.3")) {
                return project.files(
                        "${WEBLOGIC_HOME}/wlserver/modules/databinding.override_1.2.0.0.jar",
                        "${WEBLOGIC_HOME}/wlserver/server/lib/weblogic.jar",
                );
            }

            if (wlsVersion.startsWith("12.1")) {
                return project.files(
                        "${WEBLOGIC_HOME}/wlserver/modules/databinding.override_1.2.0.0.jar",
                        "${WEBLOGIC_HOME}/wlserver/server/lib/weblogic.jar",
                );
            }

            project.logger.warn("WARNING: no optimalization found for weblogic version " + wlsVersion);
            return project.fileTree(dir: WEBLOGIC_HOME, includes: [
                    "wlserver/modules/databinding.override_*.jar",
                    "wlserver/server/lib/weblogic.jar",
            ]);
        }


        if (wlsVersion.startsWith("10.3")) {
            //tidligere prosjektoppsett splittet ikke weblogic classpath ned i forskjellige deler og benyttet her samtlige jarfiler som var nødvendig for å kompilere i IntellJ
            if (wlsVersion.startsWith("10.3.6")) {
                return project.files(
                        "${WEBLOGIC_HOME}/wlserver_10.3/server/lib/weblogic.jar",
                        "${WEBLOGIC_HOME}/wlserver_10.3/server/lib/webservices.jar",
                        "${WEBLOGIC_HOME}/wlserver_10.3/server/lib/wljmsclient.jar",
                        "${WEBLOGIC_HOME}/modules/javax.annotation_1.0.0.0_1-0.jar",
                        "${WEBLOGIC_HOME}/modules/javax.servlet_1.0.0.0_2-5.jar",

                        "${WEBLOGIC_HOME}/modules/javax.ejb_3.0.1.jar",
                        "${WEBLOGIC_HOME}/modules/javax.interceptor_1.0.jar",
                        "${WEBLOGIC_HOME}/modules/javax.transaction_1.0.0.0_1-1.jar",

                        "${WEBLOGIC_HOME}/modules/com.bea.core.transaction_2.7.1.0.jar",
                        "${WEBLOGIC_HOME}/modules/com.bea.core.datasource6_1.10.0.0.jar",
                        "${WEBLOGIC_HOME}/modules/glassfish.jaxws.rt_1.3.0.0_2-1-5.jar",
                )
            }
            if (wlsVersion.startsWith("10.3.5")) {
                return project.files(
                        "${WEBLOGIC_HOME}/wlserver_10.3/server/lib/weblogic.jar",
                        "${WEBLOGIC_HOME}/wlserver_10.3/server/lib/webservices.jar",
                        "${WEBLOGIC_HOME}/wlserver_10.3/server/lib/wljmsclient.jar",
                        "${WEBLOGIC_HOME}/modules/javax.annotation_1.0.0.0_1-0.jar",
                        "${WEBLOGIC_HOME}/modules/javax.servlet_1.0.0.0_2-5.jar",

                        "${WEBLOGIC_HOME}/modules/javax.ejb_3.0.1.jar",
                        "${WEBLOGIC_HOME}/modules/javax.interceptor_1.0.jar",
                        "${WEBLOGIC_HOME}/modules/javax.transaction_1.0.0.0_1-1.jar",

                        "${WEBLOGIC_HOME}/modules/com.bea.core.transaction_2.7.0.0.jar",
                        "${WEBLOGIC_HOME}/modules/com.bea.core.datasource6_1.9.0.0.jar",
                        "${WEBLOGIC_HOME}/modules/glassfish.jaxws.rt_1.2.0.0_2-1-5.jar",
                )
            }

            project.logger.warn("WARNING: no optimalization found for weblogic version " + wlsVersion);
            return project.fileTree(dir: WEBLOGIC_HOME, includes: [
                    "wlserver_10.3/server/lib/weblogic.jar",
                    "wlserver_10.3/server/lib/webservices.jar",
                    "wlserver_10.3/server/lib/wljmsclient.jar",
                    "modules/javax.annotation_*.jar",
                    "modules/javax.servlet_*.jar",

                    "modules/javax.ejb_3.0.1.jar",
                    "modules/javax.interceptor_1.0.jar",
                    "modules/javax.transaction_*.jar",

                    "modules/com.bea.core.transaction_*.jar",
                    "modules/com.bea.core.datasource6_*.jar",
                    "modules/glassfish.jaxws.rt_*.jar",
            ]);
        }

        throw new Exception("Unsupported weblogic version found - please add support for " + wlsVersion);
    }


}
