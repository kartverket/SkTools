package no.statkart.sktools.gradle.plugins.dbtools

import org.testng.annotations.Test
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import org.testng.Assert
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch.PatchTask
import org.gradle.api.Task
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch.SyncPatchTask
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleExportTask
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleImportTask
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch.DefineLatestPatchVersionTask

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
        final def testCase = new DbToolsPluginTestCase()

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
        final def testCase = new DbToolsPluginTestCase()

        testCase.createNewFileWithDirsRelativeToProject('src/hsql/PleaseAuthenticateMe.sql')

        testCase.configureDatabasePlugin {
            toolset(name:'coolDb', type:'hsqldb', prefix:'coolDb') {
                url = "jdbc:hsqldb:mem:${this.class.simpleName}TestApplyCredentials"
                driver = 'org.hsqldb.jdbcDriver'

                sqlTask( 'PleaseAuthenticateMe', sqlFile:'src/hsql/PleaseAuthenticateMe.sql')
                properties = [
                        username: 'brukernavn',
                        password: 'passord',
                ]
            }
        }

        Assert.assertNotNull(testCase.project.tasks.findByName('coolDbPleaseAuthenticateMe'), "Forventet at task er lagt til")

        //tester defaults - username og password blir lest ifra prosjekt properties
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.credentials.username, 'brukernavn')
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.credentials.password, 'passord')
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.tasks['PleaseAuthenticateMe'].username, 'brukernavn')
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.password, 'passord')


        //setter credentials på toolsetet
        testCase.configureDatabasePlugin {
            toolset(type:'hsqldb', name:'coolDb') {
                credentials.username = 'brukernavn2'
                credentials.password = 'passord2'
            }
        }

        //sjekker at toolset har fått satt riktige credentials
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.credentials.username, 'brukernavn2', "Forventet oppdatert brukernavn")
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.credentials.password, 'passord2', "Forventet oppdatert passord")
        //sjekker at task leser credentials ifra toolset
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.username, 'brukernavn2')
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.password, 'passord2')


        //setter passord på task
        testCase.project.tasks.'coolDbPleaseAuthenticateMe'.password = 'passord3'
        //setter default brukernavn via project properties
        testCase.project.ext.setProperty 'username', 'projectUser'


        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.credentials.username, 'brukernavn2', "Forventet samme brukernavn")
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.credentials.password, 'passord2', "Forventet samme passord")
        //sjekker at credentials blir bruk som en anatomisk enhet
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.username, null) //fungerer kun når Console ikke finnes
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.password, 'passord3')



        //clearer credentials på task
        testCase.project.tasks.'coolDbPleaseAuthenticateMe'.credentials.clear()

        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.credentials.username, 'brukernavn2', "Forventet samme brukernavn")
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.credentials.password, 'passord2', "Forventet samme passord")
        //sjekker at credentials blir hentet ifra toolsetet igjen
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.username, 'brukernavn2', "Forventet conventional verdi")
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.password, 'passord2',  "Forventet conventional verdi")


        //setter credentials  på task
        testCase.project.tasks.'coolDbPleaseAuthenticateMe'.username = 'brukernavn4'
        testCase.project.tasks.'coolDbPleaseAuthenticateMe'.password = 'passord4'

        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.credentials.username, 'brukernavn4', "Forventet oppdatert brukernavn")
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.credentials.password, 'passord4', "Forventet  oppdatert passord")
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.username, 'brukernavn4', "Forventet oppdatert verdi")
        Assert.assertEquals(testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.password, 'passord4',  "Forventet oppdatert verdi")


    }



    /**
     * Tester deklarering av import task for oracle.
     * @see OracleImportTask
     */
    @Test
    void testOracleImportTask() {
        final def testCase = new DbToolsPluginTestCase()

        testCase.configureDatabasePlugin {
            toolset(name:'db1', type:'oracle') {
                properties = [  //deklarering via felles properties for toolset
                        username: 'brukernavn',
                        password: 'passord',
                ]
                importTask()
            }
            toolset(name:'db2', type:'oracle') {
                importTask() {  //deklarering via properties på task
                    username = 'brukernavn'
                    password = 'passord'
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
            }
        }
    }

    
    /**
     * Tester deklarering av import task for oracle.
     * @see OracleExportTask
     */
    @Test
    void testOracleExportTask() {
        final def testCase = new DbToolsPluginTestCase()

        testCase.configureDatabasePlugin {
            toolset(name:'db1', type:'oracle') {
                properties = [  //deklarering via felles properties for toolset
                        username: 'brukernavn',
                        password: 'passord',
                ]
                exportTask()
            }
            toolset(name:'db2', type:'oracle') {
                exportTask() {  //deklarering via properties på task
                    username = 'brukernavn'
                    password = 'passord'
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
        final def testCase = new DbToolsPluginTestCase()

        testCase.configureDatabasePlugin {
            toolset(name:'db1', type:'oracle') {
                properties = [  //deklarering via felles properties for toolset
                        username: 'brukernavn',
                        password: 'passord',
                ]

                patch {
                    patchTask('TestSchema', sqlFile:"foo.sql", description: 'Task med verdier ifra konfigurasjon og convention')
                }
            }
        }

        Assert.assertNotNull(testCase.convention.dbToolSets.db1, "Forventet toolset objekt")
        Assert.assertNotNull(testCase.convention.dbToolSets.db1.patch['null'], "Forventet patch objekt")
        Assert.assertNotNull(testCase.convention.dbToolSets.db1.patch['null'].tasks['TestSchema'], "Forventet patch task")

        testCase.convention.dbToolSets.db1.patch['null'].tasks['TestSchema'].with { PatchTask task ->
            Assert.assertEquals(task.sqlFile, testCase.project.file("foo.sql"), "Fil for patch task")
            Assert.assertEquals(task.component, 'null', "component for patch task")
            Assert.assertEquals(task.failOnError, true, "FailOnError for patch task")
            Assert.assertEquals(task.failOnWarning, true, "FailOnWarning for patch task")

            Assert.assertEquals(task.singlestep, false, "singlestep for patch task")
        }

        final Task independentTask = testCase.project.task('IndependentTask', type: PatchTask.class) {
            desciption = "Task som ikke legges til via convention/configuration, men konfigureres manuelt"
            sqlFile = project.file("patchFoo.sql")
            singlestep = true
        }
        independentTask.with { PatchTask task ->
            Assert.assertEquals(task.sqlFile, testCase.project.file("patchFoo.sql"), "Fil for patch task")
            Assert.assertEquals(task.component, 'null', "component for patch task")
            Assert.assertEquals(task.failOnError, true, "FailOnError for patch task")
            Assert.assertEquals(task.failOnWarning, true, "FailOnWarning for patch task")

            Assert.assertEquals(task.singlestep, true, "singlestep for patch task")
        }

    }

    /**
     * Tester deklarering av {@link SyncPatchTask} task
     * @since 1.3
     */
    @Test
    void testSyncPatchTask() {
        final def testCase = new DbToolsPluginTestCase()

        testCase.configureDatabasePlugin {
            toolset(name:'db1', type:'oracle') {
                properties = [  //deklarering via felles properties for toolset
                        username: 'brukernavn',
                        password: 'passord',
                ]

                patch {
                    syncPatchTask('TestSchema', sqlFile:"foo.sql", description: 'Task med verdier ifra konfigurasjon og convention') {
                        failOnError = false
                        patchTypes = ['RERUN']
                    }
                }
            }
        }

        Assert.assertNotNull(testCase.convention.dbToolSets.db1, "Forventet toolset objekt")
        Assert.assertNotNull(testCase.convention.dbToolSets.db1.patch['null'], "Forventet patch objekt")
        Assert.assertNotNull(testCase.convention.dbToolSets.db1.patch['null'].tasks['TestSchema'], "Forventet patch task")

        testCase.convention.dbToolSets.db1.patch['null'].tasks['TestSchema'].with { SyncPatchTask task ->
            Assert.assertEquals(task.sqlFile, testCase.project.file("foo.sql"), "Fil for patch task")
            Assert.assertEquals(task.component, 'null', "component for patch task")
            Assert.assertEquals(task.failOnError, false, "FailOnError for patch task")
            Assert.assertEquals(task.failOnWarning, false, "FailOnWarning for patch task")
            Assert.assertEquals(task.patchTypes.size(), 1, "patchTypes for patch task")
            Assert.assertTrue(task.patchTypes.containsAll(['RERUN']), "patchTypes for patch task")
            Assert.assertEquals(task.singlestep, false, "singlestep for patch task")
        }

        final Task independentTask = testCase.project.task('IndependentTask', type: SyncPatchTask.class) {
            desciption = "Task som ikke legges til via convention/configuration, men konfigureres manuelt"
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
        final def testCase = new DbToolsPluginTestCase()

        testCase.configureDatabasePlugin {
            toolset(name:'db1', type:'oracle') {
                patch {
                    defineLatestPatchVersionTask('AssignLatestPatchlevel', sqlFile:"foo.sql", description: 'Task med verdier ifra konfigurasjon og convention')
                }
            }
        }

        Assert.assertNotNull(testCase.convention.dbToolSets.db1, "Forventet toolset objekt")
        Assert.assertNotNull(testCase.convention.dbToolSets.db1.patch['null'], "Forventet patch objekt")
        Assert.assertNotNull(testCase.convention.dbToolSets.db1.patch['null'].tasks['AssignLatestPatchlevel'], "Forventet patch task")
        Assert.assertTrue(testCase.convention.dbToolSets.db1.patch['null'].tasks['AssignLatestPatchlevel'] instanceof DefineLatestPatchVersionTask, "Forventet type")

        testCase.convention.dbToolSets.db1.patch['null'].tasks['AssignLatestPatchlevel'].with { DefineLatestPatchVersionTask task ->
            Assert.assertEquals(task.sqlFile, testCase.project.file("foo.sql"), "Fil for patch task")
            Assert.assertEquals(task.component, 'null', "component for patch task")
        }

        final Task independentTask = testCase.project.task('IndependentTask', type: DefineLatestPatchVersionTask.class) {
            desciption = "Task som ikke legges til via convention/configuration, men konfigureres manuelt"
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
        final def testCase = new DbToolsPluginTestCase()

        testCase.configureDatabasePlugin {
            toolset(name:'coolDb', type:'hsqldb', prefix:'coolDb') {
                url = "jdbc:hsqldb:mem:${this.class.simpleName}TestApplyCredentials"
                driver = 'org.hsqldb.jdbcDriver'
            }
        }

        final Task info = testCase.project.tasks.findByName('info')
        Assert.assertNotNull(info, "Forventet at task er lagt til")

        info.execute()
    }

}