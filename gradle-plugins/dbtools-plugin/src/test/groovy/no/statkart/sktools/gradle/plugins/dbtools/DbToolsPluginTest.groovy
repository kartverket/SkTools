package no.statkart.sktools.gradle.plugins.dbtools

import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleExportTask
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleImportTask
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch.DefineLatestPatchVersionTask
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch.PatchTask
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch.SyncPatchTask
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.DbToolsProjectBuilder
import org.gradle.api.Task
import org.testng.Assert
import org.testng.annotations.Test

import static org.assertj.core.api.Assertions.assertThat

/**
 * Test av {@link no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsPlugin} m.t.p. gradle mekanikker.
 *
 * For funksjonell testing av eksekvering av sql script, se {@link DbToolsPluginHSQLDBTest}
 */
class DbToolsPluginTest {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testAppplyPlugin() {
        final ProjectHelper testCase = DbToolsProjectBuilder.builder().applyDbUtilsPlugin().build();

        Assert.assertTrue(testCase.project.convention.plugins.db instanceof DbtoolsConvention)
    }

    /**
     * Tester og demonstrerer angivelse av credentials.
     *
     * Testen illustrerer at credentials på toolset og tasker kan bli satt runtime.
     * En illustrerer også at disse kan løsrives dersom en ønsker, slik at en kan be brukeren om å tate inn credentials i spesielle tilfeller.
     */
    @Test
    void testApplyCredentials() {
        final ProjectHelper testCase = DbToolsProjectBuilder.builder().applyDbUtilsPlugin().build();
        testCase.createNewFileWithDirsRelativeToProject('src/hsql/PleaseAuthenticateMe.sql')

        testCase.configureProject {
            configureDatabasePlugin {
                toolset(name: 'coolDb', type: 'hsqldb', prefix: 'coolDb') {
                    url = "jdbc:hsqldb:mem:${this.class.simpleName}TestApplyCredentials"
                    driver = 'org.hsqldb.jdbcDriver'

                    sqlTask('PleaseAuthenticateMe', sqlFile: 'src/hsql/PleaseAuthenticateMe.sql')
                    properties = [
                            username: 'brukernavn',
                            password: 'passord',
                    ]
                }
            }
        }

        Assert.assertNotNull(testCase.project.tasks.findByName('coolDbPleaseAuthenticateMe'), "Forventet at task er lagt til")
        final DbtoolsConvention convention = testCase.project.convention.plugins.db

        //tester defaults - username og password blir lest ifra prosjekt properties
        Assert.assertEquals(convention.dbToolSets.coolDb.credentials.username, 'brukernavn')
        Assert.assertEquals(convention.dbToolSets.coolDb.credentials.password, 'passord')
        Assert.assertEquals(convention.dbToolSets.coolDb.tasks['PleaseAuthenticateMe'].username, 'brukernavn')
        Assert.assertEquals(convention.dbToolSets.coolDb.tasks['PleaseAuthenticateMe'].password, 'passord')

        //setter credentials på toolsetet
        testCase.configureProject {
            configureDatabasePlugin {
                toolset(type: 'hsqldb', name: 'coolDb') {
                    credentials.username = 'brukernavn2'
                    credentials.password = 'passord2'
                }
            }
        }

        //sjekker at toolset har fått satt riktige credentials
        Assert.assertEquals(convention.dbToolSets.coolDb.credentials.username, 'brukernavn2', "Forventet oppdatert brukernavn")
        Assert.assertEquals(convention.dbToolSets.coolDb.credentials.password, 'passord2', "Forventet oppdatert passord")
        //sjekker at task leser credentials ifra toolset
        Assert.assertEquals(convention.dbToolSets.coolDb.tasks['PleaseAuthenticateMe'].username, 'brukernavn2')
        Assert.assertEquals(convention.dbToolSets.coolDb.tasks['PleaseAuthenticateMe'].password, 'passord2')

        //setter passord på task
        testCase.project.tasks.'coolDbPleaseAuthenticateMe'.password = 'passord3'
        //setter default brukernavn via project properties
        testCase.project.ext.setProperty 'username', 'projectUser'


        Assert.assertEquals(convention.dbToolSets.coolDb.credentials.username, 'brukernavn2', "Forventet samme brukernavn")
        Assert.assertEquals(convention.dbToolSets.coolDb.credentials.password, 'passord2', "Forventet samme passord")
        //sjekker at credentials blir bruk som en anatomisk enhet
        Assert.assertEquals(convention.dbToolSets.coolDb.tasks['PleaseAuthenticateMe'].username, null /*fungerer kun når Console ikke finnes*/)
        Assert.assertEquals(convention.dbToolSets.coolDb.tasks['PleaseAuthenticateMe'].password, 'passord3')

        //clearer credentials på task
        testCase.project.tasks.'coolDbPleaseAuthenticateMe'.credentials.clear()

        Assert.assertEquals(convention.dbToolSets.coolDb.credentials.username, 'brukernavn2', "Forventet samme brukernavn")
        Assert.assertEquals(convention.dbToolSets.coolDb.credentials.password, 'passord2', "Forventet samme passord")
        //sjekker at credentials blir hentet ifra toolsetet igjen
        Assert.assertEquals(convention.dbToolSets.coolDb.tasks['PleaseAuthenticateMe'].username, 'brukernavn2', "Forventet conventional verdi")
        Assert.assertEquals(convention.dbToolSets.coolDb.tasks['PleaseAuthenticateMe'].password, 'passord2', "Forventet conventional verdi")

        //setter credentials  på task
        testCase.project.tasks.'coolDbPleaseAuthenticateMe'.username = 'brukernavn4'
        testCase.project.tasks.'coolDbPleaseAuthenticateMe'.password = 'passord4'

        Assert.assertEquals(convention.dbToolSets.coolDb.tasks['PleaseAuthenticateMe'].credentials.username, 'brukernavn4', "Forventet oppdatert brukernavn")
        Assert.assertEquals(convention.dbToolSets.coolDb.tasks['PleaseAuthenticateMe'].credentials.password, 'passord4', "Forventet  oppdatert passord")
        Assert.assertEquals(convention.dbToolSets.coolDb.tasks['PleaseAuthenticateMe'].username, 'brukernavn4', "Forventet oppdatert verdi")
        Assert.assertEquals(convention.dbToolSets.coolDb.tasks['PleaseAuthenticateMe'].password, 'passord4', "Forventet oppdatert verdi")
    }

    /**
     * Regression test.
     * Verifiserer at configuration ikke blir resolvet i initialiserings-fasen.
     */
    @Test
    void configurationInUnresolvedState() {
        final ProjectHelper testCase = DbToolsProjectBuilder.builder().applyDbUtilsPlugin().build();
        testCase.createNewFileWithDirsRelativeToProject('src/hsql/PleaseAuthenticateMe.sql')
        testCase.createNewFileWithDirsRelativeToProject('lib/testfile-2.3.3.jar')

        Assert.assertEquals(testCase.project.configurations.dbTools.state.toString(), "UNRESOLVED")

        testCase.configureProject {
            repositories {
                flatDir dirs: "${testCase.project.rootProject.projectDir}/lib"
            }
            configureDatabasePlugin {
                useDrivers 'test:testfile:2.3.3@jar'

                toolset(name: 'coolDb', type: 'hsqldb', prefix: 'coolDb') {
                    url = "jdbc:hsqldb:mem:${this.class.simpleName}TestApplyCredentials"
                    driver = 'org.hsqldb.jdbcDriver'
                }
            }
        }

        Assert.assertEquals(testCase.project.configurations.dbTools.state.toString(), "UNRESOLVED")
        Assert.assertTrue(testCase.project.configurations.dbTools.files.contains(testCase.project.file('lib/testfile-2.3.3.jar')))
    }

    /**
     * Tester deklarering av import task for oracle.
     * @see OracleImportTask
     */
    @Test
    void testOracleImportTask() {
        final ProjectHelper testCase = DbToolsProjectBuilder.builder().applyDbUtilsPlugin().build();
        testCase.configureProject {
            configureDatabasePlugin {
                toolset(name: 'db1', type: 'oracle') {
                    properties = [  //deklarering via felles properties for toolset
                                    username: 'brukernavn',
                                    password: 'passord',
                    ]
                    importTask()
                }
                toolset(name: 'db2', type: 'oracle') {
                    importTask() {  //deklarering via properties på task
                        username = 'brukernavn'
                        password = 'passord'
                    }
                }
            }
        }

        1..2.each {
            def taskName = "db${it}Import"
            Assert.assertNotNull(testCase.project.tasks.findByName(taskName), "Forventet at task er lagt til")
            Assert.assertTrue(testCase.project.tasks[taskName] instanceof OracleImportTask)
            testCase.project.tasks[taskName].with { OracleImportTask task ->
                Assert.assertEquals(task.username, 'brukernavn', "Forventet brukernavn")
                Assert.assertEquals(task.password, 'passord', "Forventet passord")
                Assert.assertEquals(task.transform, 'SEGMENT_ATTRIBUTES:n,OID:n:TYPE', "Forventet transform")
            }
        }
    }

    /**
     * Tester deklarering av import task for oracle.
     * @see OracleExportTask
     */
    @Test
    void testOracleExportTask() {
        final ProjectHelper testCase = DbToolsProjectBuilder.builder().applyDbUtilsPlugin().build();
        testCase.configureProject {
            configureDatabasePlugin {
                toolset(name: 'db1', type: 'oracle') {
                    properties = [  //deklarering via felles properties for toolset
                                    username: 'brukernavn',
                                    password: 'passord',
                    ]
                    exportTask()
                }
                toolset(name: 'db2', type: 'oracle') {
                    exportTask() {  //deklarering via properties på task
                        username = 'brukernavn'
                        password = 'passord'
                    }
                }
            }
        }

        1..2.each {
            def taskName = "db${it}Export"
            Assert.assertNotNull(testCase.project.tasks.findByName(taskName), "Forventet at task er lagt til")
            Assert.assertTrue(testCase.project.tasks[taskName] instanceof OracleExportTask)
            testCase.project.tasks[taskName].with { OracleExportTask task ->
                Assert.assertEquals(task.username, 'brukernavn', "Forventet brukernavn")
                Assert.assertEquals(task.password, 'passord', "Forventet passord")
            }
        }
    }

    /**
     * Tester deklarering av {@link PatchTask} task
     * @since 1.3
     */
    @Test
    void testPatchTask() {
        final ProjectHelper testCase = DbToolsProjectBuilder.builder().applyDbUtilsPlugin().build();
        testCase.configureProject {
            configureDatabasePlugin {
                toolset(name: 'db1', type: 'oracle') {
                    properties = [  //deklarering via felles properties for toolset
                                    username: 'brukernavn',
                                    password: 'passord',
                    ]

                    patch {
                        patchTask('TestSchema', sqlFile: "foo.sql", description: 'Task med verdier ifra konfigurasjon og convention')
                    }
                }
            }
        }

        final DbtoolsConvention convention = testCase.project.convention.plugins.db
        Assert.assertNotNull(convention.dbToolSets.db1, "Forventet toolset objekt")
        Assert.assertNotNull(convention.dbToolSets.db1.patch['null'], "Forventet patch objekt")
        Assert.assertNotNull(convention.dbToolSets.db1.patch['null'].tasks['TestSchema'], "Forventet patch task")

        convention.dbToolSets.db1.patch['null'].tasks['TestSchema'].with { PatchTask task ->
            Assert.assertEquals(task.sqlFile, testCase.project.file("foo.sql"), "Fil for patch task")
            Assert.assertEquals(task.component, 'null', "component for patch task")
            Assert.assertEquals(task.failOnError, true, "FailOnError for patch task")
            Assert.assertEquals(task.failOnWarning, true, "FailOnWarning for patch task")

            Assert.assertEquals(task.singlestep, false, "singlestep for patch task")
            Assert.assertNull(task.schema, "schema for patch task")
        }

        final Task independentTask = testCase.project.task('IndependentTask', type: PatchTask.class) {
            description = "Task som ikke legges til via convention/configuration, men konfigureres manuelt"
            sqlFile = project.file("patchFoo.sql")
            singlestep = true
        }
        independentTask.with { PatchTask task ->
            Assert.assertEquals(task.sqlFile, testCase.project.file("patchFoo.sql"), "Fil for patch task")
            Assert.assertEquals(task.component, 'null', "component for patch task")
            Assert.assertEquals(task.failOnError, true, "FailOnError for patch task")
            Assert.assertEquals(task.failOnWarning, true, "FailOnWarning for patch task")

            Assert.assertEquals(task.singlestep, true, "singlestep for patch task")
            Assert.assertNull(task.schema, "schema for patch task")
        }

    }

    /**
     * Tester deklarering av {@link PatchTask} task
     * @since 1.3
     */
    @Test
    void testPatchTaskSchema() {
        final ProjectHelper testCase = DbToolsProjectBuilder.builder().applyDbUtilsPlugin().build();
        testCase.configureProject {
            configureDatabasePlugin {
                toolset(name: 'db1', type: 'oracle') {
                    properties = [  //deklarering via felles properties for toolset
                                    username: 'brukernavn',
                                    password: 'passord',
                    ]

                    patch {
                        schema = 'schema2'
                        patchTask('TestSchema', sqlFile: "foo.sql", description: 'Task med verdier ifra konfigurasjon og convention')
                    }
                }
            }
        }

        final DbtoolsConvention convention = testCase.project.convention.plugins.db
        Assert.assertNotNull(convention.dbToolSets.db1, "Forventet toolset objekt")
        Assert.assertNotNull(convention.dbToolSets.db1.patch['null'], "Forventet patch objekt")
        Assert.assertNotNull(convention.dbToolSets.db1.patch['null'].tasks['TestSchema'], "Forventet patch task")

        convention.dbToolSets.db1.patch['null'].tasks['TestSchema'].with { PatchTask task ->
            Assert.assertNotNull(task.schema, "schema for patch task")
            Assert.assertEquals(task.schema, 'schema2', "schema for patch task")
        }

        final Task independentTask = testCase.project.task('IndependentTask', type: PatchTask.class) {
            description = "Task som ikke legges til via convention/configuration, men konfigureres manuelt"
            sqlFile = project.file("patchFoo.sql")
            singlestep = true
            schema = 'schema3'
        }
        independentTask.with { PatchTask task ->
            Assert.assertNotNull(task.schema, "schema for patch task")
            Assert.assertEquals(task.schema, 'schema3', "schema for patch task")
        }

    }

    /**
     * Tester deklarering av {@link SyncPatchTask} task
     * @since 1.3
     */
    @Test
    void testSyncPatchTask() {
        final ProjectHelper testCase = DbToolsProjectBuilder.builder().applyDbUtilsPlugin().build();
        testCase.configureProject {
            configureDatabasePlugin {
                toolset(name: 'db1', type: 'oracle') {
                    properties = [  //deklarering via felles properties for toolset
                                    username: 'brukernavn',
                                    password: 'passord',
                    ]

                    patch {
                        syncPatchTask('TestSchema', sqlFile: "foo.sql", description: 'Task med verdier ifra konfigurasjon og convention') {
                            failOnError = false
                            patchTypes = ['RERUN']
                        }
                    }
                }
            }
        }

        final DbtoolsConvention convention = testCase.project.convention.plugins.db
        Assert.assertNotNull(convention.dbToolSets.db1, "Forventet toolset objekt")
        Assert.assertNotNull(convention.dbToolSets.db1.patch['null'], "Forventet patch objekt")
        Assert.assertNotNull(convention.dbToolSets.db1.patch['null'].tasks['TestSchema'], "Forventet patch task")

        convention.dbToolSets.db1.patch['null'].tasks['TestSchema'].with { SyncPatchTask task ->
            Assert.assertEquals(task.sqlFile, testCase.project.file("foo.sql"), "Fil for patch task")
            Assert.assertEquals(task.component, 'null', "component for patch task")
            Assert.assertEquals(task.failOnError, false, "FailOnError for patch task")
            Assert.assertEquals(task.failOnWarning, false, "FailOnWarning for patch task")
            Assert.assertEquals(task.patchTypes.size(), 1, "patchTypes for patch task")
            Assert.assertTrue(task.patchTypes.containsAll(['RERUN']), "patchTypes for patch task")
            Assert.assertEquals(task.singlestep, false, "singlestep for patch task")
        }

        final Task independentTask = testCase.project.task('IndependentTask', type: SyncPatchTask.class) {
            description = "Task som ikke legges til via convention/configuration, men konfigureres manuelt"
            sqlFile = project.file("patchFoo.sql")
            singlestep = true
        }
        independentTask.with { SyncPatchTask task ->
            Assert.assertEquals(task.sqlFile, testCase.project.file("patchFoo.sql"), "Fil for patch task")
            Assert.assertEquals(task.component, 'null', "component for patch task")
            Assert.assertEquals(task.failOnError, true, "FailOnError for patch task")
            Assert.assertEquals(task.failOnWarning, false, "FailOnWarning for patch task")

            Assert.assertTrue(task.patchTypes.containsAll(['INDEX', 'TYPE', 'PACKAGE', 'FUNCTION']), "patchTypes for patch task - see SKTOOLS-86")
            Assert.assertEquals(task.singlestep, true, "singlestep for patch task")
        }

    }

    /**
     * Tester deklarering av {@link DefineLatestPatchVersionTask} task
     * @since 1.3
     */
    @Test
    void testDefineLatestPatchVersionTask() {
        final ProjectHelper testCase = DbToolsProjectBuilder.builder().applyDbUtilsPlugin().build();
        testCase.configureProject {
            configureDatabasePlugin {
                toolset(name: 'db1', type: 'oracle') {
                    patch {
                        defineLatestPatchVersionTask('AssignLatestPatchlevel', sqlFile: "foo.sql", description: 'Task med verdier ifra konfigurasjon og convention')
                    }
                }
            }
        }

        final DbtoolsConvention convention = testCase.project.convention.plugins.db
        Assert.assertNotNull(convention.dbToolSets.db1, "Forventet toolset objekt")
        Assert.assertNotNull(convention.dbToolSets.db1.patch['null'], "Forventet patch objekt")
        Assert.assertNotNull(convention.dbToolSets.db1.patch['null'].tasks['AssignLatestPatchlevel'], "Forventet patch task")
        Assert.assertTrue(convention.dbToolSets.db1.patch['null'].tasks['AssignLatestPatchlevel'] instanceof DefineLatestPatchVersionTask, "Forventet type")

        convention.dbToolSets.db1.patch['null'].tasks['AssignLatestPatchlevel'].with { DefineLatestPatchVersionTask task ->
            Assert.assertEquals(task.sqlFile, testCase.project.file("foo.sql"), "Fil for patch task")
            Assert.assertEquals(task.component, 'null', "component for patch task")
        }

        final Task independentTask = testCase.project.task('IndependentTask', type: DefineLatestPatchVersionTask.class) {
            description = "Task som ikke legges til via convention/configuration, men konfigureres manuelt"
            sqlFile = project.file("patchFoo.sql")
        }
        independentTask.with { DefineLatestPatchVersionTask task ->
            Assert.assertEquals(task.sqlFile, testCase.project.file("patchFoo.sql"), "Fil for patch task")
            Assert.assertEquals(task.component, 'null', "component for patch task")
        }

    }

    /**
     * @since 1.3 - SKTOOLS-88
     */
    @Test
    void testInfoTask() {
        final ProjectHelper testCase = DbToolsProjectBuilder.builder().applyDbUtilsPlugin().build();
        testCase.configureProject {
            configureDatabasePlugin {
                toolset(name: 'coolDb', type: 'hsqldb', prefix: 'coolDb') {
                    url = "jdbc:hsqldb:mem:${this.class.simpleName}TestApplyCredentials"
                    driver = 'org.hsqldb.jdbcDriver'
                }
            }
        }

        final Task info = testCase.project.tasks.findByName('info')
        Assert.assertNotNull(info, "Forventet at task er lagt til")

        info.execute()
    }


    /**
     * Verifiserer at taskSequence syntax fungerer på project
     * @since 1.4
     */
    @Test
    void taskSequenceIsExtendedToProject() {
        final ProjectHelper testCase = DbToolsProjectBuilder.builder().applyDbUtilsPlugin().build();
        testCase.configureProject {
            taskSequence('nestedTask') {
                dependsOn task('task1')
                dependsOn task('task2')
            }
        }

        assertThat(testCase.project.tasks.findByName('nestedTask')).describedAs("Forventet task").isNotNull()
        assertThat(testCase.project.tasks.findByName('task1')).describedAs("Forventet task").isNotNull()
        assertThat(testCase.project.tasks.findByName('task2')).describedAs("Forventet task").isNotNull()

        assertThat(testCase.project.tasks.findByName('nestedTask').dependsOn).describedAs("dependencies")
                .contains(testCase.project.tasks.'task1', testCase.project.tasks.'task2')
    }
}