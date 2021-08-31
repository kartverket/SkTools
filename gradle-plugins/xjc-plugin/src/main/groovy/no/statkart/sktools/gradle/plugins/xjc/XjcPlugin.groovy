package no.statkart.sktools.gradle.plugins.xjc

import no.statkart.sktools.gradle.plugins.xjc.internal.XjcSourceSetConvention
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GUtil

/**
 * Genererer JAXB java klasser basert på <code>*.xsd</code> filer. <br>
 * Pluginen baserer seg på {@link JavaPlugin} og integrerer seg med deklarerte {@link SourceSet}.
 *
 * <br>
 * <p> En trenger en versjon av xjc verktøyet sammen med jaxb implementasjon for kjøring av verktøyet.
 * Disse finner man i konfigurasjon med navn {@value #JAXB_CONFIGURATION_NAME}.
 * Dersom ikke noe angis, brukes en versjon av glassfish for xjc og jaxb.
 * For sktools prosjektet, se "default_xjc_implementation" og "default_jaxb_ri_implementation" og referanse til
 * <pre>
 libraries.jaxb_xjc ->  'org.glassfish.jaxb:jaxb-xjc'
 libraries.jaxb_rt -> 'org.glassfish.jaxb:jaxb-runtime'
 * </pre>
 * </p>
 *
 * <br>
 * <p> Tidligere Sun-koordinater er utgått og skal ikke benyttes. Før versjon 2.3.3 la man disse inn på classpath:
 * <pre>
 com.sun.xml.bind:jaxb-impl - Runtime
 com.sun.xml.bind:jaxb-core - Runtime
 com.sun.xml.bind:jaxb-xjc - Schema compiler
 * </pre>
 *
 * </p>
 *
 * For hvert {@code SourceSet} plugges det inn mulighet for ekstra konfigurasjon. Se {@link XjcSourceSetConvention }
 *
 * Se dokumentasjon for <i>xjc-plugins</i> modul for bruk av utvidelser.
 * <ul>
 *  <li>{@code com.sun.tools.xjc.addon.statkart.ListGenPlugin}
 * </ul>
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
@SuppressWarnings("UnnecessaryQualifiedReference")
class XjcPlugin implements Plugin<Project> {

    final static String CONVENTION_NAME = 'xjc'
    final static String JAXB_CONFIGURATION_NAME = 'jaxb'

    static final Properties pluginProperties = new Properties();
    static {
        pluginProperties.load(XjcPlugin.class.getResourceAsStream("/no/statkart/sktools/xjc-gradle-plugin.properties"))
    }

    @Override
    void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class)

        final Configuration configuration = createConfiguration(project);
        configureSourceSets(project, configuration)

        configureJaxbXjcDependencies(project, configuration);
    }

    /**
     * Utvider sourceSets med xjc tillegg
     */
    private void configureSourceSets(final Project project, final Configuration configuration) {

        //for hvert source sett som finnes/blir lagt til
        final JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class)

        javaConvention.getSourceSets().all(new Action<SourceSet>() {
            public void execute(final SourceSet sourceSet) {
                final NamedDomainObjectContainer<XjcConfig> xjcSchemas = project.container(XjcConfig.class, new NamedDomainObjectFactory<XjcConfig>() {
                    @Override
                    XjcConfig create(String name) {
                        String schemaName = sourceSet.getName() + GUtil.toCamelCase(name);
                        return new XjcConfig(schemaName, sourceSet, project);
                    }
                });

                //hekter inn utvidelser på source settet
                ((HasConvention) sourceSet).getConvention().getPlugins().put(CONVENTION_NAME, new XjcSourceSetConvention(xjcSchemas));

                xjcSchemas.all(new Action<XjcConfig>() {
                    void execute(XjcConfig xjcConfig) {
                        //setter ingen default plassering av kildefiler for sourceSet - dette må eksplisitt deklareres i konfigurasjon

                        TaskProvider<XjcTask> xjcTask = createXjcTaskForSourceSet(xjcConfig);

                        sourceSet.getJava().srcDir(xjcTask);

                    }


                    private TaskProvider<XjcTask> createXjcTaskForSourceSet(final XjcConfig config) {
                        final String taskName = config.genTaskName;
                        project.getTasks().register(taskName, XjcTask.class) { XjcTask task ->
                            task.dependsOn(configuration)
                            task.setSource(config.source.asFileTree)
                            task.setConfig(config)
                        }
                    }

                });

                //intellij stuff...
                project.plugins.withId('idea') {
                    project.afterEvaluate {
                        xjcSchemas.all(new Action<XjcConfig>() {
                            void execute(XjcConfig xjcConfig) {
                                project.idea.module.generatedSourceDirs += xjcConfig.genOutputPath.get()
                                project.tasks.named('ideaModule') {
                                    doFirst { project.mkdir(xjcConfig.genOutputPath) }
                                }
                            }
                        });
                    }
                }
            }
        });

    }

    /**
     * jaxb libs (needed on runtime classpath)
     */
    private static Configuration createConfiguration(Project project) {
        //default verdi for enkelt å komme igang / testing ...
        Configuration compileOnly = project.configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)
            .defaultDependencies(new Action<DependencySet>() {
                @Override
                void execute(DependencySet dependencies) {
                    /* As of jaxb 2.2.7 they have split the jaxb libraries into several components. xjc is now decoupled from any particular jaxb runtime. To fix this issue, ensure a jaxb runtime is made available on the classpath when executing xjc */
                    def jaxbNotation = Objects.requireNonNull(pluginProperties.getProperty("default_jaxb_ri_implementation"), "Skal settes av byggesystem")
                    def jaxbDependency = project.getDependencies().create(jaxbNotation)
                    dependencies.add(jaxbDependency)
                }
            })

        Configuration configuration = project.configurations.create(JAXB_CONFIGURATION_NAME).setVisible(false).setDescription("Classpath for jaxb library and extensions.");
        configuration.exclude([group: 'org.apache.ant', module: 'ant'])
        configuration.defaultDependencies(new Action<DependencySet>() {
            @Override
            void execute(DependencySet dependencies) {
                def xjcNotation = Objects.requireNonNull(pluginProperties.getProperty("default_xjc_implementation"), "Skal settes av byggesystem")
                dependencies.add(project.getDependencies().create(xjcNotation));
            }
        })
        configuration.extendsFrom(compileOnly)
        return configuration;
    }

    /**
     * xjc-plugin and jaxb configuration
     */
    private static def configureJaxbXjcDependencies(final Project project, final Configuration jaxbConfiguration) {
        final FileCollection processorConfiguration = processorClasspathForXjcExtension(project);

        project.getTasks().withType(XjcTask.class, new Action<XjcTask>() {
            @Override
            void execute(XjcTask task) {
                task.setClasspath(jaxbConfiguration.plus(processorConfiguration))
            }
        })
    }

    private static FileCollection processorClasspathForXjcExtension(Project project) {
        Properties testProperties = injectedTestProperties()
        if (testProperties != null) {
            def classpath = testProperties.getProperty("sktools_xjc_classpath")
            // En trenger classpath til egen-utvidelser av xjc (xjc plugins)
            // disse ligger i prosjektet no.statkart.sktools:xjc-plugins
            // avhengighet til jaxb og jaxb-xjc legges på fra annen konfigurasjon
            return project.files(classpath.split(File.pathSeparator)) //NB: for GradleRunner i debug mode
        }
        final def buildscript = project.getBuildscript().getRepositories().isEmpty() ? project.getRootProject().getBuildscript() : project.getBuildscript()
        return buildscript.getConfigurations().detachedConfiguration(wsDocGenDependency(project))
    }


    private static Dependency wsDocGenDependency(Project project) {
        ModuleDependency moduleDependency = (ModuleDependency) project.getDependencies().create(pluginProperties.getProperty("sktools_xjc_extensions"))
        moduleDependency.setTransitive(false) //ikke transitiv da en ønsker at configuration JAXB_CONFIGURATION_NAME skal diktere xjb/jaxb versjon
        return moduleDependency;
    }

    /**
     * Test properties når man kjører tester, ellers null.
     */
    static Properties injectedTestProperties() {
        InputStream stream = XjcPlugin.getResourceAsStream('/XjcPluginTest.properties')
        //dersom denne finnes på classpath kjører man tester
        return stream == null ? null : GUtil.loadProperties(stream);
    }

}
