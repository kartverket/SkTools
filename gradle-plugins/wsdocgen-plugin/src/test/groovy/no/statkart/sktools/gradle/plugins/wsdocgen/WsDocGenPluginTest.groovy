package no.statkart.sktools.gradle.plugins.wsdocgen

import org.testng.annotations.Test
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import no.statkart.sktools.gradle.testutils.builder.WsDocGenProjectBuilder
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.filewriter.WsDocgenTestutilFilewriter

import static org.testng.Assert.assertEquals
import static org.testng.Assert.assertNotNull
import static org.testng.Assert.assertNull
import static org.testng.Assert.assertTrue

/**
 * Test av {@link WsDocGenPlugin}
 *
 * <p>
 *     For testing av generering av dokumentasjon se {@link no.statkart.sktools.utils.wsdocgen.processor.WSDocProcessorTest}
 * </p>
 *
 * @author Leif Lislegård
 */
class WsDocGenPluginTest {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void appplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-wsdocgen-plugin'


        assertNotNull project.convention.plugins.wsdoc
        assertTrue project.convention.plugins.wsdoc instanceof WsDocGenConvention
    }


    @Test
    void genWsDocGeneratesDocumentationForAllSourceSets() {
        //forks a new java project in a temp folder
        ProjectHelper projectHelper = WsDocGenProjectBuilder.builder().applyJavaPlugin().applyWsDocGenPlugin().build()


        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeSimpleDemoServiceWSBean("src/main/java") //generates simple source file
            projectHelper.writeSimpleDemoServiceWSBean("src/other/java") //generates simple source file
        }

        projectHelper.configureProject {
            sourceSets {
                main.wsdoc.group { }
                other.wsdoc.group { }
            }
        }

        projectHelper.executeTask(WsDocGenPlugin.GEN_TASK_NAME)

        projectHelper.assertFileExistsInBuildDir('main/wsdoc/Group1/TestService.html')
        projectHelper.assertFileExistsInBuildDir('other/wsdoc/Group1/TestService.html')
    }


    @Test
    void tasksForVanillaConfiguration() {
        //forks a new java project in a temp folder
        ProjectHelper projectHelper = WsDocGenProjectBuilder.builder().applyJavaPlugin().applyWsDocGenPlugin().build()

        projectHelper.configureProject {
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
        ProjectHelper projectHelper = WsDocGenProjectBuilder.builder().applyJavaPlugin().applyWsDocGenPlugin().build()

        projectHelper.configureProject {
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
        ProjectHelper projectHelper = WsDocGenProjectBuilder.builder().applyJavaPlugin().applyWsDocGenPlugin().build()

        projectHelper.configureProject {
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
        ProjectHelper projectHelper = WsDocGenProjectBuilder.builder().applyWsDocGenPlugin().build()

        projectHelper.configureProject {
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
    void canCustomizeLookupPath() {
        //forks a new java project in a temp folder
        ProjectHelper projectHelper = WsDocGenProjectBuilder.builder().applyWsDocGenPlugin().build()

        projectHelper.configureProject {
            sourceSets {
                main.wsdoc.group { lookupPath '../../some/wacky/path' }
            }
        }

        final Project project = projectHelper.project
        assertEquals project.sourceSets.main.wsdoc[0].lookupPath, '../../some/wacky/path'
        assertEquals project.tasks.genMainWsdocGroup1.lookupPath, '../../some/wacky/path'
    }


    @Test
    void canCustomizeInclude() {
        //forks a new java project in a temp folder
        ProjectHelper projectHelper = WsDocGenProjectBuilder.builder().applyWsDocGenPlugin().build()

        projectHelper.configureProject {
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
    void generatedFilesHasLookupPath() {
        ProjectHelper projectHelper = WsDocGenProjectBuilder.builder().applyWsDocGenPlugin().build()

        //generer eksempel-kildekode som har domene-klasse definert
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeInterfaceServiceWSBean('src/main/java')
        }


        projectHelper.configureProject {
            sourceSets {
                main.wsdoc.group {
                    targetPath 'build/mydocs'
                    lookupPath '../wacky/path'
                }
            }
        }

        projectHelper.executeTask(WsDocGenPlugin.GEN_TASK_NAME)

        projectHelper.assertFileExists('build/mydocs/InterfaceService.html') { File file ->
            assertTrue file.text.contains("../wacky/path") //skal ha link som peker til domeneklasse (javadoc)
        }
    }


    /**
     * Demonstrerer hvordan en kan spre kilekode over flere mapper
     */
    @Test
    void multipleSourceFoldersForSourceSet() {

        //forks a new java project in a temp folder
        //ps: notice that the java plugin is applied after the plugin, at a  later stage.
        ProjectHelper projectHelper = WsDocGenProjectBuilder.builder().applyWsDocGenPlugin().applyJavaPlugin().build()


        //generer eksempel-kildekode som har domene-klasse definert
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeSimpleDemoServiceWSBean('src/main/java')
            projectHelper.writeInterfaceServiceWSBean('src/main/morejava')
        }


        projectHelper.configureProject {
            sourceSets.main {
                java.srcDir 'src/main/morejava'
                wsdoc.group {
                    targetPath 'build'
                }
            }
        }
        projectHelper.initializeProject()


        projectHelper.executeTask(WsDocGenPlugin.GEN_TASK_NAME)

        projectHelper.assertFileExists('build/InterfaceService.html')
        projectHelper.assertFileExists('build/TestService.html')

    }


}