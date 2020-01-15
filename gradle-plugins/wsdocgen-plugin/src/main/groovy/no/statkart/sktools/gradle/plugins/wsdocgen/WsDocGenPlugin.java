package no.statkart.sktools.gradle.plugins.wsdocgen;

import no.statkart.sktools.gradle.plugins.wsdocgen.internal.WsDocGroupContainer;
import no.statkart.sktools.gradle.plugins.wsdocgen.internal.WsDocSourceSetExtension;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.internal.HasConvention;
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata;
import org.gradle.util.GUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * Dokumentasjonsgenerering av {@code *WSBean.java} - JAX-WS implementasjon på server.
 *
 * Til hvert registrert {@link SourceSet} utvider pluginet med vedhengsfunksjonalitet; se {@link WsDocGroupContainer}
 *
 * @author Leif Lislegård
 * @since 1.0
 */
public class WsDocGenPlugin implements Plugin<Project> {

    public final static String CONVENTION_NAME = "wsdoc";
    public final static String GEN_TASK_NAME = "genWsdoc";
    public final static String ARCHIVE_TASK_NAME = "packWsdoc";

    public static final Properties pluginProperties = new Properties();
    static {
        try {
            pluginProperties.load(WsDocGenPlugin.class.getResourceAsStream("/no/statkart/sktools/wsdocgen-gradle-plugin.properties"));
        } catch (IOException ignored) {
            System.err.println("Error loading plugin properties!");
        }
    }

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaBasePlugin.class);

        final WsDocGenConvention convention = new WsDocGenConvention();
        project.getConvention().getPlugins().put(CONVENTION_NAME, convention);

        //common super task
        configureGenTask(project);
        configureArchives(project);

        configureSourceSetDefaults(project, convention);

        configureDocgenDependencies(project);
    }

    private static void configureSourceSetDefaults(final Project project, final WsDocGenConvention convention) {
        final AbstractArchiveTask archiveTask = (AbstractArchiveTask) project.getTasks().getByName(ARCHIVE_TASK_NAME);

        //for hvert source sett som finnes/blir lagt til
        project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(new Action<SourceSet>() {
            public void execute(final SourceSet sourceSet) {
                final WsDocGroupContainer container = new WsDocGroupContainer(sourceSet, convention);

                //hekter inn utvidelser på source settet
                ((HasConvention) sourceSet).getConvention().getPlugins().put(CONVENTION_NAME, new WsDocSourceSetExtension(container));

                container.all(new Action<WsDocGroup>() {
                    //samletask for alle grupper for dette source settet
                    /** @see WsDocGenConvention#GEN_TASK_NAME_PATTERN */
                    final String commonSourceSetTaskName = "gen" + GUtil.toCamelCase(sourceSet.getName()) + "Wsdoc";

                    @Override
                    public void execute(final WsDocGroup group) {

                        //Legger til evt defaultverdier
                        if (group.includes == null) {
                            group.include("**/*Bean.java");
                        }
                        if (group.targetPath == null) {
                            group.targetPath(project.relativePath(project.getBuildDir()) + "/" + sourceSet.getName() + "/wsdoc/" + group.name);
                        }


                        AbstractCompile task = createWsDocGenForGroupTask(project, group);
                        //setting conventional properties
                        task.getConventionMapping().map("source", new Callable() {   //tildeler en dynamisk default verdi
                            public Object call() {
                                return sourceSet.getAllJava();  //default source
                            }
                        });
                        task.getConventionMapping().map("classpath", new Callable() {
                            public Object call() {
                                return sourceSet.getCompileClasspath();
                            }
                        });
                        task.getConventionMapping().map("destinationDir", new Callable() {
                            public Object call() {
                                return project.file(group.targetPath);
                            }
                        });

                        maybeCreateSourceSetSuperTask().dependsOn(task);
                        archiveTask.from(task);

                    }

                    private Task sourceSetSuperTask = null;
                    Task maybeCreateSourceSetSuperTask() {
                        if (sourceSetSuperTask == null) {
                            sourceSetSuperTask = project.task(commonSourceSetTaskName);
                            project.getTasks().getByName(GEN_TASK_NAME).dependsOn(commonSourceSetTaskName);
                        }
                        return sourceSetSuperTask;
                    }
                });
            }

        });
    }


    static void configureGenTask(Project project) {
        project.task(GEN_TASK_NAME);
    }

    static void configureArchives(Project project) {
        Zip zip = project.getTasks().create(ARCHIVE_TASK_NAME, Zip.class);
        zip.setClassifier(CONVENTION_NAME);

        project.getArtifacts().add(Dependency.ARCHIVES_CONFIGURATION, new ArchivePublishArtifact(zip));
    }


    private static AbstractCompile createWsDocGenForGroupTask(Project project, WsDocGroup docGroup) {
        final String taskName = docGroup.getWsdocTaskName();
        final WsDocCompileTask task = project.getTasks().create(taskName, WsDocCompileTask.class);
        //SKTOOLS-131: configuration have to be configured at this point
        task.init(docGroup);

        return task;
    }

    /**
     * Docgen processor as dependency (needed on runtime classpath)
     */
    private static void configureDocgenDependencies(final Project project) {
        final FileCollection wsDocGenConfiguration;

        InputStream testKitMetadataStream = testEnvironmentClasspath();
        if (testKitMetadataStream == null) {
            ScriptHandler buildscript = project.getBuildscript().getRepositories().isEmpty() ? project.getRootProject().getBuildscript() : project.getBuildscript(); //root projects repo configuration
            wsDocGenConfiguration = buildscript.getConfigurations().detachedConfiguration(wsDocGenDependency(project));
        } else {
            Properties properties = GUtil.loadProperties(testKitMetadataStream);
            String classpath = properties.getProperty(PluginUnderTestMetadata.IMPLEMENTATION_CLASSPATH_PROP_KEY);
            // En trenger classpath for annotasjonsprosessor (wsdoc)
            // disse ligger i egen modul
            wsDocGenConfiguration = project.files((Object[]) classpath.split(";")); //NB: for GradleRunner i debug mode
        }


        project.getTasks().withType(WsDocCompileTask.class, new Action<WsDocCompileTask>() {
            @Override
            public void execute(WsDocCompileTask task) {
                task.getOptions().setAnnotationProcessorPath(wsDocGenConfiguration);
            }
        });
    }

    static Dependency wsDocGenDependency(Project project) {
        Object dependencyNotation = Objects.requireNonNull(pluginProperties.getProperty("sktools_wsdocgen"), "Skal settes av byggesystem");
        return project.getDependencies().create(dependencyNotation);
    }

    /**
     * Classpath satt opp for Gradle TestKit
     */
    static InputStream testEnvironmentClasspath() {
        return WsDocGenPlugin.class.getResourceAsStream('/' + PluginUnderTestMetadata.METADATA_FILE_NAME); //dersom denne finnes på classpath kjører man tester
    }

}
