package no.statkart.sktools.gradle.plugins.xjc

import no.statkart.sktools.gradle.plugins.xjc.internal.XjcSchemaContainer
import no.statkart.sktools.gradle.plugins.xjc.internal.XjcSourceSetExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import java.util.concurrent.Callable
import org.gradle.api.Action
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.HasConvention
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.file.ConfigurableFileCollection


/**
 * Genererer JAXB java klasser basert på <code>*.xsd<code> filer. <br />
 * Pluginen baserer seg på {@code JavaBasePlugin} og integrerer seg med deklarerte {@link SourceSet}.
 *
 * For hvert {@code SourceSet} plugges det inn mulighet for ekstra konfigurasjon. Se {@link no.statkart.sktools.gradle.plugins.xjc.internal.XjcSourceSetExtension }
 *
 * Se dokumentasjon for <i>xjc-plugins</i> modul for bruk av utvidelser.
 * <ul>
 *  <li>{@link com.sun.tools.xjc.addon.statkart.GrunnbokDocPlugin}
 *  <li>{@link com.sun.tools.xjc.addon.statkart.ListGenPlugin}
 * </ul>
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
@SuppressWarnings("UnnecessaryQualifiedReference")
class XjcPlugin implements Plugin<ProjectInternal> {

    final static String CONVENTION_NAME = 'xjc'
    final static String JAXB_CONFIGURATION_NAME = 'jaxb'

    @Override
    void apply(ProjectInternal project) {
        project.apply plugin: JavaBasePlugin.class

        final Configuration configuration = createConfiguration(project);
        final SourceSet sourceSet = configureSourceSets(project, configuration)

        configureJaxbXjcDependencies(project);
    }

    /**
     * Utvider sourceSets med xjc tillegg
     */
    private void configureSourceSets(final ProjectInternal project, final Configuration configuration) {

        final JavaBasePlugin javaBasePlugin = project.getPlugins().getPlugin(JavaBasePlugin.class)

        //for hvert source sett som finnes/blir lagt til
        final JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class)

        javaConvention.getSourceSets().all(new Action<SourceSet>() {
            public void execute(final SourceSet sourceSet) {
                final XjcSchemaContainer xjcSchemaContainer = new XjcSchemaContainer(sourceSet, project.getFileResolver());

                //hekter inn utvidelser på source settet
                ((HasConvention) sourceSet).getConvention().getPlugins().put(CONVENTION_NAME, new XjcSourceSetExtension(xjcSchemaContainer));

                //hekter inn generert resultat og legger dette compile classpath
                final FileCollection xjcCompileClasspath = sourceSet.getCompileClasspath();
                final ConfigurableFileCollection xjcOutputClasses = project.files();
                sourceSet.setCompileClasspath( xjcCompileClasspath.plus(xjcOutputClasses) ); //SKTOOLS-129: ikke compile output på compile classpath for xjc.. (ellers vil ikke task bli up-to-date)

                xjcSchemaContainer.all(new Action<XjcSourceDirectorySet>() {
                    void execute(XjcSourceDirectorySet xjcSchema) {
                        //setter ingen default plassering av kildefiler for sourceSet - dette må eksplisitt deklareres i konfigurasjon

                        final File genOutputDir = project.file(xjcSchema.config.genOutputPath)
                        final File buildOutputDir = project.file("${project.getBuildDir()}/classes/${xjcSchema.getName()}")

                        Task xjcTask = createXjcTaskForSourceSet(xjcSchema, genOutputDir).dependsOn(
                                configuration,
                        );

                        AbstractCompile compileTask = createCompileXjcTaskForSchema(xjcSchema, xjcTask, buildOutputDir).dependsOn(
                                project.getConfigurations().getByName(sourceSet.getCompileConfigurationName()),
                        )

                        sourceSet.compiledBy(compileTask); //SKTOOLS-48

                        compileTask.source(sourceSet.getJava()) //for evt ListAdapter implementasjon osv
                        project.tasks[sourceSet.getCompileJavaTaskName()].dependsOn(compileTask); //javaCompile depends on this to be compiled

                        //legger til output til classpath
                        xjcOutputClasses.from(buildOutputDir)

                        //legger til output katalog til sourceset
                        sourceSet.output.dir(buildOutputDir, builtBy: compileTask)

                        //legger til generert kildekode slik at de kan bli plukket opp av dokumentajonsverktøy, kildekode distribusjon mm
                        sourceSet.getAllJava().srcDir(genOutputDir);
                        //legger også til kildekode for xsd filer
                        sourceSet.getAllSource().srcDirs(xjcSchema);


                        project.tasks.clean.delete(genOutputDir) //SKTOOLS-10: clean sletter genererte filer

                    }


                    private XjcTask createXjcTaskForSourceSet(XjcSourceDirectorySet xjcSchema, File genOutputDir) {
                        final String taskName = xjcSchema.config.genTaskName;
                        XjcTask task = (XjcTask) project.task(type: XjcTask.class, taskName);
                        task.getConventionMapping().with {
                            map("source", new Callable() {
                                public Object call() {
                                    return xjcSchema;
                                }
                            });
                            map("config", new Callable() {
                                public Object call() {
                                    return xjcSchema.getConfig();
                                }
                            });
                            map("outputDirectory", new Callable() {
                                public Object call() {
                                    return genOutputDir;
                                }
                            });
                        }
                        return task
                    }

                    private AbstractCompile createCompileXjcTaskForSchema(XjcSourceDirectorySet xjcSchema, Task xjcTask, File buildOutputDir) {
                        final AbstractCompile compile = (AbstractCompile) project.tasks.create(xjcSchema.config.compileTaskName, XjcCompile.class);

                        javaBasePlugin.configureForSourceSet(sourceSet, compile);

                        compile.setDescription("Compiles the XCJ generated schema files");
                        compile.setSource(xjcTask);
                        compile.setDestinationDir(buildOutputDir);
                        compile.setClasspath(xjcCompileClasspath);

                        compile.doFirst { project.delete(getDestinationDir()) } //SKTOOLS-48

                        return compile;
                    }


                });
            }
        });

    }

    /**
     * jaxb libs (needed on runtime classpath)
     */
    private static Configuration createConfiguration(Project project) {
        Configuration configuration = project.configurations.create(JAXB_CONFIGURATION_NAME).setVisible(false).setDescription("Classpath for jaxb library and extensions.");
        return configuration;
    }

    /**
     * xjc-plugin and jaxb configuration
     */
    private static def configureJaxbXjcDependencies(final ProjectInternal project) {
        final def buildscript = project.getRootProject().getBuildscript(); //root projects repo configuration
        final Configuration processorConfiguration = buildscript.getConfigurations().detachedConfiguration(xjcDependency(project));
        final Configuration jaxbConfiguration = project.getConfigurations().getByName(JAXB_CONFIGURATION_NAME);

        project.getTasks().withType(XjcTask.class, new Action<XjcTask>() {
            @Override
            void execute(XjcTask task) {
                task.setClasspath(jaxbConfiguration.plus(processorConfiguration))
            }
        })
    }

    private static Dependency[] xjcDependency(ProjectInternal project) {
        ArrayList<Dependency> dependencies = new ArrayList<Dependency>(1);
        if (!runningInIDEATestEnvironment()) {
            dependencies.add(wsDocGenDependency(project));
        }
        return dependencies.toArray(new Dependency[dependencies.size()]);
    }

    private static Dependency wsDocGenDependency(ProjectInternal project) {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put("group", "no.statkart.sktools");
        props.put("name", "xjc-plugins");
        props.put("version", XjcPlugin.class.getPackage().getImplementationVersion()); //manifest informasjon satt ifra byggesystem
        return project.getDependencies().create(props);
    }

    static boolean runningInIDEATestEnvironment() {
        return !XjcPlugin.class.getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(".jar");
    }

}
