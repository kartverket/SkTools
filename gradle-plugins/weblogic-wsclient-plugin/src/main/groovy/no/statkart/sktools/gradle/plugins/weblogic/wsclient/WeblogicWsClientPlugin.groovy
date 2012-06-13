package no.statkart.sktools.gradle.plugins.weblogic.wsclient

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.plugins.JavaPluginConvention
import no.statkart.sktools.gradle.plugins.weblogic.WeblogicBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.compile.AbstractCompile

import org.gradle.api.plugins.BasePlugin

import org.gradle.api.artifacts.Dependency

import org.gradle.api.GradleException
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.internal.ConventionMapping

import java.util.concurrent.Callable
import org.gradle.api.tasks.SourceSetContainer

/**
 * Baserer seg på {@code JavaBasePlugin} og integrerer med {@code JavaPlugin} dersom denne aktiveres.
 *
 * @since 1.1
 * @author Leif Lislegård
 */
class WeblogicWsClientPlugin implements Plugin<Project> {

    public static final String CONVENTION_NAME = 'weblogicWsClient'
    public static final String GEN_CLIENT_TASK_NAME = 'genWeblogicWsClient'


    @Override
    void apply(Project project) {
        project.apply plugin: JavaBasePlugin.class;
        project.apply plugin: WeblogicBasePlugin.class;

        JavaPluginConvention javaConvention = project.getConvention().getPlugins().get("java");

        //konfigurerer opp et source sett
        final SourceSet sourceSet = configureSourceSet(javaConvention);

        WeblogicWsClientConvention wsClientConvention = new WeblogicWsClientConvention(project);
        project.convention.plugins.put(WeblogicWsClientPlugin.CONVENTION_NAME, wsClientConvention);

        //task for henting av schema filer
        Task provideSchema = createCollectSchemaTask(project, 'collectSchemaIfNotSpecified').dependsOn(
                project.configurations.getByName(Dependency.DEFAULT_CONFIGURATION),     //dependencier blir registrert til 'default'.
        );

        //task for generering av client source
        WeblogicGenClientTask genClientSourceTask = (WeblogicGenClientTask) createGenerateSourceTask(wsClientConvention, sourceSet, provideSchema).dependsOn(
                provideSchema.name,
                project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_CONFIGURATION_NAME), //tvinger rekompilering ved endring i classpath + weblogic jar filer.
        );

        Task compileTask = createCompileTask(wsClientConvention, sourceSet, genClientSourceTask).dependsOn(
                genClientSourceTask.path,
        )

        //hekter inn genClient ved kjøring av 'resources' task.
        Task processWeblogicResources = project.tasks.getByName(sourceSet.processResourcesTaskName).dependsOn(
                WeblogicWsClientPlugin.GEN_CLIENT_TASK_NAME,
        );

        //hekter inn genClient ved kjøring av 'compile' task.
        Task compileWeblogicResources = project.tasks.getByName(sourceSet.compileJavaTaskName).dependsOn(
                WeblogicWsClientPlugin.GEN_CLIENT_TASK_NAME,
        );


        project.afterEvaluate {
            setConventionalDefaults(wsClientConvention, sourceSet)
        }


    }

    private def setConventionalDefaults(WeblogicWsClientConvention wsClientConvention, SourceSet sourceSet) {
        if (wsClientConvention.genDir == null) {
            wsClientConvention.genDir = "gen/${sourceSet.name}/wsclient"
        }
    }



    private Task createCollectSchemaTask(final Project project, final String taskName) {

        Task task = project.task(taskName) {

            //legger til input for beregening av up to date
            getInputs().property('definedWebServicesInConvention', new Callable() {
                Object call() {
                    WeblogicWsClientConvention wsClientConvention = project.getConvention().getPlugins().get(WeblogicWsClientPlugin.CONVENTION_NAME);
                    return wsClientConvention.webService
                }
            });

            doLast {
                WeblogicWsClientConvention wsClientConvention = project.getConvention().getPlugins().get(WeblogicWsClientPlugin.CONVENTION_NAME);

                wsClientConvention.webService.eachWithIndex { WebServiceConfig webservice, idx ->


                    if (webservice.schemaFiles != null) {
                        //dersom schemaFiles er angitt benyttes denne

                    } else if (webservice.baseWar != null) {
                        //dersom baseWar er gitt, beregnes schema filer ifra baseWar war filer

                        UnionFileCollection resolvedFiles = new UnionFileCollection()

                        Collection<File> warFiles = wsClientConvention.project.configurations.getByName(Dependency.DEFAULT_CONFIGURATION).files(webservice.baseWar).findAll {
                            it.getName().toLowerCase().endsWith(".war")
                        }

                        warFiles.each { File file ->
                            resolvedFiles.add(project.zipTree(file).matching(webservice.matching))
                        }

                        //oppdaterer convention
                        webservice.schemaFiles = resolvedFiles
                    } else {
                        throw new GradleException("Enten shamaFiles eller baseWar må anngis i ${WeblogicWsClientPlugin.class.simpleName}-convention")
                    }

                    getOutputs().files(webservice.schemaFiles) //registrerer output for task

                } //end of each webservice config iteration
            }
        }
        return task;
    }




    /**
     * Oppretter task for kodegenerering av klient-stubber.
     *
     * TaskOutput blir lagt til som javasource for sourceSet. NB: denne FileCollection kan kun inneholde Dirs (ikke filer osv.)
     */
    private WeblogicGenClientTask createGenerateSourceTask(final WeblogicWsClientConvention wsClientConvention, final SourceSet sourceSet, final Task provideSchema) {
        final Project project = wsClientConvention.project

        WeblogicGenClientTask genTask = (WeblogicGenClientTask) project.task(type: WeblogicGenClientTask.class, sourceSet.getTaskName('gen', 'wsClientSource'))
        genTask.setDescription(String.format("Generates WS-client source based on Weblogic tools."));
        genTask.setGroup(BasePlugin.BUILD_GROUP);

        ConventionMapping conventionMapping = genTask.getConventionMapping();

        conventionMapping.map("source", new Callable() {
            public Object call() {
                return provideSchema.getOutputs().getFiles().getAsFileTree();  //default source er hva provideSchema task genererer
            }
        })

        //setter ikke classpath da denne ikke trengs

        conventionMapping.map("destinationDir", new Callable<Object>() {
            public Object call() throws Exception {
                return project.file("${project.buildDir}/wsclient/${sourceSet.name}-gen");
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
    private WeblogicWsClientCompileTask createCompileTask(final WeblogicWsClientConvention wsClientConvention, final SourceSet sourceSet, final AbstractCompile genTask) {
        final Project project = wsClientConvention.project

        WeblogicWsClientCompileTask compile = (WeblogicWsClientCompileTask) project.task(type: WeblogicWsClientCompileTask, WeblogicWsClientPlugin.GEN_CLIENT_TASK_NAME)
        compile.setDescription(String.format("Compiles the %s.%s.", sourceSet.name, 'wsclient'));

        ConventionMapping conventionMapping = compile.conventionMapping
        conventionMapping.map("source", new Callable() {
            public Object call() {
                return project.files(genTask.getDestinationDir()).getAsFileTree();
            }
        });
        conventionMapping.map("classpath", new Callable<Object>() {
            public Object call() throws Exception {
                return sourceSet.getCompileClasspath();
            }
        });
        conventionMapping.map("destinationDir", new Callable<Object>() {
            public Object call() throws Exception {
                return project.file("${project.buildDir}/wsclient/${sourceSet.name}");
            }
        });

        // kopierer inn ressurser
        compile.doLast {
            project.copy {  //class files
                into compile.getDestinationDir()
                from genTask.getDestinationDir()
                exclude '**/*.java'
            }
            project.copy { //source files
                into wsClientConvention.genDir
                from genTask.getDestinationDir()
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
            sourceSet = sourceSets.add(SourceSet.MAIN_SOURCE_SET_NAME) //legger til nytt sourcesett
        }

        return sourceSet;
    }

}
