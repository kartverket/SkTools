package no.statkart.sktools.gradle.plugins.dbtools

import org.testng.annotations.BeforeTest
import java.sql.Connection
import java.sql.DriverManager
import groovy.sql.Sql
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeMethod
import org.testng.annotations.AfterMethod
import no.statkart.sktools.utils.databasepatcher.DatabasePatcherTestCase

/**
 * Testklasse som setter opp en tom database.
 *
 * Hver test får definert opp
 * <ul>
 *     <li> system-bruker - {@link #systemCredentials}
 *     <li> standard-bruker - {@link #defaultCredentials}
 * </ul>
 *
 * Ytterlige brukere defineres via {@link #defineDatabaseUser(String, String)}
 *
 */
abstract class HSQLDBTest {

    private Connection connection
    protected final LinkedHashMap<Credentials, Sql> databaseUsers = new LinkedHashMap<Credentials, Sql>(2)

    protected final jdbcDriverClassString = 'org.hsqldb.jdbcDriver'

    protected String getUrl(def SCHEMA_NAME = getSchemaName()) {
        return "jdbc:hsqldb:mem:${SCHEMA_NAME}"
    }

    private String getSchemaName(def suffix = '') {
        return "${this.class.simpleName}${suffix}"
    }

    protected final static HSQLDBTest.Credentials systemCredentials = new Credentials('sa', '')
    public HSQLDBTest.Credentials defaultCredentials
    int testIdx = 0;


    /** immutable */
    static final class Credentials {
        final String username, password

        Credentials(username, password) {
            this.username = username
            this.password = password
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


    protected DatabasePatcherTestCase buildDatabasePatcherTestCase() {
        return buildDatabasePatcherTestCase(defaultCredentials)
    }

    /**
     * Setter opp databasePatcher med angitte credentials
     */
    protected DatabasePatcherTestCase buildDatabasePatcherTestCase(Credentials credentials) {
        Sql sql = getSql(credentials)
        def testCase = new DatabasePatcherTestCase(jdbcDriverClassString, sql.connection.metaData.URL, credentials.username, credentials.password)

        return testCase
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
        testIdx++
        databaseUsers.clear();
        addDatabaseUser(url, systemCredentials);
        defaultCredentials = defineDatabaseUser("BRUKER${testIdx}", '')
    }

    @AfterMethod
    void cleanupDatabaseUsers() {
        databaseUsers.each { def key, Sql sql ->
            sql.close();
        }
    }


    public Sql getSql(Credentials credentials = defaultCredentials) {
        databaseUsers.get(credentials)
    }

    public HSQLDBTest.Credentials defineDatabaseUser(String username, String password) {
        Credentials credentials = new Credentials(username, password)
        testIdx++
        setUpDatabaseUser(credentials, getSchemaName(testIdx));
        addDatabaseUser(getUrl(getSchemaName(testIdx)), credentials)

        credentials
    }

    private void setUpDatabaseUser(Credentials credentials, def schemaName) {
        getSql(systemCredentials).execute("CREATE USER ${credentials.username} PASSWORD ${credentials.password}".toString());
        getSql(systemCredentials).execute("CREATE SCHEMA ${schemaName} AUTHORIZATION ${credentials.username}".toString());
        getSql(systemCredentials).execute("ALTER USER ${credentials.username} SET INITIAL SCHEMA ${schemaName}".toString());
    }

    private Sql addDatabaseUser(String url, Credentials credentials) {
        Sql newInstance = Sql.newInstance(url, credentials.username, credentials.password, jdbcDriverClassString)
        databaseUsers.put(credentials, newInstance)
        return newInstance
    }


}
