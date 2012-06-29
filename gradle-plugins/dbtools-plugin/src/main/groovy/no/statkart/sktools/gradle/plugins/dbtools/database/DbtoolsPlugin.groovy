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
    private static final Set<String> loadedDrivers = new HashSet<String>();

    public DbtoolsConvention convention


    def void apply(final Project project) {
        convention = new DbtoolsConvention(project)
        project.convention.plugins.db = convention

        project.afterEvaluate {
            assignConventionalValues(project);
            registerDrivers(project);
        }
    }

    void assignConventionalValues(Project project) {
        convention.dbToolSets.values().each {

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
        convention.dbToolSets.values().collect { it.driver }.each {
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



