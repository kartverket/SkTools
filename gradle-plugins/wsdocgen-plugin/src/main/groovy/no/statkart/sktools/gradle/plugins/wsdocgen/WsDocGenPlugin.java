package no.statkart.sktools.gradle.plugins.wsdocgen;

import no.statkart.sktools.gradle.plugins.wsdocgen.internal.WsDocGroupContainer;
import org.gradle.api.Action;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.util.GUtil;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * Dokumentasjonsgenerering av {@code *WSBean.java} - JAX-WS implementasjon på server.
 * <p>
 * Til hvert registrert {@link SourceSet} utvider pluginet med vedhengsfunksjonalitet; se {@link WsDocGroupContainer}
 *
 * @author Leif Lislegård
 * @since 1.0
 */
@NonNullApi
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
        project.getPluginManager().apply(JavaBasePlugin.class);

        //common super task
        configureGenTask(project);
        configureArchives(project);

        configureSourceSetDefaults(project);

        configureDocgenDependencies(project);
    }

    private static void configureSourceSetDefaults(final Project project) {
        final AbstractArchiveTask archiveTask = (AbstractArchiveTask) project.getTasks().getByName(ARCHIVE_TASK_NAME);

        //for hvert source sett som finnes/blir lagt til
        project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(sourceSet -> {
            final WsDocGroupContainer container = new WsDocGroupContainer(project, sourceSet);

            //hekter inn utvidelser på source settet
            sourceSet.getExtensions().add(CONVENTION_NAME, container);

            container.all(new Action<WsDocGroup>() {
                //samletask for alle grupper for dette source settet
                final String commonSourceSetTaskName = "gen" + GUtil.toCamelCase(sourceSet.getName()) + "Wsdoc";

                @Override
                public void execute(final WsDocGroup group) {

                    if (!group.getTargetPath().isPresent()) {
                        group.getTargetPath().set(
                            project.getLayout().getBuildDirectory()
                                .dir(sourceSet.getName() + "/wsdoc/" + group.name)
                                .map(Directory::getAsFile)
                        );
                    }

                    Task task = createWsDocGenForGroupTask(project, sourceSet, group);

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
        });
    }


    static void configureGenTask(Project project) {
        project.task(GEN_TASK_NAME);
    }

    static void configureArchives(Project project) {
        Zip zip = project.getTasks().create(ARCHIVE_TASK_NAME, Zip.class);
        zip.getArchiveClassifier().set(CONVENTION_NAME);

        project.getArtifacts().add(Dependency.ARCHIVES_CONFIGURATION, zip);
    }


    private static WsDocCompileTask createWsDocGenForGroupTask(Project project, SourceSet sourceSet, WsDocGroup group) {
        final String taskName = group.getWsdocTaskName();
        WsDocCompileTask task = project.getTasks().create(taskName, WsDocCompileTask.class);

        //setting conventional properties
        task.setSource(sourceSet.getAllJava());
        task.setClasspath(project.files(
            (Callable<FileCollection>) sourceSet::getCompileClasspath
        ));
        task.setDestinationDir(group.getTargetPath());
        task.getLookupPath().set(group.getLookupPath());
        task.getEncoding().set(group.getEncoding());
        task.getServiceXsltFile().set(group.getServiceXsltPath());
        task.getIndexXsltFile().set(group.getIndexXsltPath());

        return task;
    }

    /**
     * Docgen processor as dependency (needed on runtime classpath)
     */
    private static void configureDocgenDependencies(final Project project) {
        final FileCollection wsDocGenConfiguration;

        Properties testProperties = injectedTestProperties();
        if (testProperties == null) {
            ScriptHandler buildscript = project.getBuildscript().getRepositories().isEmpty() ? project.getRootProject().getBuildscript() : project.getBuildscript(); //root projects repo configuration
            wsDocGenConfiguration = buildscript.getConfigurations().detachedConfiguration(wsDocGenDependency(project));
        } else {
            String classpath = testProperties.getProperty("sktools_wsdocgen_classpath");
            // En trenger classpath for annotasjonsprosessor (wsdoc)
            // disse ligger i egen modul
            wsDocGenConfiguration = project.files((Object[]) classpath.split(File.pathSeparator)); //NB: for GradleRunner i debug mode
        }


        project.getTasks().withType(WsDocCompileTask.class, task -> task.getOptions().setAnnotationProcessorPath(wsDocGenConfiguration));
    }

    static Dependency wsDocGenDependency(Project project) {
        Object dependencyNotation = Objects.requireNonNull(pluginProperties.getProperty("sktools_wsdocgen"), "Skal settes av byggesystem");
        return project.getDependencies().create(dependencyNotation);
    }

    /**
     * Test properties når man kjører tester, ellers null.
     */
    @Nullable
    static Properties injectedTestProperties() {
        InputStream stream = WsDocGenPlugin.class.getResourceAsStream("/WsDocGenPluginTest.properties");
        //dersom denne finnes på classpath kjører man tester
        return stream == null ? null : GUtil.loadProperties(stream);
    }

}
