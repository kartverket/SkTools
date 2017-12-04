package no.statkart.sktools.gradle.plugins.dbtools

import com.google.common.base.Preconditions
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import no.statkart.sktools.gradle.plugins.dbtools.testutils.DbToolsPluginPatchHelper
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import org.gradle.api.Task
import org.gradle.api.tasks.TaskExecutionException
import org.testng.Assert
import org.testng.annotations.Test

import java.sql.SQLSyntaxErrorException

/**
 * Tester funksjonell plugin funjsonalitet via HSQLDB - en in memory database
 *
 * Testene sjekker her at faktiske sql-setninger blir kjørt mot databasen som forventet.
 */
class DbToolsPluginHSQLDBTest extends HSQLDBTest {



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
        Preconditions.checkState(sql.connection.isValid(0))

        final ProjectHelper testCase = GradleProjectBuilder.builder().build {
            apply plugin: 'sktools-dbtools-plugin'
        };

        // STEG 1 - oppretter sql-filer relativt til prosjekt

        final File createShemaFile = testCase.createNewFileWithDirsRelativeToProject('src/hsql/CreateSchema.sql', """\
            CREATE TABLE TEST_TABLE (
               ID INTEGER NOT NULL,
               NAVN VARCHAR(32) NOT NULL,
               PRIMARY KEY (ID)
            );
            """
        )

        final File createShema2File = testCase.createNewFileWithDirsRelativeToProject('src/hsql/CreateSchema2.sql', """
                CREATE TABLE TEST_TABLE2 (
                   ID INTEGER NOT NULL,
                   NAVN VARCHAR(32) NOT NULL,
                   PRIMARY KEY (ID)
                );
            """
        )


        testCase.configureProject {
            configureDatabasePlugin {
                toolset( type:'hsqldb', prefix:'Prefix_', name:'hsql' ) {
                    sqlTask('CreateSchema', sqlFile: createShemaFile)
                    sqlTask('CreateSchema2', sqlFile: createShema2File)

                    url = sql.connection.properties.URL
                    driver = jdbcDriverClassString

                    credentials.username = defaultCredentials.username
                    credentials.password = defaultCredentials.password
                }
            }

        }


        final DbtoolsConvention convention = testCase.project.convention.plugins.db

        // STEG 3 - credentials ihht konfig
        def credentials = convention.dbToolSets['hsql'].credentials
        Assert.assertEquals credentials.username, defaultCredentials.username
        Assert.assertEquals credentials.password, defaultCredentials.password

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
            Assert.fail 'forventer exception'
        } catch (TaskExecutionException tee) {
            def cause = tee.cause
            Assert.assertTrue cause.message.contains('authorization')
            Assert.assertTrue cause instanceof java.sql.SQLInvalidAuthorizationSpecException ||
                    cause.cause instanceof java.sql.SQLInvalidAuthorizationSpecException
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

        final def user1 = defaultCredentials
        final def user2 = defineDatabaseUser('USER2', '')

        Preconditions.checkState(getSql(user1).connection.isValid(0))
        Preconditions.checkState(getSql(user2).connection.isValid(0))

        final ProjectHelper testCase = GradleProjectBuilder.builder().build {
            apply plugin: 'sktools-dbtools-plugin'
        };


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
        testCase.configureProject {
            configureDatabasePlugin {
                toolset( type:'hsqldb', prefix:'DB1', name:'hsql' ) {
                    sqlTask('CreateSchema', sqlFile: createShemaFile)

                    url = "${getSql(user1).connection.properties.URL}"
                    driver = jdbcDriverClassString

                    credentials.username = user1.username
                    credentials.password = user1.password
                }
                toolset( type:'hsqldb', prefix:'DB2', name:'hsql' ) {
                    sqlTask('CreateSchema2', sqlFile: createShema2File)

                    url = "${getSql(user2).connection.properties.URL}"
                    driver = jdbcDriverClassString

                    credentials.username = user2.username
                    credentials.password = user2.password
                }
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
    void testPatchStandardTaskPrintPatchVersion() {
        final ProjectHelper testCase = GradleProjectBuilder.builder().build {
            apply plugin: 'sktools-dbtools-plugin'
        };

        testCase.configureProject {
            configureDatabasePlugin {
                toolset(name: 'Prefix', type: 'hsqldb', prefix: 'Prefix') {

                    credentials.username = defaultCredentials.username
                    credentials.password = defaultCredentials.password

                    url = sql.connection.properties.URL
                    driver = jdbcDriverClassString

                    patch {
                        //tom konfigurasjon
                    }
                }
            }
        }

        // STEG 3 - tester
        Task printPatchVersionTask = testCase.project.tasks.getByName('prefixPrintPatchVersion')
        Assert.assertNotNull(printPatchVersionTask, "Forventet task for 'prefixPrintPatchVersion")
    }

    /**
     * Tester at standard test blir lagt til for en "patch"
     */
    @Test
    void testPatchStandardTaskSetIndexInSyncWithPatch() {
        final ProjectHelper testCase = GradleProjectBuilder.builder().build {
            apply plugin: 'sktools-dbtools-plugin'
        };

        testCase.configureProject {
            configureDatabasePlugin {
                toolset(name: 'Prefix', type: 'hsqldb', prefix: 'Prefix') {

                    credentials.username = defaultCredentials.username
                    credentials.password = defaultCredentials.password

                    url = sql.connection.properties.URL
                    driver = jdbcDriverClassString

                    patch {
                        //tom konfigurasjon
                    }
                }
            }
        }

        // STEG 3 - tester
        Task setIndexInSyncWithPatchTask = testCase.project.tasks.getByName('prefixSetIndexInSyncWithPatch')
        Assert.assertNotNull(setIndexInSyncWithPatchTask, "Forventet task for 'prefixSetIndexInSyncWithPatch")
    }

    /**
     * Tester at standard test blir lagt til for en "patch"
     */
    @Test
    void testPatchStandardTaskUnSetIndexInSyncWithPatch() {
        final ProjectHelper testCase = GradleProjectBuilder.builder().build {
            apply plugin: 'sktools-dbtools-plugin'
        };

        testCase.configureProject {
            configureDatabasePlugin {
                toolset(name: 'Prefix', type: 'hsqldb', prefix: 'Prefix') {

                    credentials.username = defaultCredentials.username
                    credentials.password = defaultCredentials.password

                    url = sql.connection.properties.URL
                    driver = jdbcDriverClassString

                    patch {
                        //tom konfigurasjon
                    }
                }
            }
        }

        // STEG 3 - tester
        Task unsetIndexInSyncWithPatchTask = testCase.project.tasks.getByName('prefixUnSetIndexInSyncWithPatch')
        Assert.assertNotNull(unsetIndexInSyncWithPatchTask, "Forventet task for 'prefixUnSetIndexInSyncWithPatch")
    }

    /**
     * Tester patchDatabase target
     */
    @Test
    void testPatchDatabase() {
        final ProjectHelper testCase = GradleProjectBuilder.builder().build {
            apply plugin: 'sktools-dbtools-plugin'
        };

        // STEG 1 - setter opp testmaterie
        File patchFile = DbToolsPluginPatchHelper.createSimplePatchFile()

        // STEG 2 - konfigurering av plugin
        testCase.configureProject {
            configureDatabasePlugin {
                toolset(name: 'Prefix', type: 'hsqldb', prefix: 'Prefix') {

                    credentials.username = defaultCredentials.username
                    credentials.password = defaultCredentials.password

                    url = sql.connection.properties.URL
                    driver = jdbcDriverClassString

                    patch {
                        patchTask('TestSchema', sqlFile:patchFile)
                    }

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
