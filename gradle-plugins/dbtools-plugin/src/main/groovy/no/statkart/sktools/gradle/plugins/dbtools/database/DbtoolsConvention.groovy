package no.statkart.sktools.gradle.plugins.dbtools.database

import org.gradle.api.Project
import org.apache.commons.lang.NotImplementedException
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleTasksConvention
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleTasks
import java.sql.Driver
import java.sql.DriverManager
import org.gradle.api.Task
import no.statkart.sktools.gradle.plugins.dbtools.database.hsqldb.HsqldbTasksConvention

/**
 * Pluginen kan konfigureres til å håndtere flere ulike databaser og flere instanser av denne.
 * <p>
 *     For at pluginen kan utføre JDBC kall mot databasen trenger en å registrere driverene. Dette konfigureres via {@link DbtoolsConvention#useDrivers(Object) }
 * </p>
 *
 *
 * Konfigurasjon skjer via {@link DbtoolsConvention#configureDatabasePlugin(Closure)}
 *
 * <pre>
 *   <code>

configurations {
    drivers
}

dependencies {
    drivers "com.oracle:ojdbc6:11.2.0.2.0@jar"
}

configureDatabasePlugin {

    useDrivers configurations.drivers

    useToolset 'oracle', 'Db', 'mineScript' {

        ... //for details, see {@link DbtoolsConvention#useToolset(String, String, String, Closure) }


    }

}

 *   </code>
 * </pre>
 *
 */
public class DbtoolsConvention {
    private final Project project;
    private final Set<String> loadedDrivers = new HashSet<String>();

    public final Map<String, ?> env = new HashMap<String, Object>()

    Task buildSQLTask

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

    /**
     * Følgende toolset er tilgjengelige:
     *
     * <ul>
     *     <li>'oracle' - se {@link OracleTasksConvention#config(Closure)} </li>
     *     <li>'hsqldb' - se {@link HsqldbTasksConvention#config(Closure)} </li>
     * </ul>
     *
     * <p>
     *
     * Eksempel for å legge til egne oracle targets:
     *
     * <pre><code>

configureDatabasePlugin {
     useToolset 'oracle', 'Db', 'mineScript' {

        url = "jdbc:oracle:thin:@oraclehost:1521:testbase"
        credentials.username = 'sa'
        credentials.password = ''

        //annen oracle config her. Se {@link OracleTasksConvention}
     }
}
     * </code></pre>
     *
     * Utifra dette genereres det opp tasks som kan kjøres på bakgrunn av filer som ligger  i ./scr/mineScript/&#42&#42/&#42.sql <br />
     * Taskene vil bli eksekvert med konfigurert url, username og password. <br />
     * Taskene vil bli navngitt <b>&lt;prefix&gt;&lt;filnavn&gt; </b><br />
     * Standard tasks for import og eksport vil også bli lagt til. <br />
     *
     *
     * <p>
     * <p>
     * Pluginen støtter flere samtidige toolset.
     *
     *
     * @param type type toolset (oracle eller hsqldb)
     * @param prefix prefiks for alle tasks for toolsett
     * @param path plassering for sql script
     * @param closure konfigurasjon av toolset
     * @return
     */
    def useToolset(String type, String prefix, String path, Closure closure) {

        project.logger.info("Adding ${type} toolset for ${prefix} (${path})...")

        if ('oracle'.equalsIgnoreCase(type)) {
            return addOracleToolset(prefix, path, closure)

        } else if ('hsqldb'.equalsIgnoreCase(type)) {
            return addHsqldbToolset(prefix, path, closure)

        } else {
            throw new NotImplementedException("Ukjent verktøyset/database")
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

    private def registerDriver(String driverAsString) {
        if (!loadedDrivers.contains(driverAsString)) {
            project.logger.info("Registring jdbc-driver: ${driverAsString}")
            Class driver = groovy.lang.GroovyObject.class.classLoader.loadClass(driverAsString)

            // You might need one or both of these as well
            Driver instance = driver.newInstance()
            DriverManager.registerDriver(instance)

            loadedDrivers.add(driverAsString)
        }
    }

    private def addOracleToolset(String prefix, String path, Closure closure) {
        OracleTasksConvention convention = env.get(prefix)

        if (convention == null) {
            project.logger.info("Applying Oracle convention to ${path} ...")
            convention = new OracleTasksConvention(project, prefix)
            env.put(prefix, convention)

            project.logger.info('Adding default tools for Oracle...')
            OracleTasks.addDefaultTools('Database', convention)
        }

        convention.config(closure)

        registerDriver(convention.driver)

        convention.addTasks(path)

        return convention
    }

    private def addHsqldbToolset(String prefix, String path, Closure closure) {
        HsqldbTasksConvention convention = env.get(prefix)

        if (convention == null) {
            project.logger.info("Applying HSQLDB convention to ${path} ...")
            convention = new HsqldbTasksConvention(project, prefix)
            env.put(prefix, convention)

//            project.logger.info('Adding default tools for HSQLDB...')
//            HsqldbTasks.addDefaultTools('Database', convention)
        }

        convention.config(closure)

        registerDriver(convention.driver)

        convention.addTasks(path)

        return convention
    }

}
