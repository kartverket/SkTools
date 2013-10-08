package no.statkart.sktools.gradle.plugins.dbtools

import org.testng.annotations.Test

import org.testng.Assert
import java.sql.SQLSyntaxErrorException
import org.gradle.api.Task
import org.gradle.api.tasks.TaskExecutionException

/**
 * Tester funksjonell plugin funjsonalitet via HSQLDB - en in memory database
 *
 * Testene sjekker her at faktiske sql-setninger blir kjørt mot databasen som forventet.
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
        defineDatabaseUser("USER1", "");

        assert sql.connection.isValid(0)

        final def testCase = new DbToolsPluginPatchTestCase()

        // STEG 1 - oppretter sql-filer relativt til prosjekt

        File createShemaFile = testCase.createNewFileWithDirsRelativeToProject('src/hsql/CreateSchema.sql', """\
            CREATE TABLE TEST_TABLE (
               ID INTEGER NOT NULL,
               NAVN VARCHAR(32) NOT NULL,
               PRIMARY KEY (ID)
            );
            """
        )
        File dataFile = testCase.createNewFileWithDirsRelativeToProject('src/hsql/data.sql', """\
            INSERT INTO TEST_TABLE (ID, NAVN) VALUES (1, 'CHUCK NORRIS');
            """
        )


        // STEG 2 - konfigurering av plugin
        testCase.configureDatabasePlugin {
            toolset( name:'Prefix', type:'hsqldb', prefix:'Prefix') {

                credentials.username = username
                credentials.password = password

                url = sql.connection.properties.URL
                driver = jdbcDriverClassString

                sqlTask( 'CreateSchema', sqlFile:'src/hsql/CreateSchema.sql')
                sqlTask( 'data', sqlFile:'src/hsql/data.sql')
            }
        }


        // STEG 3 - tasks ihht konvensjon
        Task createSchemaTask = testCase.project.tasks.getByName('prefixCreateSchema')
        Task dataTask = testCase.project.tasks.getByName('prefixData')

        Assert.assertNotNull createSchemaTask, "Forventet task for ${createShemaFile.name}"
        Assert.assertNotNull dataTask, "Forventet task for ${dataFile.name}"


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
            Assert.assertNotNull row, 'Forventer en rad'
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
     *     prefix_CreateSchema
     *     prefix_CreateSchema2
     *
     * STEG 4: Tester kjøring av prefix_CreateSchema for bruker 1
     * STEG 5: Tester kjøring av prefix_CreateSchema2 for bruker 2 som ikke finnes, forventer da en feil.
     *
     */
    @Test
    void testDynamicCredentials() {
        assert sql.connection.isValid(0)

        final def testCase = new DbToolsPluginPatchTestCase()

        // STEG 1 - oppretter sql-filer relativt til prosjekt

        File createShemaFile = testCase.createNewFileWithDirsRelativeToProject('src/hsql/CreateSchema.sql', """\
            CREATE TABLE TEST_TABLE (
               ID INTEGER NOT NULL,
               NAVN VARCHAR(32) NOT NULL,
               PRIMARY KEY (ID)
            );
            """
        )

        File createShema2File = testCase.createNewFileWithDirsRelativeToProject('src/hsql/CreateSchema2.sql', """
                CREATE TABLE TEST_TABLE2 (
                   ID INTEGER NOT NULL,
                   NAVN VARCHAR(32) NOT NULL,
                   PRIMARY KEY (ID)
                );
            """
        )


        testCase.configureDatabasePlugin {
            useToolset('hsqldb', 'Prefix_', 'hsql') {

                url = sql.connection.properties.URL
                driver = jdbcDriverClassString

                credentials.username = 'sa'
                credentials.password = ''
            }
        }


        // STEG 3 - credentials ihht konfig
        def credentials = testCase.convention.dbToolSets['Prefix_'].credentials
        Assert.assertEquals credentials.username, 'sa'
        Assert.assertEquals credentials.password, ''

        Task createSchemaTask = testCase.project.tasks.getByName('prefix_CreateSchema')
        Assert.assertNotNull createSchemaTask, "Forventet task for ${createShemaFile.name}"

        Task createSchema2Task = testCase.project.tasks.getByName('prefix_CreateSchema2')
        Assert.assertNotNull createSchema2Task, "Forventet task for ${createShema2File.name}"



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
            assert false
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
     *     dB1CreateSchema
     *     dB2CreateSchema2
     *
     * STEG 4: Tester kjøring av dB1CreateSchema
     * STEG 5: Tester kjøring av dB2CreateSchema
     *
     */
    @Test
    void testMultipleDatabases() {

        Credentials user1 = defaultCredentials
        Credentials user2 = defineDatabaseUser('USER2', '')

        Assert.assertTrue getSql(user1).connection.isValid(0)
        Assert.assertTrue getSql(user2).connection.isValid(0)


        final def testCase = new DbToolsPluginPatchTestCase()


        // STEG 1 - oppretter sql-filer relativt til prosjekt

        File createShemaFile = testCase.createNewFileWithDirsRelativeToProject('src/hsql/CreateSchema.sql', """\
            CREATE TABLE TEST_TABLE (
               ID INTEGER NOT NULL,
               NAVN VARCHAR(32) NOT NULL,
               PRIMARY KEY (ID)
            );
            """
        )
        File createShema2File = testCase.createNewFileWithDirsRelativeToProject('src/hsql2/CreateSchema2.sql', """\
            CREATE TABLE TEST_TABLE2 (
               ID INTEGER NOT NULL,
               NAVN VARCHAR(32) NOT NULL,
               PRIMARY KEY (ID)
            );
            """
        )



        // STEG 2 - konfigurering av plugin
        testCase.configureDatabasePlugin {
            useToolset('hsqldb', 'DB1', 'hsql') {

                url = "${getSql(user1).connection.properties.URL}"
                driver = jdbcDriverClassString

                credentials.username = user1.username
                credentials.password = user1.password
            }
            useToolset('hsqldb', 'DB2', 'hsql2') {

                url = "${getSql(user2).connection.properties.URL}"
                driver = jdbcDriverClassString

                credentials.username = user2.username
                credentials.password = user2.password
            }
        }


        // STEG 3 - credentials ihht konfig
        Task createSchemaTask = testCase.project.tasks.getByName('dB1CreateSchema')
        Assert.assertNotNull createSchemaTask, "Forventet task for ${createShemaFile.name}"

        Task createSchema2Task = testCase.project.tasks.getByName('dB2CreateSchema2')
        Assert.assertNotNull createSchema2Task, "Forventet task for ${createShema2File.name}"



        // STEG 4 - kjøring av tasks for DB1

        createSchemaTask.execute()

        try {
            def row = getSql(user1).firstRow('select * from TEST_TABLE')
            Assert.assertNull row, 'Forventer ingen rader'
        } catch (SQLSyntaxErrorException sqlsee) {
            Assert.fail 'Forventer at tabell finnes'
        }


        // STEG 5 - kjøring av tasks for DB2

        createSchema2Task.execute()

        try {
            def row = getSql(user2).firstRow('select * from TEST_TABLE2')
            Assert.assertNull row, 'Forventer ingen rader'
        } catch (SQLSyntaxErrorException sqlsee) {
            Assert.fail 'Forventer at tabell finnes'
        }

    }


    /**
     * Tester at standard test blir lagt til for en "patch"
     */
    @Test
    void testPatchStandardTasks() {
        final def testCase = new DbToolsPluginPatchTestCase()

        testCase.configureDatabasePlugin {
            toolset(name: 'Prefix', type: 'hsqldb', prefix: 'Prefix') {

                credentials.username = username
                credentials.password = password


                url = sql.connection.properties.URL
                driver = jdbcDriverClassString

                patch {
                    //tom konfigurasjon
                }

            }
        }

        // STEG 3 - tester
        Task printPatchVersionTask = testCase.project.tasks.getByName('prefixPrintPatchVersion')
        Task setIndexInSyncWithPatchTask = testCase.project.tasks.getByName('prefixSetIndexInSyncWithPatch')

        Assert.assertNotNull(printPatchVersionTask, "Forventet task for 'prefixPrintPatchVersion")
        Assert.assertNotNull(setIndexInSyncWithPatchTask, "Forventet task for 'prefixSetIndexInSyncWithPatch")

    }

    /**
     * Tester patchDatabase target
     */
    @Test
    void testPatchDatabase() {
        final def testCase = new DbToolsPluginPatchTestCase()

        // STEG 1 - setter opp testmaterie
        File patchFile = testCase.createSimplePatchFile()

        // STEG 2 - konfigurering av plugin
        testCase.configureDatabasePlugin {
            toolset(name: 'Prefix', type: 'hsqldb', prefix: 'Prefix') {

                credentials.username = username
                credentials.password = password

                url = sql.connection.properties.URL
                driver = jdbcDriverClassString

                patch {
                    patchTask('TestSchema', sqlFile:patchFile)
                }

            }
        }

        // STEG 3 - tester
        Task printPatchVersionTask = testCase.project.tasks.getByName('prefixPatchTestSchema')

//
//        if (IntelliJTestUtil.isIntelliJTestRuntime) {
//            printPatchVersionTask.classpath = project.files(this.class.classLoader.properties['URLs'])
//        }

        printPatchVersionTask.execute()

    }


}
