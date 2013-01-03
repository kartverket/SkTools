package no.statkart.sktools.gradle.plugins.dbtools

import org.testng.annotations.Test
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import org.gradle.api.Task
import org.testng.Assert
import no.statkart.sktools.testutils.IntelliJTestUtil

/**
 * Tester patch funksjonalitet
 */
class DbToolsPluginPatchTest extends HSQLDBTest {

    /**
     * Tester at standard test blir lagt til for en "patch"
     */
    @Test
    void testStdPatchTasks() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        // STEG 1 - setter opp testmaterie

        // STEG 2 - konfigurering av plugin
        project.ext.setProperty 'username', username
        project.ext.setProperty 'password', password

        project.apply plugin: 'sktools-dbtools-plugin'


        DbtoolsConvention convention = project.convention.getPlugin(DbtoolsConvention.class)
        convention.configureDatabasePlugin {
            toolset(name: 'Prefix', type: 'hsqldb', prefix: 'Prefix') {

                url = sql.connection.properties.URL
                driver = jdbcDriverClassString

                patch {
                    //tom konfigurasjon
                }

            }
        }

        // STEG 3 - tester
        Task printPatchVersionTask = project.tasks.getByName('prefixPrintPatchVersion')
        Task setIndexInSyncWithPatchTask = project.tasks.getByName('prefixSetIndexInSyncWithPatch')

        Assert.assertNotNull(printPatchVersionTask, "Forventet task for 'prefixPrintPatchVersion")
        Assert.assertNotNull(setIndexInSyncWithPatchTask, "Forventet task for 'prefixSetIndexInSyncWithPatch")

    }

    /**
     * Tester patchDatabase target
     */
    @Test
    void testPatchDatabase() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        // STEG 1 - setter opp testmaterie

        File patchFile = createPatchFile()

        // STEG 2 - konfigurering av plugin
        project.ext.setProperty 'username', username
        project.ext.setProperty 'password', password

        project.apply plugin: 'sktools-dbtools-plugin'


        DbtoolsConvention convention = project.convention.getPlugin(DbtoolsConvention.class)
        convention.configureDatabasePlugin {
            toolset(name: 'Prefix', type: 'hsqldb', prefix: 'Prefix') {

                url = sql.connection.properties.URL
                driver = jdbcDriverClassString

                patch {
                    patchTask('TestSchema', sqlFile:patchFile)
                }

            }
        }

        // STEG 3 - tester
        Task printPatchVersionTask = project.tasks.getByName('prefixPatchTestSchema')

        //
//        if (IntelliJTestUtil.isIntelliJTestRuntime) {
//            printPatchVersionTask.classpath = project.files(this.class.classLoader.properties['URLs'])
//        }

        printPatchVersionTask.execute()

    }

    // helper methods -->

    private static File createPatchFile(File dir = null) {
        File patchFile = File.createTempFile("patch", ".sql", dir)
        patchFile.withPrintWriter {
            it.println '''

--kommentar

-- PATCH DB.MIN.VERSION="<any>"
-- PATCH DATA DB.VERSION="1.0" PATCH.NO="1" "Create test table"

CREATE TABLE TEST_TABLE (
   ID INTEGER NOT NULL,
   NAVN VARCHAR(32) NOT NULL,
   PRIMARY KEY (ID)
);

-- PATCH INDEX DB.VERSION="1.0" PATCH.NO="2" "Indexing names"
CREATE INDEX TEST_TABLE_IDX_NAME ON TEST_TABLE(NAVN);

-- PATCH DATA DB.VERSION="1.0" PATCH.NO="3" "Inserting Chuck Norris"
INSERT INTO TEST_TABLE (ID, NAVN) VALUES (1, 'CHUCK NORRIS');

-- PATCH INDEX DB.VERSION="1.0" PATCH.NO="4" "Indexing names and id"
CREATE INDEX TEST_TABLE_IDX_01 ON TEST_TABLE(NAVN, ID);

'''
            it.flush()
        }
        return patchFile
    }



}
