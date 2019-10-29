package no.statkart.sktools.gradle.plugins.wsdocgen

import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.TestKitBase
import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import org.gradle.api.Project
import org.testng.annotations.Test

import static no.statkart.sktools.gradle.testutils.filewriter.WsDocgenTestutilFilewriter.writeInterfaceServiceWSBean
import static no.statkart.sktools.gradle.testutils.filewriter.WsDocgenTestutilFilewriter.writeSimpleDemoServiceWSBean
import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.contentOf
import static org.testng.Assert.*

/**
 * Test av {@link WsDocGenPlugin}
 *
 * <p>
 *     For testing av generering av dokumentasjon se {@link no.statkart.sktools.utils.wsdocgen.processor.WSDocProcessorTest}
 * </p>
 *
 * @author Leif Lislegård
 */
class WsDocGenPluginTest extends TestKitBase {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void appplyPlugin() {
        writeFile("build.gradle", """
            plugins {
              id 'sktools-wsdocgen-plugin'
            }
        """)

        assertNoFailures(testGradleBuild("assemble"))
    }


    @Test
    void genWsDocGeneratesDocumentationForAllSourceSets() {
        writeFile("build.gradle", """
            plugins {
              id 'java'
              id 'sktools-wsdocgen-plugin'
            }

            sourceSets {
                main.wsdoc.group { }
                other.wsdoc.group { }
            }
        """)

        writeSimpleDemoServiceWSBean(file("src/main/java")) //generates simple source file
        writeSimpleDemoServiceWSBean(file("src/other/java")) //generates simple source file

        assertNoFailures(testGradleBuild(WsDocGenPlugin.GEN_TASK_NAME))

        assertThat(file('build/main/wsdoc/Group1/TestService.html')).exists()
        assertThat(file('build/other/wsdoc/Group1/TestService.html')).exists()
    }


    @Test
    void tasksForVanillaConfiguration() {
        //forks a new java project in a temp folder
        final ProjectHelper projectHelper = GradleProjectBuilder.builder().build {
            apply plugin: 'java'
            apply plugin: 'sktools-wsdocgen-plugin'

            sourceSets {
                main.wsdoc.group { }
            }
        }

        final Project project = projectHelper.project

        assertNotNull project.tasks.findByName('genWsdoc'), "gen task"
        assertNotNull project.tasks.findByName('genMainWsdoc'), "gen task for source set"
        assertNotNull project.tasks.findByName('genMainWsdocGroup1'), "gen task for group1"

        assertTrue project.tasks['genWsdoc'].dependsOn.contains('genMainWsdoc')
        assertTrue project.tasks['genMainWsdoc'].dependsOn.contains(project.tasks['genMainWsdocGroup1'])
    }

    /**
     * Samletask for source sets skal ikke komme med dersom tomme wsdoc grupper (ikke annotert i konfigurasjon)
     */
    @Test
    void noTasksForNoneAnnotatedSourceSets() {
        //forks a new java project in a temp folder
        final ProjectHelper projectHelper = GradleProjectBuilder.builder().build {
            apply plugin: 'java'
            apply plugin: 'sktools-wsdocgen-plugin'

            sourceSets {
                main { }
                other.wsdoc.group { }
            }
        }

        final Project project = projectHelper.project

        assertNotNull project.tasks.findByName('genWsdoc'), "gen task"

        //negative test
        assertNull project.tasks.findByName('genMainWsdoc'), "gen task for source set"
        assertNull project.tasks.findByName('genMainWsdocGroup1'), "gen task for group1"

        //positive test
        assertNotNull project.tasks.findByName('genOtherWsdoc'), "gen task for source set"
        assertNotNull project.tasks.findByName('genOtherWsdocGroup1'), "gen task for group1"
    }

    @Test
    void sourceSetForVanillaConfiguration() {
        //forks a new java project in a temp folder
        final ProjectHelper projectHelper = GradleProjectBuilder.builder().build {
            apply plugin: 'java'
            apply plugin: 'sktools-wsdocgen-plugin'

            sourceSets {
                main.wsdoc.group { }
                other
            }
        }

        final Project project = projectHelper.project
        assertNotNull project.sourceSets.main.wsdoc
        assertNotNull project.sourceSets.other.wsdoc

        assertEquals project.sourceSets.main.wsdoc.size(), 1
        assertEquals project.sourceSets.other.wsdoc.size(), 0

        assertEquals project.sourceSets.main.wsdoc[0].includes, ['**/*Bean.java']
        assertEquals project.sourceSets.main.wsdoc[0].targetPath, 'build/main/wsdoc/Group1'
    }


    @Test
    void canCustomizeOutputLocation() {
        //forks a new java project in a temp folder
        final ProjectHelper projectHelper = GradleProjectBuilder.builder().build {
            apply plugin: 'sktools-wsdocgen-plugin'

            sourceSets {
                multi {
                    wsdoc {
                        group { targetPath 'gen/doc' }
                        group { targetPath 'gen/doc2' }
                    }
                }
                main.wsdoc.group { /*default*/ }
                other.wsdoc.group { targetPath 'gen/doc' }
            }
        }

        final Project project = projectHelper.project

        //tests vanilla configuration
        assertEquals project.sourceSets.main.wsdoc[0].targetPath, 'build/main/wsdoc/Group1'
        assertEquals project.tasks.genMainWsdocGroup1.destinationDir, project.file('build/main/wsdoc/Group1')

        //test override
        assertEquals project.sourceSets.other.wsdoc[0].targetPath, 'gen/doc'
        assertEquals project.tasks.genOtherWsdocGroup1.destinationDir, project.file('gen/doc')

        //test multiple groups
        assertEquals project.sourceSets.multi.wsdoc[0].targetPath, 'gen/doc'
        assertEquals project.sourceSets.multi.wsdoc[1].targetPath, 'gen/doc2'
        assertEquals project.tasks.genMultiWsdocGroup1.destinationDir, project.file('gen/doc')
        assertEquals project.tasks.genMultiWsdocGroup2.destinationDir, project.file('gen/doc2')
    }



    @Test
    void canCustomizeInclude() {
        //forks a new java project in a temp folder
        final ProjectHelper projectHelper = GradleProjectBuilder.builder().build {
            apply plugin: 'sktools-wsdocgen-plugin'

            sourceSets {
                main.wsdoc.group { include '**/TestServiceWSBean.java' }
            }
        }

        final Project project = projectHelper.project
        assertEquals project.sourceSets.main.wsdoc[0].includes, ['**/TestServiceWSBean.java']
        assertEquals project.tasks.genMainWsdocGroup1.includes.size(), 1
        assertTrue project.tasks.genMainWsdocGroup1.includes.contains('**/TestServiceWSBean.java')
    }


    @Test
    void generatedFilesHasLookupPathWhenConfigured() {

        writeFile("build.gradle", """
            plugins {
              id 'sktools-wsdocgen-plugin'
            }

            sourceSets {
                main.wsdoc.group {
                    targetPath 'build/mydocs'
                    lookupPath '../wacky/path'
                }
            }
        """)

        //generer eksempel-kildekode som har domene-klasse definert
        writeInterfaceServiceWSBean(file('src/main/java'))

        assertNoFailures(testGradleBuild(WsDocGenPlugin.GEN_TASK_NAME))

        assertThat(file('build/mydocs/InterfaceService.html')).exists()
        assertThat(contentOf(file('build/mydocs/InterfaceService.html')))
            .describedAs("skal ha link som peker til domeneklasse (javadoc)")
            .contains("../wacky/path")
    }


    /**
     * Demonstrerer hvordan en kan spre kilekode over flere mapper
     */
    @Test
    void multipleSourceFoldersForSingleSourceSet() {

        //forks a new java project in a temp folder
        //ps: notice that the java plugin is applied after the plugin, at a  later stage.
        writeFile("build.gradle", """
            plugins {
              id 'sktools-wsdocgen-plugin'
              id 'java' //after
            }

            sourceSets.main {
                java.srcDir 'src/main/morejava'
                wsdoc.group {
                    targetPath 'build'
                }
            }
        """)

        //generer eksempel-kildekode som har domene-klasse definert
        writeSimpleDemoServiceWSBean(file("src/main/java"))
        writeInterfaceServiceWSBean(file('src/main/morejava'))


        assertNoFailures(testGradleBuild(WsDocGenPlugin.GEN_TASK_NAME))

        assertThat(file('build/InterfaceService.html')).exists()
        assertThat(file('build/TestService.html')).exists()
    }


}