package no.statkart.sktools.gradle.plugins.wsdocgen;

import groovy.lang.Closure;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.util.Configurable;

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
 * Til hvert registrerte {@link SourceSet} utvider pluginet med vedhengsfunksjonalitet (exstension).
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

    TaskProvider<Zip> archiveTaskProvider;
    TaskProvider<Task> genTaskProvider;

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaBasePlugin.class);

        //common super task
        configureGenTask(project);
        configureArchives(project);

        configureSourceSetDefaults(project);

        configureDocgenDependencies(project);
    }

    private void configureSourceSetDefaults(final Project project) {

        //for hvert source sett som finnes/blir lagt til
        project.getExtensions().getByType(SourceSetContainer.class).configureEach(sourceSet -> {
            //hekter inn utvidelser på source settet
            sourceSet.getExtensions().add(CONVENTION_NAME, new WsDocExtension(project, sourceSet));
        });
    }

    class WsDocExtension implements Callable<WsDocGroup>, Configurable<WsDocGroup> {
        final Project project;
        final SourceSet sourceSet;

        WsDocGroup group = null;

        WsDocExtension(Project project, SourceSet sourceSet) {
            this.project = project;
            this.sourceSet = sourceSet;
        }

        @Override
        public WsDocGroup configure(Closure closure) {
            if (group == null) {
                attachNewWsDocGroup();
            }
            project.configure(group, closure);
            return group;
        }

        private void attachNewWsDocGroup() {
            group = new WsDocGroup(project, "group", sourceSet);
            group.getTargetPath().set(defaultTargetPath());

            //samletask for alle grupper for dette source settet
            TaskProvider<WsDocCompileTask> commonSourceSetTask = createWsDocGenForGroupTask(project, sourceSet, group);

            WsDocGenPlugin.this.genTaskProvider.configure(task -> task.dependsOn(commonSourceSetTask));
            WsDocGenPlugin.this.archiveTaskProvider.configure(zip -> zip.from(commonSourceSetTask));
        }

        @Override
        public WsDocGroup call() {
            return group;
        }

        protected Provider<File> defaultTargetPath() {
            return project.getLayout().getBuildDirectory()
                .dir(sourceSet.getName() + "/wsdoc")
                .map(Directory::getAsFile);
        }


        /**
         * Compatibility with pre SKTOOLS-213 syntax
         */
        @Deprecated //kan fjernes i sktools 8
        WsDocGroup group(Closure<?> closure) {
            project.getLogger().warn("Deprecated syntax: wsdoc.group - see SKTOOLS-213 for details");
            return configure(closure);
        }
    }

    void configureGenTask(Project project) {
        genTaskProvider = project.getTasks().register(GEN_TASK_NAME);
    }

    void configureArchives(Project project) {
        archiveTaskProvider = project.getTasks().register(ARCHIVE_TASK_NAME, Zip.class, zip ->
            zip.getArchiveClassifier().set(CONVENTION_NAME));

        project.getArtifacts().add(Dependency.ARCHIVES_CONFIGURATION, archiveTaskProvider);
    }


    private static TaskProvider<WsDocCompileTask> createWsDocGenForGroupTask(Project project, SourceSet sourceSet, WsDocGroup group) {
        final String taskName = "gen" + Utils.toCamelCase(sourceSet.getName()) + "Wsdoc";
        return project.getTasks().register(taskName, WsDocCompileTask.class, task -> {
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
        });
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
        return stream == null ? null : Utils.loadProperties(stream);
    }

}
