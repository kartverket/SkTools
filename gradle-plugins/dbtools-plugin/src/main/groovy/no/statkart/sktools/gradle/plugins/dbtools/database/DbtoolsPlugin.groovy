package no.statkart.sktools.gradle.plugins.dbtools.database

import java.sql.Driver
import java.sql.DriverManager
import org.gradle.api.Plugin
import org.gradle.api.Project
import no.statkart.sktools.gradle.plugins.dbtools.database.util.PatchConfiguration
import org.gradle.api.artifacts.Configuration

import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.api.invocation.Gradle
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleExportTask
import org.gradle.api.internal.ConventionTask
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleImportTask

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
    public final static String CONVENTION_NAME = "db";
    public final static String DBTOOLS_CONFIGURATION = "dbTools";

    private static final Set<String> loadedDrivers = new HashSet<String>();

    public DbtoolsConvention dbtoolsConvention


    def void apply(final Project project) {
        dbtoolsConvention = new DbtoolsConvention(project)
        project.getConvention().getPlugins().put(CONVENTION_NAME, dbtoolsConvention);

        configureConfigurations(project)

        assignConventionMappings(project)

        project.afterEvaluate {
            assignConventionalValues(project);
            registerDrivers(project);
        }
    }

    private void configureConfigurations(Project project) {
        Configuration configuration = project.configurations.add(DBTOOLS_CONFIGURATION);
    }

    void assignConventionMappings(Project project) {
        PatchConfiguration.assignConventionMappings(project)

        //SKTOOLS-40: setter parallell dersom -Dparallel=<nr> er angitt
        def setParallelClosure = { ConventionTask it ->
            def systemProperties = project.gradle.getStartParameter().getMergedSystemProperties()
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



