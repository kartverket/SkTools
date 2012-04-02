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
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar

import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet

import org.gradle.api.tasks.ConventionValue
import org.gradle.api.plugins.Convention
import org.gradle.api.internal.IConventionAware
import org.gradle.api.artifacts.Dependency

import org.gradle.api.GradleException
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.internal.ConventionMapping

import java.util.concurrent.Callable

/**
 * Legger til et java source sett med navn {@code 'weblogic'} (se {@link WeblogicWsClientPlugin#WEBLOGIC_SOURCE_SET_NAME})
 *
 * Baserer seg på {@code JavaBasePlugin} og integrerer med {@code JavaPlugin} dersom denne aktiveres.
 *
 * @since 1.1
 * @author Leif Lislegård
 */
class WeblogicWsClientPlugin implements Plugin<Project> {

    public static final String CONVENTION_NAME = 'weblogicWsClient'
    public static final String WEBLOGIC_SOURCE_SET_NAME = 'weblogic'
    public static final String GEN_CLIENT_TASK_NAME = 'genWeblogicWsClient'
    public static final String PROCESS_WEBLOGIC_RESOURCES_TASK_NAME = 'processWeblogicResources'
    public static final String COMPILE_WEBLOGIC_TASK_NAME = 'compileWeblogicJava'
    public static final String WEBLOGIC_JAR_TASK_NAME = 'weblogicJar'

    @Override
    void apply(Project project) {
        project.apply plugin: WeblogicBasePlugin.class;

        JavaPluginConvention javaConvention = project.getConvention().getPlugins().get("java");

        //konfigurerer opp et source sett for tilfeller der man ønsker å legge til ekstra kode/resources samt javadoc generering/dokumentasjon.
        final SourceSet sourceSet = configureSourceSet(javaConvention);

        WeblogicWsClientConvention wsClientConvention = new WeblogicWsClientConvention(project, sourceSet);
        project.convention.plugins.put(WeblogicWsClientPlugin.CONVENTION_NAME, wsClientConvention);

        //task for henting av schema filer
        Task provideSchema = createCollectSchemaTask(project, 'collectSchemaIfNotSpecified').dependsOn(
                project.configurations.getByName(Dependency.DEFAULT_CONFIGURATION),     //dependencier blir registrert til 'default'.
        );

        //task for generering av client source
        Task genClientTask = createGenClientTask(wsClientConvention, sourceSet).dependsOn(
                provideSchema.name,
                project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_CONFIGURATION_NAME), //tvinger rekompilering ved endring i classpath + weblogic jar filer.
        );

        genClientTask.getConventionMapping().map("defaultSource", new ConventionValue() {
            public Object getValue(Convention conventionManager, IConventionAware conventionAwareObject) {
                return provideSchema.getOutputs().getFiles().getAsFileTree();  //default source er hva provideSchema task genererer
            }
        })

        //hekter inn genClient ved kjøring av 'resources' task.
        Task processWeblogicResources = project.tasks.getByName(WeblogicWsClientPlugin.PROCESS_WEBLOGIC_RESOURCES_TASK_NAME).dependsOn(
                WeblogicWsClientPlugin.GEN_CLIENT_TASK_NAME,
        );

        //task for artifakt
        Task archiveWsClient = configureArchives(wsClientConvention, sourceSet)


        project.afterEvaluate {
            //output for task blir lagt til java-sourceSet
            sourceSet.getJava().srcDirs(wsClientConvention.getGenDir())

            //output for task blir lagt til resources
            sourceSet.getResources().srcDirs(wsClientConvention.getGenDir())
        }


        //cleaner generert kode
        project.tasks.getByName(BasePlugin.CLEAN_TASK_NAME).doLast {
            project.delete(wsClientConvention.getGenDir())
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

                    } else if (webservice.dependency != null) {
                        //dersom dependency er gitt, beregnes schema filer ifra dependency war filer

                        UnionFileCollection resolvedFiles = new UnionFileCollection()

                        Collection<File> warFiles = wsClientConvention.project.configurations.getByName(Dependency.DEFAULT_CONFIGURATION).files(webservice.dependency).findAll {
                            it.getName().toLowerCase().endsWith(".war")
                        }

                        warFiles.each { File file ->
                            resolvedFiles.add(project.zipTree(file).matching(webservice.matching))
                        }

                        //oppdaterer convention
                        webservice.schemaFiles = resolvedFiles
                    } else {
                        throw new GradleException("Enten shamaFiles eller dependency må anngis i ${WeblogicWsClientPlugin.class.simpleName}-convention")
                    }

                    getOutputs().files(webservice.schemaFiles) //registrerer output for task

                } //end of each webservice config iteration
            }
        }
        return task;
    }


    private Task configureArchives(final WeblogicWsClientConvention wsClientConvention, final SourceSet sourceSet) {
        Project project = wsClientConvention.project;

        if (project.getTasks().findByName(JavaPlugin.TEST_TASK_NAME) != null) {
            project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(JavaPlugin.TEST_TASK_NAME);
        }
        Jar jar = project.getTasks().add(WeblogicWsClientPlugin.WEBLOGIC_JAR_TASK_NAME, Jar.class);
//        jar.getManifest().from(javaConvention.getManifest());
        jar.setDescription("Assembles a jar archive containing the WS-Client classes.");
        jar.setClassifier(WeblogicWsClientPlugin.WEBLOGIC_SOURCE_SET_NAME)
        jar.setGroup(BasePlugin.BUILD_GROUP);
        jar.from(sourceSet.getOutput());
//        jar.getMetaInf().from(new Callable() {
//            public Object call() throws Exception {
//                return javaConvention.getMetaInf();
//            }
//        });


        ArchivePublishArtifact artifact = new ArchivePublishArtifact(jar);
        project.getExtensions().getByType(DefaultArtifactPublicationSet.class).addCandidate(artifact);
        project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_CONFIGURATION_NAME).getArtifacts().add(artifact);
        return jar;
    }

    /**
     * Oppretter {@link WeblogicWsClientPlugin#GEN_CLIENT_TASK_NAME}
     *
     * Setter classpath og destinationDir som defaults via wsClientConvention.
     *
     * TaskOutput blir lagt til som javasource for sourceSet. NB: denne FileCollection kan kun inneholde Dirs (ikke filer osv.)
     */
    private Task createGenClientTask(final WeblogicWsClientConvention wsClientConvention, final SourceSet sourceSet) {
        final Project project = wsClientConvention.project

        AbstractCompile compileTask = (AbstractCompile) project.task(type: WeblogicGenClientTask.class, WeblogicWsClientPlugin.GEN_CLIENT_TASK_NAME)
        compileTask.setDescription(String.format("Generates WS-client source based on Weblogic tools."));
        compileTask.setGroup(BasePlugin.BUILD_GROUP);

        ConventionMapping conventionMapping = compileTask.getConventionMapping();
        conventionMapping.map("classpath", new ConventionValue() {
            public Object getValue(Convention convention, IConventionAware conventionAwareObject) {
                return sourceSet.getCompileClasspath();
            }
        });
        conventionMapping.map("destinationDir", new ConventionValue() {
            public Object getValue(Convention convention, IConventionAware conventionAwareObject) {
                return wsClientConvention.getGenDir()
            }
        });


        return compileTask
    }


    private SourceSet configureSourceSet(final JavaPluginConvention javaConvention) {
        SourceSet sourceSet = javaConvention.getSourceSets().add(WeblogicWsClientPlugin.WEBLOGIC_SOURCE_SET_NAME) //legger til nytt sourcesett

        sourceSet.getJava().exclude('*.class')
        sourceSet.getResources().exclude('*.class').exclude('*.java')

        return sourceSet;
    }

}
