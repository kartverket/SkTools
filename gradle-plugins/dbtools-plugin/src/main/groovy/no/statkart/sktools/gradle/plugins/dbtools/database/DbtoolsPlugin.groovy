package no.statkart.sktools.gradle.plugins.dbtools.database

import java.sql.Driver
import java.sql.DriverManager
import org.gradle.api.Plugin
import org.gradle.api.Project

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

    private static final Set<String> loadedDrivers = new HashSet<String>();

    public DbtoolsConvention dbtoolsConvention


    def void apply(final Project project) {
        dbtoolsConvention = new DbtoolsConvention(project)
        project.getConvention().getPlugins().put(CONVENTION_NAME, dbtoolsConvention);

        project.afterEvaluate {
            assignConventionalValues(project);
            registerDrivers(project);
        }
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



