package no.statkart.sktools.gradle.plugins.dbtools.database

import org.gradle.api.Project
import org.apache.commons.lang.NotImplementedException
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleDatabaseConvention
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleTasks
import java.sql.Driver
import java.sql.DriverManager

/**
 * Konfigurasjon skjer i {@link DbtoolsConvention#configureDatabasePlugin(Closure)}
 *
 *
 * Dette kan eksempelvis gjøres via:
 * <pre><code>
 *        useDrivers libraries.ojdbc
 *        useDrivers libraries.db2_jdbc
 * </code></pre>
 */
public class DbtoolsConvention {
    private Project project;
    private Set<String> loadedDrivers = new HashSet<String>();

    public String username
    public String password

    public final Map<String, ?> env = new HashMap<String, Object>()

    DbtoolsConvention(Project project) {
        this.project = project
    }

    /**
     * Configures this plugin by running closure defined in your project.
     *
     * Configuration methods available:
     * <ul>
     *     <li> {@link #useToolset(String, String, String, Closure)}
     *     <li> {@link #useDrivers(Object)}
     * </ul>
     */
    void configureDatabasePlugin(Closure closure) {
        closure.delegate = this
        closure()

    }

    public Map<String, Object> getEnvironments() {
        return env
    }

    def useToolset(String type, String prefix, String path, Closure closure) {

        project.logger.info("Adding ${type} toolset for ${prefix} (${path})...")

        if ('oracle'.equalsIgnoreCase(type)) {
            return addOracleToolset(type, prefix, path, closure)

        } else {
            throw new NotImplementedException("Kun støttet for oracle...")
        }
    }

    /**
     *  For å kunne benytte jdbc funksjonalitet, må jdbc klasser være lastet inn i classloader til gradle/groovy.
     */
    def useDrivers(def dependencies) {
        URLClassLoader loader = GroovyObject.class.classLoader
        dependencies.each {File file ->
            loader.addURL(file.toURL())
        }
    }

    private def addOracleToolset(String type, String prefix, String path, Closure closure) {
        OracleDatabaseConvention convention = env.get(prefix)

        if (convention == null) {
            project.logger.info("Applying oracle convention to ${path} ...")
            convention = new OracleDatabaseConvention(project, prefix)
            env.put(prefix, convention)

            project.logger.info('Adding default tools for oracle...')
            OracleTasks.addDefaultTools(project, 'Database', convention)
        }

        convention.config(closure)

        if (!loadedDrivers.contains(convention.driver)) {
            project.logger.info("Registring jdbc-driver: ${convention.driver}")
            Class driver = GroovyObject.class.classLoader.loadClass(convention.driver)

            // You might need one or both of these as well
            Driver instance = driver.newInstance()
            DriverManager.registerDriver(instance)

            loadedDrivers.add(convention.driver)
        }

        convention.addTasks(path)

        return convention
    }

}
