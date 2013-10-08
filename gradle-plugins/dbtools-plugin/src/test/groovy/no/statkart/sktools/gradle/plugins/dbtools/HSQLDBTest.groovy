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
 */
abstract class HSQLDBTest {

    private Connection connection
    protected final LinkedHashMap<Credentials, Sql> sqls = new LinkedHashMap<Credentials, Sql>(2)

    protected final jdbcDriverClassString = 'org.hsqldb.jdbcDriver'

    protected String getUrl(def SCHEMA_NAME = getSchemaName()) {
        return "jdbc:hsqldb:mem:${SCHEMA_NAME}"
    }

    private String getSchemaName(def suffix = '') {
        return "${this.class.simpleName}${suffix}"
    }

    protected final HSQLDBTest.Credentials defaultCredentials = new Credentials('sa', '')
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
        def testCase = new DatabasePatcherTestCase(jdbcDriverClassString, sql.connection.metaData.URL, defaultCredentials.username, defaultCredentials.password)

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

        connection = DriverManager.getConnection(url, defaultCredentials.username, defaultCredentials.password)
    }

    @AfterTest
    void teardownDb() {
        if (!connection.closed) connection.close()
    }


    @BeforeMethod
    void setupSql() {
        testIdx++
        sqls.clear();
        buildSQLInstance(url, defaultCredentials);
    }

    @AfterMethod
    void cleanupSql() {
        sqls.each { def key, Sql sql ->
            sql.close();
        }
    }


    public Sql getSql(Credentials credentials = defaultCredentials) {
        sqls.get(credentials)
    }

    public HSQLDBTest.Credentials defineDatabaseUser(String username, String password) {
        Credentials credentials = new Credentials(username, password)
        testIdx++
        addDatabaseUser(credentials, getSchemaName(testIdx));
        buildSQLInstance(getUrl(getSchemaName(testIdx)), credentials)

        credentials
    }

    private void addDatabaseUser(Credentials credentials, def schemaName) {
        sql.execute("CREATE USER ${credentials.username} PASSWORD ${credentials.password}".toString());
        sql.execute("CREATE SCHEMA ${schemaName} AUTHORIZATION ${credentials.username}".toString());
        sql.execute("ALTER USER ${credentials.username} SET INITIAL SCHEMA ${schemaName}".toString());
    }

    private Sql buildSQLInstance(String url, Credentials credentials) {
        Sql newInstance = Sql.newInstance(url, credentials.username, credentials.password, jdbcDriverClassString)
        sqls.put(credentials, newInstance)
        return newInstance
    }


}
