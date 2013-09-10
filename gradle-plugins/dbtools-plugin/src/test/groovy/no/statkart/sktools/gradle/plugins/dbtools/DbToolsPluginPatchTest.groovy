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
