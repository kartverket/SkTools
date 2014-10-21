package no.statkart.sktools.gradle.plugins.dbtools.database.util

/*
 For at denne biten kan fungere, må jdbc driveren finnes i classpath og være registrert i kjørende classloader.
 Registrering av denne i Gradle kan gjøres på følgende måte:

 Class driver = loader.loadClass('oracle.jdbc.OracleDriver')

 // You might need one or both of these as well
 Driver instance = driver.newInstance()
 DriverManager.registerDriver(instance)

 */

import groovy.sql.Sql
import no.statkart.sktools.utils.parsers.sql.model.Statement
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.SQLException

/**
 * Eksekverer sql statements til basen.
 *
 * Gjør kall til databasen med statements parset ifra enten fil eller streng.
 *
 * @since 1.2
 * @author Leif Lislegård
 */
class SQLExecutor {
    private static Logger logger = LoggerFactory.getLogger(SQLExecutor.class);

    List<Statement> statements

    public void executeStatements(ExecSpecs specs) {

        if (logger.isDebugEnabled()) {
            logger.debug("Calling executeStatements() with parameters \n\t " + [username:specs.username, password:specs.password, driver:specs.driver, url:specs.url])
        }

        waitForConnectionRate(15)

        def sql = Sql.newInstance(specs.url, specs.username, specs.password, specs.driver)
        println("connected to database: ${specs.url} [${specs.username}]")

        statements.each() { Statement statement ->

            logger.info("Executing ${statement.class.simpleName}: \n${statement.sql}")

            try {
                sql.execute(statement.sql)
            } catch (SQLException sqle) {

                if (specs.failOnError) {
                    logger.info("Exception: ", sqle)
                    if (!logger.isInfoEnabled()) {
                        logger.error("Statement: \n${statement.sql.trim()}\n")
                    }
                    logger.error("Message: \n${sqle.message}\n")
                } else {
                    logger.debug("Exception: ", sqle)
                    logger.warn("Message: \n${sqle.message}\n")
                }

                if (!specs.failOnError) {
                    logger.warn("Error when executing statement at line#${statement.lineNumber}... failOnError is '${specs.failOnError}' meaning we continue...")
                } else {
                    logger.error("Error when executing statement at line#${statement.lineNumber}")
                    throw sqle
                }

            }
        }

        sql.close() //SKTOOLS-59: release connection to database

    }


    private final LinkedHashSet<Long> INVOCATIONS = new LinkedHashSet<Long>();
    /**
     * SKTOOLS-59: optional limitation of number of connections per second
     * @param rateLimit
     */
    private void waitForConnectionRate(int rateLimit) {

        boolean messageLogged = false;

        while (INVOCATIONS.size() >= rateLimit) {
            if (!messageLogged) {
                logger.debug "slowing down due to RATE_LIMIT=${rateLimit}..." //SKTOOLS-59
                messageLogged = true;
            }

            Thread.sleep(10)
            INVOCATIONS.removeAll(INVOCATIONS.findAll {it < System.currentTimeMillis() - 1000}) //removing previous invocations
        }


        INVOCATIONS.add(System.currentTimeMillis());

    }

}

public class ExecSpecs {
    ExecSpecs() { }

    String username
    String password

    String url
    String driver

    //SKTOOLS-27
    boolean failOnError
}
