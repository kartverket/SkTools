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
    protected Sql sql

    protected final jdbcDriverClassString = 'org.hsqldb.jdbcDriver'
    private String url = "jdbc:hsqldb:mem:${this.class.simpleName}"
    protected final def username = 'sa'
    protected final def password = ''

    int testIdx = 0;


    protected DatabasePatcherTestCase buildDatabasePatcherTestCase() {
        def testCase = new DatabasePatcherTestCase(jdbcDriverClassString, sql.connection.metaData.URL, username, password)

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

        connection = DriverManager.getConnection(url, username, password)
    }

    @AfterTest
    void teardownDb() {
        if (!connection.closed) connection.close()
    }


    @BeforeMethod
    void setupSql() {
        testIdx++
        sql = buildSQLInstance(url + testIdx, username, password)
    }

    @AfterMethod
    void cleanupSql() {
        sql.close()
    }



    protected Sql buildSQLInstance(String url) {
        return buildSQLInstance(url, 'sa', '')
    }

    protected Sql buildSQLInstance(String url, String username, String password) {
        return Sql.newInstance(url, username, password, jdbcDriverClassString)
    }


}
