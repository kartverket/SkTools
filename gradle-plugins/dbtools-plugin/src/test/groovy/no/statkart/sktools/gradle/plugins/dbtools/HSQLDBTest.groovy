package no.statkart.sktools.gradle.plugins.dbtools

import org.testng.annotations.BeforeTest
import java.sql.Connection
import java.sql.DriverManager
import groovy.sql.Sql
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeMethod
import org.testng.annotations.AfterMethod
import no.statkart.sktools.utils.databasepatcher.testutils.DatabasePatcherTestContext
import org.slf4j.LoggerFactory
import org.slf4j.Logger

/**
 * Testklasse som setter opp en tom database.
 *
 * Hver test får definert opp
 * <ul>
 *     <li> system-bruker - {@link #systemCredentials}
 *     <li> standard-bruker - {@link #defaultCredentials}
 * </ul>
 *
 * Ytterlige brukere defineres via {@link #defineDatabaseUser}
 *
 */
abstract class HSQLDBTest {
    final Logger logger = LoggerFactory.getLogger(this.class)

    private Connection connection
    protected final LinkedHashMap<Credentials, Sql> databaseUsers = new LinkedHashMap<Credentials, Sql>(2)

    protected final jdbcDriverClassString = 'org.hsqldb.jdbcDriver'

    protected String getUrl() {
        return "jdbc:hsqldb:mem:${this.class.getSimpleName()}"
    }

    protected final static HSQLDBTest.Credentials systemCredentials = new Credentials('SA', '', null)
    public HSQLDBTest.Credentials defaultCredentials
    int brukerIdx = 1;


    /** immutable */
    static final class Credentials {
        final String username, password, defaultSchema

        Credentials(username, password, schema) {
            this.username = username
            this.password = password
            this.defaultSchema = schema
        }

        @Override
        int hashCode() {
            return username.hashCode()
        }

        @Override
        boolean equals(Object obj) {
            if (obj instanceof Credentials) {
              username.equals(obj.username) && password.equals(obj.password)
            }
            false
        }

        @Override
        String toString() {
            return "${username}/${password}"
        }
    }


    /**
     * Setter opp databasePatcher med angitte credentials
     */
    protected DatabasePatcherTestContext buildDatabasePatcherTestFixture(Credentials credentials = defaultCredentials, Credentials schemaCredentials = null) {
        Sql sql = getSql(credentials)
        String schema = (credentials.equals(schemaCredentials)) ? null : (schemaCredentials != null) ? schemaCredentials.defaultSchema : credentials.defaultSchema
        def testContext = new DatabasePatcherTestContext(jdbcDriverClassString, getUrl(), credentials.username, credentials.password, schema)

        return testContext
    }

    /**
     * Connection som holder databasen oppe. Kallet gjør at en in memory database blir kreert og vil være tilgjengelig så lenge som denne connectionen lever.
     */
    @BeforeTest
    void setupDb() {
        try {
            Class.forName(jdbcDriverClassString);
        } catch (Exception e) {
            System.out.println("ERROR: failed to load HSQLDB JDBC driver.");
            e.printStackTrace();
            throw new RuntimeException("Failed to load JDBC driver", e);
        }

        connection = DriverManager.getConnection(url, systemCredentials.username, systemCredentials.password)
    }

    @AfterTest
    void teardownDb() {
        if (!connection.closed) connection.close()
    }


    @BeforeMethod
    void setupDatabaseUsers() {
        databaseUsers.clear();
        addDatabaseUser(url, systemCredentials);
        defaultCredentials = defineDatabaseUser("BRUKER${brukerIdx}", '', "SKJEMA${brukerIdx}")
    }

    @AfterMethod
    void cleanupDatabaseUsers() {
        databaseUsers.reverseEach { def credentials, Sql sql ->
            if (credentials != systemCredentials) {
                [
                        "DROP SCHEMA \"${credentials.defaultSchema}\" CASCADE",
                ].each { String sqlString ->
                    logger.debug('SQL: {}', sqlString)
                    getSql(systemCredentials).execute(sqlString);
                }
            }

            sql.close();
        }
    }


    public Sql getSql(Credentials credentials = defaultCredentials) {
        databaseUsers.get(credentials)
    }

    public HSQLDBTest.Credentials defineDatabaseUser(String username, String password, String schema = username) {
        Credentials credentials = new Credentials(username, password, schema)

        setUpDatabaseUser(credentials);
        addDatabaseUser(getUrl(), credentials)
        brukerIdx++

        credentials
    }

    private void setUpDatabaseUser(Credentials credentials) {
        [
                "CREATE USER \"${credentials.username}\" PASSWORD \"${credentials.password}\"",
                "CREATE SCHEMA \"${credentials.defaultSchema}\" AUTHORIZATION \"${credentials.username}\"",
                "ALTER USER \"${credentials.username}\" SET INITIAL SCHEMA \"${credentials.defaultSchema}\"",
        ].each { String sqlString ->
            logger.debug('SQL: {}', sqlString)
            getSql(systemCredentials).execute(sqlString);
        }
    }

    private Sql addDatabaseUser(String url, Credentials credentials) {
        Sql newInstance = Sql.newInstance(url, credentials.username, credentials.password, jdbcDriverClassString)
        databaseUsers.put(credentials, newInstance)
        return newInstance
    }


}
