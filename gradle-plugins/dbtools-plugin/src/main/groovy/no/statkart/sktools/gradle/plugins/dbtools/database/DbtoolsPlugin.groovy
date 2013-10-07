package no.statkart.sktools.gradle.plugins.dbtools.database

import java.sql.Driver
import java.sql.DriverManager
import org.gradle.api.Plugin
import org.gradle.api.Project
import no.statkart.sktools.gradle.plugins.dbtools.database.util.PatchConfiguration
import org.gradle.api.artifacts.Configuration

import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleExportTask
import org.gradle.api.internal.ConventionTask
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleImportTask

import org.gradle.api.plugins.JavaBasePlugin

import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractSQLTask
import org.gradle.api.Task
import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractDatabaseConvention

/**
 * Gradle plugin for database-moduler.
 *
 * <p>
 * <h5>Bruksanvisning</h5>
 *
 * <pre>
 *   <code>

apply plugin: 'sktools-dbtools-plugin'

//see {@link DbtoolsConvention#configureDatabasePlugin(Closure) }
configureDatabasePlugin {

    ...

}

 *   </code>
 * </pre>
 *     En modul kan betjene flere databaser samtidig. Disse blir satt opp via egne *Convention instanser.
 *
 *     @see DbtoolsConvention
 */
class DbtoolsPlugin implements Plugin<Project>  {
    public static final String CONVENTION_NAME = "db";
    public static final String DBTOOLS_CONFIGURATION = "dbTools";
    public static final String CHECK_TASK_NAME = "check";

    private static final Set<String> loadedDrivers = new HashSet<String>();

    public DbtoolsConvention dbtoolsConvention


    def void apply(final Project project) {
        dbtoolsConvention = new DbtoolsConvention(project)
        project.getConvention().getPlugins().put(CONVENTION_NAME, dbtoolsConvention);

        configureConfiguration(project)
        assignConventionMappings(project)

        configureTest(project, dbtoolsConvention); //SKTOOLS-81
        configureInfo(project, dbtoolsConvention); //SKTOOLS-88

        project.afterEvaluate {
            assignConventionalValues(project);
            registerDrivers(project);
        }
    }

    /** @since 1.3 - SKTOOLS-88  **/
    private Task configureInfo(final Project project, final DbtoolsConvention pluginConvention) {
        final Task checkSQLTasks = project.tasks.create('info') {
            description = 'Displays current configuration of dbToolsets'
            group = 'help'
            doLast {
                println "Dbtools configuration for ${project.path}:"

                dbtoolsConvention.dbToolSets.each { String name, AbstractDatabaseConvention toolset ->
                    println "\n\nInfo for toolset ${CONVENTION_NAME}.dbToolSets['${name}'] (prefix: '${toolset.prefix}')"
                    toolset.printInfo()
                }
                if (dbtoolsConvention.dbToolSets.isEmpty()) {
                    println "\n\nNo toolsets defined."
                }
            }
        }
        return checkSQLTasks
    }


    /** @since 1.3 - SKTOOLS-81  **/
    private Task configureTest(final Project project, final DbtoolsConvention pluginConvention) {
        final Task checkSQLTasks = project.tasks.create('checkSQLTasks') {
            description = 'Verifies configuration of SQLTasks'
            group = JavaBasePlugin.VERIFICATION_GROUP
            doLast {
                project.tasks.withType(AbstractSQLTask.class) { AbstractSQLTask task ->
                    try {
                        task.validate() //SKTOOLS-81
                    } catch (Throwable t) {
                        logger.error "Error when validating task ${task.path}"
                    }
                }
            }
        }
        project.afterEvaluate {
            Task checkTask = project.getTasks().findByName(CHECK_TASK_NAME) ?: project.task(CHECK_TASK_NAME, description: 'Checks the dbTools configuration', group: JavaBasePlugin.VERIFICATION_GROUP)
            checkTask.dependsOn checkSQLTasks
        }
        return checkSQLTasks
    }

    private Configuration configureConfiguration(Project project) {
        Configuration configuration = project.configurations.create(DBTOOLS_CONFIGURATION);
        return configuration
    }

    void assignConventionMappings(Project project) {
        PatchConfiguration.assignConventionMappings(project)

        //SKTOOLS-40: setter parallell dersom -Dparallel=<nr> er angitt
        def setParallelClosure = { ConventionTask it ->
            def systemProperties = project.gradle.getStartParameter().getSystemPropertiesArgs()
            if (systemProperties.containsKey('parallel')) {
                it.conventionMapping.with {
                    map 'parallel', {
                        Integer.parseInt(systemProperties.get('parallel'))
                    }
                }
            }
        }

        project.tasks.withType(OracleExportTask.class, setParallelClosure)
        project.tasks.withType(OracleImportTask.class, setParallelClosure)
    }

    void assignConventionalValues(Project project) {
        dbtoolsConvention.dbToolSets.values().each {

            //setter default properties
            if (it.properties == null) {
                Map<String, Object> props = new HashMap<String,Object>()
                project.properties.each {
                    if (it.value instanceof CharSequence || it.value instanceof Number || it.value instanceof Boolean) {
                        props.put(it.key, String.valueOf(it.value))
                    }
                }
                it.properties = props;
            }

        }

    }

    private void registerDrivers(Project project) {

        //For å kunne benytte jdbc funksjonalitet, må jdbc klasser være lastet inn i classloader til groovy.
        URLClassLoader groovyClassloader = GroovyObject.class.classLoader
        project.configurations[DBTOOLS_CONFIGURATION].files.each {File file ->
            groovyClassloader.addURL(file.toURL())
        }

        dbtoolsConvention.dbToolSets.values().collect { it.driver }.each {
            String driverAsString = it

            if (!loadedDrivers.contains(driverAsString)) {
                project.logger.info("Registring jdbc-driver: ${driverAsString}")
                Class driver = groovy.lang.GroovyObject.class.classLoader.loadClass(driverAsString)

                // You might need one or both of these as well
                Driver instance = driver.newInstance()
                DriverManager.registerDriver(instance)

                loadedDrivers.add(driverAsString)
            }
        }
    }


}



