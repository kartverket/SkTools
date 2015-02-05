package no.statkart.sktools.gradle.plugins.wsdocgen;

import no.statkart.sktools.gradle.plugins.wsdocgen.internal.WsDocGroupContainer;
import no.statkart.sktools.gradle.plugins.wsdocgen.internal.WsDocSourceSetExtension;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.internal.HasConvention;
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.util.GUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;

/**
 * Dokumentasjonsgenerering av {@code *WSBean.java} - JAX-WS implementasjon på server.
 *
 * Til hvert registrert {@link SourceSet} utivider pluginet med vedhengsfunksjonalitet; se {@link WsDocGroupContainer}
 *
 * @author Leif Lislegård
 * @since 1.0
 */
public class WsDocGenPlugin implements Plugin<ProjectInternal> {

    public final static String CONVENTION_NAME = "wsdoc";
    public final static String GEN_TASK_NAME = "genWsDoc";
    public final static String ARCHIVE_TASK_NAME = "packWsDoc";

    @Override
    public void apply(ProjectInternal project) {
        project.getPlugins().apply(JavaBasePlugin.class);

        final WsDocGenConvention convention = new WsDocGenConvention();
        project.getConvention().getPlugins().put(CONVENTION_NAME, convention);

        //common super task
        configureGenTask(project);
        configureArchives(project);

        configureSourceSetDefaults(project, convention);

        configureDocgenDependencies(project);
    }

    private void configureSourceSetDefaults(final ProjectInternal project, final WsDocGenConvention convention) {
        final AbstractArchiveTask archiveTask = (AbstractArchiveTask) project.getTasks().getByName(ARCHIVE_TASK_NAME);

        //for hvert source sett som finnes/blir lagt til
        project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(new Action<SourceSet>() {
            public void execute(final SourceSet sourceSet) {
                final WsDocGroupContainer container = new WsDocGroupContainer(sourceSet, convention);

                //hekter inn utvidelser på source settet
                ((HasConvention) sourceSet).getConvention().getPlugins().put(CONVENTION_NAME, new WsDocSourceSetExtension(container));

                container.all(new Action<WsDocGroup>() {
                    //samletask for alle grupper for dette source settet
                    final String commonSourceSetTaskName = String.format(convention.GEN_TASK_NAME_PATTERN, GUtil.toCamelCase(sourceSet.getName()), "");

                    @Override
                    public void execute(final WsDocGroup group) {

                        //Legger til evt defaultverdier
                        if (group.includes == null) {
                            group.include("**/*Bean.java");
                        }
                        if (group.targetPath == null) {
                            group.targetPath(String.format("%s/%s/wsdoc/%s", project.relativePath(project.getBuildDir()), sourceSet.getName(), group.name));
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
    private static void configureDocgenDependencies(final ProjectInternal project) {
        final ScriptHandler buildscript = project.getRootProject().getBuildscript(); //root projects repo configuration
        final Configuration wsDocGenConfiguration = buildscript.getConfigurations().detachedConfiguration(wsDocgenDependency(project));

        project.getTasks().withType(WsDocCompileTask.class, new Action<WsDocCompileTask>() {
            @Override
            public void execute(WsDocCompileTask task) {
                task.setProcessorClasspath(wsDocGenConfiguration);
            }
        });
    }

    static Dependency[] wsDocgenDependency(ProjectInternal project) {
        ArrayList<Dependency> dependencies = new ArrayList<Dependency>(1);
        if (!runningInIDEATestEnvironment()) {
            dependencies.add(wsDocGenDependency(project));
        }
        return dependencies.toArray(new Dependency[dependencies.size()]);
    }

    private static Dependency wsDocGenDependency(ProjectInternal project) {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put("group", "no.statkart.sktools");
        props.put("name", "wsdocgen");
        props.put("version", WsDocGenPlugin.class.getPackage().getImplementationVersion()); //manifest informasjon satt ifra byggesystem
        return project.getDependencies().create(props);
    }

    static boolean runningInIDEATestEnvironment() {
        return !WsDocGenPlugin.class.getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(".jar");
    }


}
