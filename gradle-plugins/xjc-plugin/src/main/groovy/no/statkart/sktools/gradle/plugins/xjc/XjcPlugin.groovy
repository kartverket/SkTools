package no.statkart.sktools.gradle.plugins.xjc

import no.statkart.sktools.gradle.plugins.xjc.internal.XjcSchemaContainer
import no.statkart.sktools.gradle.plugins.xjc.internal.XjcSourceSetConvention
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.HasConvention
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet

import java.util.concurrent.Callable

/**
 * Genererer JAXB java klasser basert på <code>*.xsd<code> filer. <br />
 * Pluginen baserer seg på {@code JavaBasePlugin} og integrerer seg med deklarerte {@link SourceSet}.
 *
 * For hvert {@code SourceSet} plugges det inn mulighet for ekstra konfigurasjon. Se {@link XjcSourceSetConvention }
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
        project.apply plugin: JavaPlugin.class

        final Configuration configuration = createConfiguration(project);
        configureSourceSets(project, configuration)

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
                final XjcSchemaContainer xjcSchemas = new XjcSchemaContainer(sourceSet, project);

                //hekter inn utvidelser på source settet
                ((HasConvention) sourceSet).getConvention().getPlugins().put(CONVENTION_NAME, new XjcSourceSetConvention(xjcSchemas));

                xjcSchemas.all(new Action<XjcConfig>() {
                    void execute(XjcConfig xjcConfig) {
                        //setter ingen default plassering av kildefiler for sourceSet - dette må eksplisitt deklareres i konfigurasjon

                        final File genOutputDir = project.file(xjcConfig.genOutputPath)

                        Task xjcTask = createXjcTaskForSourceSet(xjcConfig, genOutputDir).dependsOn(
                                configuration,
                        );

                        sourceSet.getJava().srcDir(genOutputDir);
                        project.tasks.getByName(sourceSet.getCompileJavaTaskName()).dependsOn(xjcTask)

                        project.plugins.withId('idea') {
                            project.idea.module.generatedSourceDirs += genOutputDir
                        }
                    }


                    private XjcTask createXjcTaskForSourceSet(final XjcConfig config, File genOutputDir) {
                        final String taskName = config.genTaskName;
                        XjcTask task = (XjcTask) project.task(type: XjcTask.class, taskName);
                        task.getConventionMapping().with {
                            map("source", new Callable() {
                                public Object call() {
                                    return config.source.asFileTree;
                                }
                            });
                            map("config", new Callable() {
                                public Object call() {
                                    return config;
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
