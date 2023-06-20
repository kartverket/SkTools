package no.statkart.sktools.gradle.plugins.dbtools

import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsPlugin
import no.statkart.sktools.gradle.plugins.dbtools.database.util.SQLTask
import org.assertj.core.api.Assertions
import org.assertj.core.api.ThrowableAssert
import org.assertj.core.util.Preconditions
import org.gradle.api.Project
import org.gradle.api.Task
import org.testng.Assert
import org.testng.annotations.Test

import java.sql.SQLInvalidAuthorizationSpecException
import java.sql.SQLSyntaxErrorException

import static no.statkart.sktools.gradle.plugins.dbtools.testutils.PatchTestutil.createSimplePatchFile

/**
 * Tester funksjonell plugin funjsonalitet via HSQLDB - en in memory database
 *
 * Testene sjekker her at faktiske sql-setninger blir kjørt mot databasen som forventet.
 */
class DbToolsPluginHSQLDBTest extends HSQLDBTest {
    public static final Map<Object, Object> testProperties;
    static {
        Properties properties = new Properties();
        try {
            properties.load(DbtoolsPlugin.class.getResourceAsStream("/DbtoolsPluginTest.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        testProperties = Collections.unmodifiableMap(properties);
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
        Preconditions.checkState(sql.connection.isValid(0), "Invalid connection - see %s", 'getSql()')

        // STEG 1 - oppretter sql-filer relativt til prosjekt

        final File createShemaFile = writeFileUTF8('src/hsql/CreateSchema.sql', """\
            CREATE TABLE TEST_TABLE (
               ID INTEGER NOT NULL,
               NAVN VARCHAR(32) NOT NULL,
               PRIMARY KEY (ID)
            );
            """
        )

        final File createShema2File = writeFileUTF8('src/hsql/CreateSchema2.sql', """\
                CREATE TABLE TEST_TABLE2 (
                   ID INTEGER NOT NULL,
                   NAVN VARCHAR(32) NOT NULL,
                   PRIMARY KEY (ID)
                );
            """
        )


        final Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-dbtools-plugin'

             configureDatabasePlugin {
                toolset( type:'hsqldb', prefix:'Prefix_', name:'hsql' ) {
                    sqlTask('CreateSchema', sqlFile: createShemaFile)
                    sqlTask('CreateSchema2', sqlFile: createShema2File) {
                        username = 'foo'
                        password = 'bar'
                    }

                    url = sql.connection.properties.URL
                    driver = jdbcDriverClassString

                    credentials.username = defaultCredentials.username
                    credentials.password = defaultCredentials.password
                }
            }

        }


        final DbtoolsConvention convention = project.convention.plugins.db

        // STEG 3 - credentials ihht konfig
        Assert.assertEquals convention.dbToolSets['hsql'].credentials.username, defaultCredentials.username
        Assert.assertEquals convention.dbToolSets['hsql'].credentials.password, defaultCredentials.password

        SQLTask createSchemaTask = project.tasks.getByName('prefix_CreateSchema') as SQLTask
        Assert.assertNotNull createSchemaTask, "Forventet task for ${createShemaFile.name}"

        SQLTask createSchema2Task = project.tasks.getByName('prefix_CreateSchema2') as SQLTask
        Assert.assertNotNull createSchema2Task, "Forventet task for ${createShema2File.name}"



        // STEG 4 - kjøring av tasks for bruker 1

        createSchemaTask.exec()

        try {
            def row = sql.firstRow('select * from TEST_TABLE')
            Assert.assertNull row, 'Forventer ingen rader'
        } catch (SQLSyntaxErrorException ignored) {
            Assert.fail 'Forventer at tabell finnes'
        }

        // STEG 5 - kjøring av tasks for bruker 2

        Assertions.assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            void call() throws Throwable {
                createSchema2Task.exec()
            }
        })
            .hasRootCauseMessage("invalid authorization specification - not found: foo")
            .hasCauseInstanceOf(SQLInvalidAuthorizationSpecException.class)
    }

    @Test
    void connectingUsingUnknownUserGivesInformativeMessage() {
        Preconditions.checkState(sql.connection.isValid(0), "Invalid connection - see %s", 'getSql()')

        final Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-dbtools-plugin'

            configureDatabasePlugin {
                toolset( type:'hsqldb', prefix:'Prefix_', name:'hsql' ) {
                    sqlTask('CreateSchema', sqlString: 'ignored')

                    url = sql.connection.properties.URL
                    driver = jdbcDriverClassString

                    credentials.username = 'foo'
                    credentials.password = 'bar'
                }
            }
        }

        SQLTask createSchemaTask = project.tasks.getByName('prefix_CreateSchema') as SQLTask

        Assertions.assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            void call() throws Throwable {
                createSchemaTask.exec()
            }
        })
        .describedAs("SKTOOLS-204: Informativ feilmelding")
            .hasMessage("ERROR connecting to database jdbc:hsqldb:mem:DbToolsPluginHSQLDBTest [foo/b*r]")
            .hasCauseInstanceOf(SQLInvalidAuthorizationSpecException.class)
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

        Preconditions.checkState(getSql(user1).connection.isValid(0), "Invalid connection - see %s", "getSql($user1)")
        Preconditions.checkState(getSql(user2).connection.isValid(0), "Invalid connection - see %s", "getSql($user2)")


        // STEG 1 - oppretter sql-filer relativt til prosjekt
        File createShemaFile = writeFileUTF8('src/hsql/CreateSchema.sql', """\
            CREATE TABLE TEST_TABLE (
               ID INTEGER NOT NULL,
               NAVN VARCHAR(32) NOT NULL,
               PRIMARY KEY (ID)
            );
            """
        )
        File createShema2File = writeFileUTF8('src/hsql2/CreateSchema2.sql', """\
            CREATE TABLE TEST_TABLE2 (
               ID INTEGER NOT NULL,
               NAVN VARCHAR(32) NOT NULL,
               PRIMARY KEY (ID)
            );
            """
        )



        // STEG 2 - konfigurering av plugin
        final Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-dbtools-plugin'

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
        SQLTask createSchemaTask = project.tasks.getByName('dB1CreateSchema') as SQLTask
        Assert.assertNotNull createSchemaTask, "Forventet task for ${createShemaFile.name}"

        SQLTask createSchema2Task = project.tasks.getByName('dB2CreateSchema2') as SQLTask
        Assert.assertNotNull createSchema2Task, "Forventet task for ${createShema2File.name}"



        // STEG 4 - kjøring av tasks for DB1

        createSchemaTask.exec()

        try {
            def row = getSql(user1).firstRow('select * from TEST_TABLE')
            Assert.assertNull row, 'Forventer ingen rader'
        } catch (SQLSyntaxErrorException ignored) {
            Assert.fail 'Forventer at tabell finnes'
        }


        // STEG 5 - kjøring av tasks for DB2

        createSchema2Task.exec()

        try {
            def row = getSql(user2).firstRow('select * from TEST_TABLE2')
            Assert.assertNull row, 'Forventer ingen rader'
        } catch (SQLSyntaxErrorException ignored) {
            Assert.fail 'Forventer at tabell finnes'
        }

    }


    /**
     * Tester at standard test blir lagt til for en "patch"
     */
    @Test
    void testPatchStandardTaskPrintPatchVersion() {
        final Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-dbtools-plugin'

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
        Task printPatchVersionTask = project.tasks.getByName('prefixPrintPatchVersion')
        Assert.assertNotNull(printPatchVersionTask, "Forventet task for 'prefixPrintPatchVersion")
    }

    /**
     * Tester at standard test blir lagt til for en "patch"
     */
    @Test
    void testPatchStandardTaskSetIndexInSyncWithPatch() {
        final Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-dbtools-plugin'

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
        Task setIndexInSyncWithPatchTask = project.tasks.getByName('prefixSetIndexInSyncWithPatch')
        Assert.assertNotNull(setIndexInSyncWithPatchTask, "Forventet task for 'prefixSetIndexInSyncWithPatch")
    }

    /**
     * Tester at standard test blir lagt til for en "patch"
     */
    @Test
    void testPatchStandardTaskUnSetIndexInSyncWithPatch() {
        final Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-dbtools-plugin'

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
        Task unsetIndexInSyncWithPatchTask = project.tasks.getByName('prefixUnSetIndexInSyncWithPatch')
        Assert.assertNotNull(unsetIndexInSyncWithPatchTask, "Forventet task for 'prefixUnSetIndexInSyncWithPatch")
    }

    /**
     * Tester patchDatabase target
     */
    @Test
    void testPatchDatabase() {
        // STEG 1 - setter opp testmaterie
        createSimplePatchFile(file('patch.sql'))

        // STEG 2 - konfigurering av plugin
        writeFileUTF8("build.gradle", """\
            plugins {
              id 'sktools.dbtools'
            }

            repositories {
                mavenCentral()
            }

            configureDatabasePlugin {
                useDrivers "${testProperties.libraries_hsqldb}"

                toolset(name: 'Prefix', type: 'hsqldb', prefix: 'Prefix') {

                    credentials.username = "${defaultCredentials.username}"
                    credentials.password = "${defaultCredentials.password}"

                    url = "${sql.connection.properties.URL}"
                    driver = "${jdbcDriverClassString}"

                    patch {
                        patchTask('TestSchema', sqlFile: 'patch.sql')
                    }

                }
            }
        """)

        // STEG 3 - tester
        assertNoFailures(testGradleBuild(":prefixPatchTestSchema"))
    }



    @Test
    void tasknameForPatch() {
        final Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-dbtools-plugin'

            configureDatabasePlugin {
                toolset(name: 'Prefix', type: 'hsqldb', prefix: 'Prefix') {

                    credentials.username = defaultCredentials.username
                    credentials.password = defaultCredentials.password

                    url = sql.connection.properties.URL
                    driver = jdbcDriverClassString

                    patch {
                        patchTask('TestSchema', sqlFile:file("."))
                    }
                }
            }
        }
        Assert.assertNotNull(project.tasks.findByName("prefixPatchTestSchema"), "Forventer task med navn")
    }

    @Test
    void tasknameForNull() {
        final Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-dbtools-plugin'

            configureDatabasePlugin {
                toolset(name: 'main', type: 'hsqldb', prefix: '') {

                    credentials.username = defaultCredentials.username
                    credentials.password = defaultCredentials.password

                    url = sql.connection.properties.URL
                    driver = jdbcDriverClassString

                    sqlTask('TestSchema', sqlFile:file("."))
                }
            }
        }
        Assert.assertNotNull(project.tasks.findByName("testSchema"), "Forventer task med navn")
    }

}
