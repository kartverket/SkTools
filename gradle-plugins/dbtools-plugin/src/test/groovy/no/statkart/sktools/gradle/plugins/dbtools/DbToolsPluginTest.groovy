package no.statkart.sktools.gradle.plugins.dbtools

import org.testng.annotations.Test
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import org.testng.Assert

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

        assert testCase.project.tasks.findByName('coolDbPleaseAuthenticateMe') != null //forutsetter at denne er lagt til

        //tester defaults - username og password blir lest ifra prosjekt properties
        assert testCase.convention.dbToolSets.coolDb.credentials.username == 'brukernavn'
        assert testCase.convention.dbToolSets.coolDb.credentials.password == 'passord'
        assert testCase.convention.dbToolSets.coolDb.tasks['PleaseAuthenticateMe'].username == 'brukernavn'
        assert testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.password == 'passord'


        //setter credentials på toolsetet
        testCase.configureDatabasePlugin {
            toolset(type:'hsqldb', name:'coolDb') {
                credentials.username = 'brukernavn2'
                credentials.password = 'passord2'
            }
        }

        //sjekker at toolset har fått satt riktige credentials
        assert testCase.convention.dbToolSets.coolDb.credentials.username == 'brukernavn2'
        assert testCase.convention.dbToolSets.coolDb.credentials.password == 'passord2'
        //sjekker at task leser credentials ifra toolset
        assert testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.username == 'brukernavn2'
        assert testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.password == 'passord2'


        //setter passord på task
        testCase.project.tasks.'coolDbPleaseAuthenticateMe'.password = 'passord3'
        //setter default brukernavn via project properties
        testCase.project.ext.setProperty 'username', 'projectUser'


        assert testCase.convention.dbToolSets.coolDb.credentials.username == 'brukernavn2'
        assert testCase.convention.dbToolSets.coolDb.credentials.password == 'passord2'
        //sjekker at credentials blir bruk som en anatomisk enhet
        assert testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.username == null    //fungerer kun n[r Console ikke finnes
        assert testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.password == 'passord3'



        //clearer credentials på task
        testCase.project.tasks.'coolDbPleaseAuthenticateMe'.credentials.clear()

        assert testCase.convention.dbToolSets.coolDb.credentials.username == 'brukernavn2'
        assert testCase.convention.dbToolSets.coolDb.credentials.password == 'passord2'
        //sjekker at credentials blir hentet ifra toolsetet igjen
        assert testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.username == 'brukernavn2'
        assert testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.password == 'passord2'


        //setter credentials  på task
        testCase.project.tasks.'coolDbPleaseAuthenticateMe'.username = 'brukernavn4'
        testCase.project.tasks.'coolDbPleaseAuthenticateMe'.password = 'passord4'

        assert testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.username == 'brukernavn4'
        assert testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.password == 'passord4'
        assert testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.credentials.username == 'brukernavn4'
        assert testCase.convention.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.credentials.password == 'passord4'

    }



    /**
     * Tester deklarering av import task for oracle.
     *
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
            assert testCase.project.tasks.findByName(taskName) != null //forutsetter at denne er lagt til
            assert testCase.project.tasks[taskName].username == 'brukernavn'
            assert testCase.project.tasks[taskName].password == 'passord'
        }
    }

    
    /**
     * Tester deklarering av import task for oracle.
     *
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
            assert testCase.project.tasks.findByName(taskName) != null //forutsetter at denne er lagt til
            assert testCase.project.tasks[taskName].username == 'brukernavn'
            assert testCase.project.tasks[taskName].password == 'passord'
        }
    }    
    
}
