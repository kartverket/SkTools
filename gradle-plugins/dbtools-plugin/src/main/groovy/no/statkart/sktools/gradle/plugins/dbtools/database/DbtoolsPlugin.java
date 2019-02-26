package no.statkart.sktools.gradle.plugins.dbtools.database;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleExportTask;
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleImportTask;
import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractDatabaseConvention;
import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractSQLTask;
import no.statkart.sktools.gradle.plugins.dbtools.database.util.PatchConfiguration;
import org.codehaus.groovy.runtime.MethodClosure;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.execution.TaskExecutionGraphListener;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaBasePlugin;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Gradle plugin for database-moduler.
 * <p/>
 * <p/>
 * <h5>Bruksanvisning</h5>
 * <p/>
 * <pre>
 *   <code>
 *
 * apply plugin: 'sktools-dbtools-plugin'
 *
 * //see {@link DbtoolsConvention#configureDatabasePlugin(Closure) }
 * configureDatabasePlugin {
 *
 * ...
 *
 * }
 *
 *   </code>
 * </pre>
 * En modul kan betjene flere databaser samtidig. Disse blir satt opp via egne *Convention instanser.
 *
 * @see DbtoolsConvention
 */
public class DbtoolsPlugin implements Plugin<Project> {
    public static final String CONVENTION_NAME = "db";
    public static final String DBTOOLS_CONFIGURATION = "dbTools";
    public static final String CHECK_TASK_NAME = "check";

    public DbtoolsConvention dbtoolsConvention;


    public void apply(final Project project) {
        dbtoolsConvention = new DbtoolsConvention(project);
        project.getConvention().getPlugins().put(CONVENTION_NAME, dbtoolsConvention);

        final Configuration configuration = project.getConfigurations().create(DBTOOLS_CONFIGURATION);
        assignConventionMappings(project);

        configureTest(project); //SKTOOLS-81
        configureInfo(project); //SKTOOLS-88

        project.afterEvaluate(new Action<Project>() {
            @Override
            public void execute(Project project) {
                assignConventionalValues(project);
            }
        });
        loadDrivers(configuration, project);
    }

    /**
     * @since 1.3 - SKTOOLS-88
     **/
    private Task configureInfo(final Project project) {
        final Task infoTask = project.task("info");
        infoTask.setDescription("Displays current configuration of dbToolsets");
        infoTask.setGroup("help");
        infoTask.doLast(new Action<Task>() {
            final Logger logger = project.getLogger();

            @Override
            public void execute(Task task) {
                logger.quiet("Dbtools configuration for {}", project.getPath());

                for (Map.Entry<String, ? extends AbstractDatabaseConvention> entry : dbtoolsConvention.dbToolSets.entrySet()) {
                    logger.quiet("\n\nInfo for toolset {}.dbToolSets['{}'] (prefix: '{}')", CONVENTION_NAME, entry.getKey(), entry.getValue());
                }
                if (dbtoolsConvention.dbToolSets.isEmpty()) {
                    logger.quiet("\n\nNo toolsets defined.");
                }
            }
        });
        return infoTask;
    }

    /**
     * @since 1.3 - SKTOOLS-81
     **/
    private static Task configureTest(final Project project) {
        final Task checkSQLTasks = project.task("checkSQLTasks");
        checkSQLTasks.setDescription("Verifies configuration of SQLTasks");
        checkSQLTasks.setGroup(JavaBasePlugin.VERIFICATION_GROUP);
        checkSQLTasks.doLast(new Action<Task>() {
            @Override
            public void execute(Task task) {
                task.getProject().getTasks().withType(AbstractSQLTask.class, new Action<AbstractSQLTask>() {
                    @Override
                    public void execute(AbstractSQLTask task) {
                        try {
                            task.validate(); //SKTOOLS-81
                        } catch (Throwable t) {
                            task.getProject().getLogger().error("Error when validating task %s", task.getPath());
                        }
                    }
                });
            }
        });

        project.afterEvaluate(new Action<Project>() {
            @Override
            public void execute(Project project) {
                Task checkTask = project.getTasks().findByName(CHECK_TASK_NAME);
                if (checkTask == null) {
                    checkTask = project.task(CHECK_TASK_NAME);
                    checkTask.setDescription("Checks the dbTools configuration");
                    checkTask.setGroup(JavaBasePlugin.VERIFICATION_GROUP);
                    checkTask.dependsOn(checkSQLTasks);
                }
            }
        });

        return checkSQLTasks;
    }

    static void assignConventionMappings(Project project) {
        PatchConfiguration.assignConventionMappings(project);
        //SKTOOLS-40: setter parallell dersom -Dparallel=<nr> er angitt
        final Map<String, String> systemPropertiesArgs = project.getGradle().getStartParameter().getSystemPropertiesArgs();
        if (systemPropertiesArgs.containsKey("parallel")) {
            final Action<ConventionTask> setParallel = new Action<ConventionTask>() {
                @Override
                public void execute(ConventionTask task) {
                    task.getConventionMapping().map("parallel", new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            return Integer.valueOf(systemPropertiesArgs.get("parallel"));
                        }
                    });
                }
            };

            project.getTasks().withType(OracleExportTask.class, setParallel);
            project.getTasks().withType(OracleImportTask.class, setParallel);
        }
    }

    void assignConventionalValues(Project project) {
        for (AbstractDatabaseConvention databaseConvention : dbtoolsConvention.dbToolSets.values()) {
            // Setter default properties
            if (databaseConvention.getProperties() == null) {
                Map<String, Object> props = new HashMap<>();
                for (Map.Entry<String, ?> propertyEntry : project.getProperties().entrySet()) {
                    Object value = propertyEntry.getValue();
                    if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
                        props.put(propertyEntry.getKey(), String.valueOf(value));
                    }
                }
                databaseConvention.setProperties(props);
            }
        }
    }

    private static void loadDrivers(final Configuration configuration, final Project project) {
        // Konfigurasjon skal IKKE resolves i konfigurasjonsfasen (Gradle 3.x)
        // - resolver configuration etter at prosjektet er initialisert
        project.getGradle().getTaskGraph().addTaskExecutionGraphListener(new TaskExecutionGraphListener() {
            //GroovyCastException: Cannot cast object 'org.gradle.internal.classloader.MutableURLClassLoader
            final ClassLoader groovyClassloader = GroovyObject.class.getClassLoader();
            final MethodClosure addURLClosure = new MethodClosure(groovyClassloader, "addURL");

            @Override
            public void graphPopulated(TaskExecutionGraph taskExecutionGraph) {
                for (File file : configuration.getFiles()) {
                    //For å kunne benytte jdbc funksjonalitet, må jdbc klasser være lastet inn i classloader til groovy.
                    try {
                        addURLClosure.call(file.toURI().toURL());
                    } catch (MalformedURLException e) {
                        throw new Error("Implementation error", e);
                    }
                }
            }
        });
    }


}
