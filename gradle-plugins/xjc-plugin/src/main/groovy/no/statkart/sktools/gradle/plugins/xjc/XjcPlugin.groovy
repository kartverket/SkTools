package no.statkart.sktools.gradle.plugins.xjc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import java.util.concurrent.Callable
import org.gradle.api.Action
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.HasConvention
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.file.ConfigurableFileCollection

import no.statkart.sktools.gradle.plugins.xjc.internal.XjcCompileTaskImpl
import org.gradle.api.internal.ConventionMapping

/**
 * Genererer JAXB java klasser basert på <code>*.xsd<code> filer. <br />
 * Pluginen baserer seg på {@code JavaBasePlugin} og integrerer seg med deklarerte {@link SourceSet}s.
 *
 * For hvert sourceSet plugges det inn muglihet for ekstra konfiguasjon. Se {@link XjcSourceSetExtention }
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
class XjcPlugin implements Plugin<ProjectInternal> {

    final static String CONVENTION_NAME = 'xjc'
    final static String JAXB_CONFIGURATION_NAME = 'jaxb'

    @Override
    void apply(ProjectInternal project) {
        project.apply plugin: JavaBasePlugin.class

        final Configuration xjcConfiguration = createConfiguration(project);
        final SourceSet sourceSet = configureSourceSets(project, xjcConfiguration)

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
                XjcSourceSetExtention xjcSourceSet = new XjcSourceSetExtention(sourceSet, project.getFileResolver());

                //hekter inn utvidelser på source settet
                ((HasConvention) sourceSet).getConvention().getPlugins().put(CONVENTION_NAME, xjcSourceSet); // SKIF-195

                //hekter inn generert resultat og legger dette compile classpath
                ConfigurableFileCollection xjcOutputClasspath = project.files()
                sourceSet.setCompileClasspath( sourceSet.getCompileClasspath().plus(xjcOutputClasspath) )


                final String buildPath = project.relativePath(project.getBuildDir())

                xjcSourceSet.getXjc().all(new Action<XjcSchema>() {
                    void execute(XjcSchema xjcSchema) {
                        //setter ingen default plassering av kildefiler for sourceSet - dette må eksplisitt deklareres i konfigurasjon

                        File buildOutputDir = project.file("${buildPath}/classes/${xjcSchema.getName()}")
                        File genOutputDir = project.file(xjcSchema.getGeneratedSourcesDir())

                        //legger til output til classpath
                        xjcOutputClasspath.from(buildOutputDir)

                        //legger til output katalog til sourceset
                        sourceSet.output.dir(buildOutputDir)

                        //legger til generert kildekode slik at de kan bli plukket opp av dokumentajonsverktøy, kildekode distribusjon mm
                        sourceSet.getAllJava().srcDir(genOutputDir);
                        //legger også til kildekode for xsd filer
                        sourceSet.getAllSource().srcDirs(xjcSchema);


                        Task xjcTask = createXjcTaskForSourceSet(xjcSchema, genOutputDir).dependsOn(
                                configuration,
                        );

                        Task compileTask = createCompileXjcTaskForSchema(xjcSchema, xjcTask, buildOutputDir).dependsOn(
                                xjcTask,
                                project.getConfigurations().getByName(sourceSet.getCompileConfigurationName()),
                        )

                        project.tasks[sourceSet.getCompileJavaTaskName()].dependsOn(compileTask);

                    }


                    private XjcTask createXjcTaskForSourceSet(XjcSchema xjcSchema, File genOutputDir) {
                        XjcTask task = (XjcTask) project.task(type: XjcTask.class, xjcSchema.getGenerateXjcSchemaTaskName());
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
                            map("classpath", new Callable() {
                                public Object call() {
                                    //merge plugins dependencies with jaxb, putting jaxb first in classpath for optional override.
                                    return new UnionFileCollection(project.getConfigurations().getByName(XjcPlugin.JAXB_CONFIGURATION_NAME), findPluginClasspath(project));
                                }
                            });
                        }
                        return task
                    }

                    private Task createCompileXjcTaskForSchema(XjcSchema xjcSchema, Task xjcTask, File buildOutputDir) {
                        final AbstractCompile compile;
                        if (project.getGradle().getGradleVersion().compareTo("1.5") > 0 ) {
                            compile = (AbstractCompile) project.tasks.replace(xjcSchema.getCompileXjcSchemaTaskName(), XjcCompileTaskImpl.class)  //todo: endre bruk av replace() til create()
                        } else {
                            compile = (AbstractCompile) project.tasks.add(xjcSchema.getCompileXjcSchemaTaskName(), XjcCompileTaskImpl.class) //todo: remove backward compability with Gradle 1.5
                        }

                        javaBasePlugin.configureForSourceSet(sourceSet, compile);

                        compile.setDescription("Compiles the XCJ generated schema files");
                        compile.setSource(xjcTask);
                        compile.source(xjcSchema.getJava());  //for evt ListAdapter implementasjon osv
                        compile.setDestinationDir(buildOutputDir);

                        return compile;
                    }


                });
            }
        });

        project.getTasks().withType(XjcCompileTaskImpl.class, new Action<XjcCompileTaskImpl>() {
            public void execute(final XjcCompileTaskImpl compile) {
                ConventionMapping conventionMapping = compile.getConventionMapping();
                conventionMapping.map("dependencyCacheDir", new Callable<Object>() {
                    public Object call() throws Exception {
                        return javaConvention.getDependencyCacheDir();
                    }
                });
            }
        });


    }



    //todo: en bedre strategi her er eksplisitt å legge til dependencies. Dette kunne feks leses inn via en property fil for pluginet?
    // Ovenstående strategi har utfordring da en i test sammenheng ikke kan deklarere dependencies. Disse vil da feile når man ikke
    // har noe installert i noen repositories som er tilgjengelige.
    // TIPS: se hva som er gjort for dbtools-plugin
    private static FileCollection findPluginClasspath(final Project project) {

        Closure<Boolean> pluginDependencyMatcher = {Dependency dependency -> dependency.getGroup() == 'no.statkart.sktools.gradle' && dependency.getName() == 'xjc-plugin'}
        Closure<Boolean> samlepomDependencyMatcher = {Dependency dependency -> dependency.getGroup() == 'no.statkart.sktools.gradle' && dependency.getName() == 'gradle-plugins'}

        List<FileCollection> candidateFileCollections = [project, project.getRootProject()].collect {
            FileCollection resolvedFiles = project.files();
            Configuration buildConfiguration = it.getBuildscript().getConfigurations().getByName(ScriptHandler.CLASSPATH_CONFIGURATION);

            Dependency pluginDependency = buildConfiguration.dependencies.find (pluginDependencyMatcher);
            if (!pluginDependency.is(null)) {
                resolvedFiles = buildConfiguration.fileCollection(pluginDependency);
            } else {
                //forsøker å finne 'samlepom' [SKIF-154]
                Dependency samlepomDependency = buildConfiguration.dependencies.find (samlepomDependencyMatcher);
                if (!samlepomDependency.is(null)) {
                    resolvedFiles = buildConfiguration.fileCollection(samlepomDependency);
                }
            }
//            println "resolved files: ${resolvedFiles.files}"
            return resolvedFiles;
        }

        FileCollection candidate = candidateFileCollections.find { !it.isEmpty() }
        if (candidate == null) candidate = project.files(); //ok under eksekvering av test da man ikke bruker noen filer her.

        return candidate;
    }


    //classpath classpath for jaxb libs (needed at runtime)
    private Configuration createConfiguration(Project project) {
        Configuration configuration = project.configurations.create(JAXB_CONFIGURATION_NAME).setVisible(false).setDescription("Classpath for jaxb library and extensions.");
        return configuration;
    }

}
