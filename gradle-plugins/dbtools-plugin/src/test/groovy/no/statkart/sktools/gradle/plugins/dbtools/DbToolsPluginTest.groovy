package no.statkart.sktools.gradle.plugins.dbtools

import org.testng.annotations.Test
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import org.testng.Assert

/**
 *
 */
class DbToolsPluginTest {


    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testAppplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-dbtools-plugin'

        Assert.assertTrue(project.convention.plugins.db instanceof DbtoolsConvention)

    }


    /**
     * Tester og demonstrerer angivelse av credentials.
     *
     * Testen illustrerer at credentials på toolset og tasker kan bli satt runtime.
     * En illustrerer også at disse kan løsrives dersom en ønsker, slik at en kan be brukeren om å tate inn credentials i spesielle tilfeller.
     */
    @Test
    void testApplyCredentials() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-dbtools-plugin'

        project.mkdir('src/hsql')
        project.file('src/hsql/PleaseAuthenticateMe.sql').createNewFile()

        project.configureDatabasePlugin {
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

        assert project.tasks.findByName('coolDbPleaseAuthenticateMe') != null //forutsetter at denne er lagt til

        //tester defaults - username og password blir lest ifra prosjekt properties
        assert project.dbToolSets.coolDb.credentials.username == 'brukernavn'
        assert project.dbToolSets.coolDb.credentials.password == 'passord'
        assert project.dbToolSets.coolDb.tasks['PleaseAuthenticateMe'].username == 'brukernavn'
        assert project.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.password == 'passord'


        //setter credentials på toolsetet
        project.configureDatabasePlugin {
            toolset(type:'hsqldb', name:'coolDb') {
                credentials.username = 'brukernavn2'
                credentials.password = 'passord2'
            }
        }

        //sjekker at toolset har fått satt riktige credentials
        assert project.dbToolSets.coolDb.credentials.username == 'brukernavn2'
        assert project.dbToolSets.coolDb.credentials.password == 'passord2'
        //sjekker at task leser credentials ifra toolset
        assert project.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.username == 'brukernavn2'
        assert project.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.password == 'passord2'


        //setter passord på task
        project.project.tasks.'coolDbPleaseAuthenticateMe'.password = 'passord3'
        //setter default brukernavn via project properties
        project.ext.setProperty 'username', 'projectUser'


        assert project.dbToolSets.coolDb.credentials.username == 'brukernavn2'
        assert project.dbToolSets.coolDb.credentials.password == 'passord2'
        //sjekker at credentials blir bruk som en anatomisk enhet
        assert project.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.username == null    //fungerer kun n[r Console ikke finnes
        assert project.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.password == 'passord3'



        //clearer credentials på task
        project.project.tasks.'coolDbPleaseAuthenticateMe'.credentials.clear()

        assert project.dbToolSets.coolDb.credentials.username == 'brukernavn2'
        assert project.dbToolSets.coolDb.credentials.password == 'passord2'
        //sjekker at credentials blir hentet ifra toolsetet igjen
        assert project.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.username == 'brukernavn2'
        assert project.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.password == 'passord2'


        //setter credentials  på task
        project.project.tasks.'coolDbPleaseAuthenticateMe'.username = 'brukernavn4'
        project.project.tasks.'coolDbPleaseAuthenticateMe'.password = 'passord4'

        assert project.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.username == 'brukernavn4'
        assert project.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.password == 'passord4'
        assert project.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.credentials.username == 'brukernavn4'
        assert project.dbToolSets.coolDb.tasks.PleaseAuthenticateMe.credentials.password == 'passord4'

    }



    /**
     * Tester deklarering av import task for oracle.
     *
     */
    @Test
    void testOracleImportTask() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-dbtools-plugin'

        project.configureDatabasePlugin {
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
            assert project.tasks.findByName(taskName) != null //forutsetter at denne er lagt til
            assert project.tasks[taskName].username == 'brukernavn'
            assert project.tasks[taskName].password == 'passord'
        }
    }

    
    /**
     * Tester deklarering av import task for oracle.
     *
     */
    @Test
    void testOracleExportTask() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-dbtools-plugin'

        project.configureDatabasePlugin {
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
            assert project.tasks.findByName(taskName) != null //forutsetter at denne er lagt til
            assert project.tasks[taskName].username == 'brukernavn'
            assert project.tasks[taskName].password == 'passord'
        }
    }    
    
}
