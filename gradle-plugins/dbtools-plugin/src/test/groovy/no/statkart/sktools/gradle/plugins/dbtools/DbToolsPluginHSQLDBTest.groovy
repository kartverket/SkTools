package no.statkart.sktools.gradle.plugins.dbtools

import org.testng.annotations.Test

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import org.testng.Assert
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import java.sql.SQLSyntaxErrorException
import org.gradle.api.Task
import java.sql.SQLInvalidAuthorizationSpecException
import java.sql.SQLException
import org.gradle.api.tasks.TaskExecutionException
import groovy.sql.Sql

/**
 * Tester plugin funjsonalitet via HSQLDB - en in memory database
 */
class DbToolsPluginHSQLDBTest extends HSQLDBTest {


    /**
     * Tester at custom tasks blir lagt til i hht til konvensjon.
     *
     * STEG 1: Testen oppretter to filer relativt til prosjektet:
     *     src/hsqldb/CreateSchema.sql
     *     src/hsqldb/data.sql
     *
     * STEG 2: Konfigurering av plugin
     *
     * STEG 3: Deretter sjekkes det at to tasks er lagt til ihht til konvensjon:
     *     Prefix_CreateSchema
     *     Prefix_data
     *
     * STEG 4: Til sist blir taskene kalt og man kontrollerer databasen etter forventede endringer underveis.
     *
     *
     */
    @Test
    void testCustomTasksByConvention() {
        assert sql.connection.isValid(0)

        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()


        // STEG 1 - oppretter sql-filer relativt til prosjekt
        File dir = new File('hsql', new File('src', project.rootDir))
        assert dir.mkdirs()

        File createShemaFile = new File('CreateSchema.sql', dir)
        File dataFile = new File('data.sql', dir)

        createShemaFile.withWriter { writer ->
            writer << """\
                CREATE TABLE TEST_TABLE (
                   ID INTEGER NOT NULL,
                   NAVN VARCHAR(32) NOT NULL,
                   PRIMARY KEY (ID)
                );
            """
        }

        dataFile.withWriter { writer ->
            writer << """\
            INSERT INTO TEST_TABLE (ID, NAVN) VALUES (1, 'CHUCK NORRIS');
            """
        }


        // STEG 2 - konfigurering av plugin
        project.setProperty 'username', username
        project.setProperty 'password', password

        project.apply plugin:'sktools-dbtools-plugin'


        DbtoolsConvention convention = project.convention.getPlugin(DbtoolsConvention.class)
        convention.configureDatabasePlugin {
            useToolset('hsqldb', 'Prefix_', 'hsql') {

                url = sql.connection.properties.URL
                driver = jdbcDriverClassString
            }
        }


        // STEG 3 - tasksk ihht konvensjon
        Task createSchemaTask = project.tasks.getByName('Prefix_CreateSchema')
        Task dataTask = project.tasks.getByName('Prefix_data')

        Assert.assertNotNull createSchemaTask, "Forventet task for ${createShemaFile.name}"
        Assert.assertNotNull dataTask, "Forventet task for ${dataFile.name}"

        //todo: kjører denne inntill en finner ut av hvordan en kan starte taks med depends on og full pakke
        project.tasks.getByName('buildSQL').execute()

        // STEG 4 - kjøring av tasks

        try {
            sql.firstRow('select * from TEST_TABLE')
            Assert.fail 'Forventer tom base'
        } catch (SQLSyntaxErrorException sqlsee) {
            assert true
        }

        createSchemaTask.execute()

        try {
            def row = sql.firstRow('select * from TEST_TABLE')
            Assert.assertNull row, 'Forventer ingen rader'
        } catch (SQLSyntaxErrorException sqlsee) {
            Assert.fail 'Forventer at tabell finnes'
        }


        dataTask.execute()

        try {
            def row = sql.firstRow('select ID, NAVN from TEST_TABLE where ID = 1')
            Assert.assertNotNull row, 'Forventer en rader'
            Assert.assertEquals row.ID, 1, 'forventet verdi'
            Assert.assertEquals row.NAVN, 'CHUCK NORRIS', 'forventet verdi'
        } catch (SQLSyntaxErrorException sqlsee) {
            Assert.fail 'Forventer at tabell finnes'
        }

    }



    /**
     * Tester angivelse av brukernavn og passord mot basen
     *
     * STEG 1: Testen oppretter to filer relativt til prosjektet:
     *     src/hsqldb/CreateSchema.sql
     *     src/hsqldb/CreateSchema2.sql
     *
     * STEG 2: Konfigurering av plugin
     *
     * STEG 3: Deretter sjekkes det at credentials er satt og at tasks er lagt til ihht til konvensjon:
     *     Prefix_CreateSchema
     *     Prefix_CreateSchema2
     *
     * STEG 4: Tester kjøring av Prefix_CreateSchema for bruker 1
     * STEG 5: Tester kjøring av Prefix_CreateSchema2 for bruker 2 som ikke finnes, forventer da en feil.
     *
     */
    @Test
    void testDynamicCredentials() {
        assert sql.connection.isValid(0)

        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()


        // STEG 1 - oppretter sql-filer relativt til prosjekt
        File dir = new File('hsql', new File('src', project.rootDir))
        assert dir.mkdirs()

        File createShemaFile = new File('CreateSchema.sql', dir)
        File createShema2File = new File('CreateSchema2.sql', dir)

        createShemaFile.withWriter { writer ->
            writer << """\
                CREATE TABLE TEST_TABLE (
                   ID INTEGER NOT NULL,
                   NAVN VARCHAR(32) NOT NULL,
                   PRIMARY KEY (ID)
                );
            """
        }

        createShema2File.withWriter { writer ->
            writer << """\
                CREATE TABLE TEST_TABLE2 (
                   ID INTEGER NOT NULL,
                   NAVN VARCHAR(32) NOT NULL,
                   PRIMARY KEY (ID)
                );
            """
        }


        // STEG 2 - konfigurering av plugin
        project.apply plugin:'sktools-dbtools-plugin'


        DbtoolsConvention convention = project.convention.getPlugin(DbtoolsConvention.class)
        convention.configureDatabasePlugin {
            useToolset('hsqldb', 'Prefix_', 'hsql') {

                url = sql.connection.properties.URL
                driver = jdbcDriverClassString

                credentials.username = 'sa'
                credentials.password = ''
            }
        }


        // STEG 3 - credentials ihht konfig
        def credentials = convention.environments['Prefix_'].credentials
        Assert.assertEquals credentials.username, 'sa'
        Assert.assertEquals credentials.password, ''

        Task createSchemaTask = project.tasks.getByName('Prefix_CreateSchema')
        Assert.assertNotNull createSchemaTask, "Forventet task for ${createShemaFile.name}"

        Task createSchema2Task = project.tasks.getByName('Prefix_CreateSchema2')
        Assert.assertNotNull createSchema2Task, "Forventet task for ${createShema2File.name}"


        //todo: kjører denne inntill en finner ut av hvordan en kan starte taks med depends on og full pakke
        project.convention.plugins.db.buildSQLTask.execute()


        // STEG 4 - kjøring av tasks for bruker 1

        createSchemaTask.execute()

        try {
            def row = sql.firstRow('select * from TEST_TABLE')
            Assert.assertNull row, 'Forventer ingen rader'
        } catch (SQLSyntaxErrorException sqlsee) {
            Assert.fail 'Forventer at tabell finnes'
        }

        // STEG 5 - kjøring av tasks for bruker 2

        credentials.username = 'sa2'
        credentials.username = ''

        try {
            createSchema2Task.execute()
        } catch (TaskExecutionException tee) {
            def cause = tee.cause
            assert cause.message.contains('authorization')
            assert cause instanceof java.sql.SQLInvalidAuthorizationSpecException
        }

    }



    /**
     * Tester plugin mot flere database-oppsett mot forskjellige databaser
     *
     * STEG 1: Testen oppretter to filer relativt til prosjektet:
     *     src/hsqldb/CreateSchema.sql
     *     src/hsqldb2/CreateSchema2.sql
     *
     * STEG 2: Konfigurering av plugin - Det blir konfigurert opp to toolsett
     *      DB1
     *      DB2
     *
     * STEG 3: Deretter sjekkes det at credentials er satt og at tasks er lagt til ihht til konvensjon:
     *     DB1_CreateSchema
     *     DB2_CreateSchema2
     *
     * STEG 4: Tester kjøring av DB1CreateSchema
     * STEG 5: Tester kjøring av DB2CreateSchema
     *
     */
    @Test
    void testMultipleDatabases() {
        assert sql.connection.isValid(0)

        String db1URL = sql.connection.properties.URL
        String db2URL = "${db1URL}DB2"

        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()


        // STEG 1 - oppretter sql-filer relativt til prosjekt
        File dir1 = new File('hsql', new File('src', project.rootDir))
        File dir2 = new File('hsql2', new File('src', project.rootDir))
        assert dir1.mkdirs()
        assert dir2.mkdirs()

        File createShemaFile = new File('CreateSchema.sql', dir1)
        File createShema2File = new File('CreateSchema2.sql', dir2)

        createShemaFile.withWriter { writer ->
            writer << """\
                CREATE TABLE TEST_TABLE (
                   ID INTEGER NOT NULL,
                   NAVN VARCHAR(32) NOT NULL,
                   PRIMARY KEY (ID)
                );
            """
        }

        createShema2File.withWriter { writer ->
            writer << """\
                CREATE TABLE TEST_TABLE2 (
                   ID INTEGER NOT NULL,
                   NAVN VARCHAR(32) NOT NULL,
                   PRIMARY KEY (ID)
                );
            """
        }


        // STEG 2 - konfigurering av plugin
        project.apply plugin:'sktools-dbtools-plugin'


        DbtoolsConvention convention = project.convention.getPlugin(DbtoolsConvention.class)
        convention.configureDatabasePlugin {
            useToolset('hsqldb', 'DB1', 'hsql') {

                url = "${db1URL}"
                driver = jdbcDriverClassString

                credentials.username = 'sa'
                credentials.password = ''
            }
            useToolset('hsqldb', 'DB2', 'hsql2') {

                url = "${db2URL}"
                driver = jdbcDriverClassString

                credentials.username = 'sa'
                credentials.password = ''
            }
        }


        // STEG 3 - credentials ihht konfig
        Task createSchemaTask = project.tasks.getByName('DB1CreateSchema')
        Assert.assertNotNull createSchemaTask, "Forventet task for ${createShemaFile.name}"

        Task createSchema2Task = project.tasks.getByName('DB2CreateSchema2')
        Assert.assertNotNull createSchema2Task, "Forventet task for ${createShema2File.name}"



        //todo: kjører denne inntill en finner ut av hvordan en kan starte taks med depends on og full pakke
        project.convention.plugins.db.buildSQLTask.execute()


        // STEG 4 - kjøring av tasks for DB1

        createSchemaTask.execute()

        try {
            def row = sql.firstRow('select * from TEST_TABLE')
            Assert.assertNull row, 'Forventer ingen rader'
        } catch (SQLSyntaxErrorException sqlsee) {
            Assert.fail 'Forventer at tabell finnes'
        }


        // STEG 5 - kjøring av tasks for DB2

        createSchema2Task.execute()

        try {
            def row = buildSQLInstance(db2URL, 'sa', '').firstRow('select * from TEST_TABLE2')
            Assert.assertNull row, 'Forventer ingen rader'
        } catch (SQLSyntaxErrorException sqlsee) {
            Assert.fail 'Forventer at tabell finnes'
        }



    }

}
