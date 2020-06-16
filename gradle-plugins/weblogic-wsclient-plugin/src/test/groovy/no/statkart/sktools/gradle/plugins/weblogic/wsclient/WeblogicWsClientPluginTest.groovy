package no.statkart.sktools.gradle.plugins.weblogic.wsclient


import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.TestKitBase
import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.Test
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.Task

/**
 * Test  av {@link WeblogicWsClientPlugin}-funksjonalitet.
 *
 * @author Leif Lislegård
 */
class WeblogicWsClientPluginTest extends TestKitBase {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testApplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-weblogic-wsclient-plugin'


        assert project.convention.plugins.weblogicWsClient != null
        assert project.convention.plugins.weblogicWsClient instanceof WeblogicWsClientConvention

    }

    /**
     * Tester plugin via alternativt navn
     */
    @Test
    void testApplyPlugin2() {
        writeFileUTF8("build.gradle", '''\
            plugins {
                id 'sktools.weblogic-wsclient'
            }
        ''')

        assertNoFailures(testGradleBuild("tasks"))
    }

    /**
     *
     * Tester og demonstrerer ulike måter å konfigurere opp pluginen på.
     */
    @Test
    void testConventionConfiguration() {
        //forks a new project in a temp folder
        ProjectHelper wsClientProjectHelper = GradleProjectBuilder.builder().build {
            apply plugin: 'sktools-weblogic-wsclient-plugin'

            weblogicWsClient {
                example1 {
                    baseWar { 'org.organisation:someproject:1.1' }
                }
                example2 {
                    baseWar 'org.organisation:someproject:1.1'
                    exceptionReusePackage 'reduce.to.this.pkg'
                }
                example3 {
                    baseWar "org.organisation:someproject:1.1"
                    schemaFiles project.files('some.wsdl', 'some.xsd')
                }
                some1 {
                    schemaFiles 'some.wsdl', 'some.xsd'
                }
                some2 {
                    schemaFiles {
                        project.files('some.wsdl', 'some.xsd')
                    }
                }
            }
        }

        wsClientProjectHelper.initializeProject()

        def convention = wsClientProjectHelper.project.convention.plugins.get(WeblogicWsClientPlugin.CONVENTION_NAME) as WeblogicWsClientConvention

        assert convention != null
        assert convention.genDir == 'gen/main/wsclient'
        assert convention.webService['example2'].exception.packageOrPathString != null

        //tester baseWar
        ['example1','example2','example3'].each {
            def webService = convention.webService[it]
            def dependencies = webService.baseWars.dependencies
            assert dependencies.size() == 1
            def dependency = dependencies.iterator().next()
            assert dependency instanceof org.gradle.api.artifacts.Dependency
            assert dependency.version == '1.1'
            assert dependency.name == 'someproject'
            assert dependency.group == 'org.organisation'
        }

        //tester schema files
        Set<File> someFiles = wsClientProjectHelper.project.files('some.wsdl', 'some.xsd').files
        ['example3', 'some1', 'some2'].each {
            assert convention.webService[it] != null
            assert convention.webService[it].schemaFiles.files.containsAll(someFiles)   //forventer at filer finnes
        }

    }

//    /**
//     * Tester generering av wsclient der man peker til war modul i et annet gradle prosjekt.
//     * Benytter her JavaPlugin oppsett.
//     */
//    @Test
//    void testDependency() {
//        //forks a new wsClientProject in a temp folder
//        ProjectHelper wsClientProjectHelper = GradleProjectBuilder.builder('wsclient').withConventionalWEBLOGIC().build {
//            apply plugin: 'java'
//            apply plugin: 'sktools-weblogic-wsclient-plugin'
//        }
//
//        //forks a child wsClientProject within the same temp folder
//        ProjectHelper wsWarProjectHelper = GradleProjectBuilder.builder("wswar").withConventionalWEBLOGIC().withParent(wsClientProjectHelper).build {
//            apply plugin: 'sktools-weblogic-wswar-plugin'
//        }
//
//        //oppretter to servicer
//        use(WeblogicWsWarTestutilFilewriter) {
//            wsWarProjectHelper.writeDemoServiceWSBean2('src/weblogic/java')
//            wsWarProjectHelper.writePingServiceWSBean('src/weblogic/java')
//        }
//
//        //konfigurerer prosjekt
//        wsClientProjectHelper.configureProject {
//            weblogicWsClient {
//                webService {
//                    baseWar { project([path: ':wswar', configuration: 'weblogic']) }
//                }
//            }
//        }
//
//        wsClientProjectHelper.initializeProject()
//
//
//        //eksekverer - genererer wsclient artifakt
//        wsClientProjectHelper.executeTask('assemble')
//        //forventer at ovenstående kaller {@code WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME }
//
//        //tester at avhengighet er blit bygd.
//        wsWarProjectHelper.assertFileExists('build/libs/wswar-weblogic.war', 'Forventer at war modul er blitt generert og pakket.')
//
//        //tester at tasker er blitt eksekvert.
//        wsWarProjectHelper.assertTaskExecutedNotSkipped(WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME)
//
//        wsClientProjectHelper.assertTaskExecutedNotSkipped(WeblogicWsClientPlugin.GEN_CLIENT_TASK_NAME)
//        assert wsClientProjectHelper.project.tasks['processResources'].state.executed
//        assert wsClientProjectHelper.project.tasks['compileJava'].state.executed
//
//
//        //tester sourceSet
//        SourceSet sourceSet = wsClientProjectHelper.project.sourceSets.main;
//
//        //tester sourceSet.output
//        assert sourceSet.output.asFileTree.files.find { it.toURI().path.endsWith('/no/statkart/test/service/demotns/TestServiceWS.class')}
//        assert sourceSet.output.asFileTree.files.find { it.toURI().path.endsWith('/META-INF/wsdls/TestServiceWS.wsdl')}
//
//        //tester sourceSet.source
//        assert sourceSet.allSource.asFileTree.files.find { it.toURI().path.endsWith('/META-INF/wsdls/TestServiceWS.wsdl')}
//        assert sourceSet.allSource.asFileTree.files.find { it.toURI().path.endsWith('/META-INF/wsdls/PingServiceWS_schema1.xsd')}
//        assert sourceSet.allSource.asFileTree.files.find { it.toURI().path.endsWith('/no/statkart/test/service/demotns/TestServiceWS.java')}
//
//
//        //tester artifakt
//        wsClientProjectHelper.assertFileExists('build/libs/wsclient.jar') { File archiveFile ->
//            FileTree archiveFileTree = project.zipTree(archiveFile)
//            assert archiveFileTree.files.find { it.toURI().path.endsWith('/META-INF/wsdls/TestServiceWS.wsdl')}
//            assert archiveFileTree.files.find { it.toURI().path.endsWith('/no/statkart/test/service/demotns/TestServiceWS.class')}
//            assert !archiveFileTree.files.find { it.toURI().path.endsWith('/no/statkart/test/service/demotns/TestServiceWS.java')}
//
//            assert archiveFileTree.files.find { it.toURI().path.endsWith('/META-INF/wsdls/PingServiceWS_schema1.xsd')}
//        }
//
//
//    }

    /**
     * Verifiserer at build task er avhengig av forventede tasker.
     */
    @Test()
    void testBuildTaskDependsOn() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = GradleProjectBuilder.builder().build {
            apply plugin: 'java'
            apply plugin: 'sktools-weblogic-wsclient-plugin'
        }

        projectHelper.initializeProject(false)

        List<String> taskDependencies = projectHelper.findDependsOnTaskNames('build')

        assert taskDependencies.contains('build')
        assert taskDependencies.contains(WeblogicWsClientPlugin.GEN_CLIENT_TASK_NAME)
        assert taskDependencies.contains('processResources')
        assert taskDependencies.contains('compileJava')
        assert taskDependencies.contains(BasePlugin.ASSEMBLE_TASK_NAME)

    }

    /**
     * Tester weblogicClasspath
     */
    @Test
    void testWeblogicClasspath() {

        //forks a new project in a temp folder
        ProjectHelper projectHelper = GradleProjectBuilder.builder().build {
            apply plugin: 'sktools-weblogic-wsclient-plugin'
        }

        File someJarFile = projectHelper.project.file('weblogic.jar')

        projectHelper.configureProject {
            dependencies {
                weblogicProvided files(someJarFile)
            }

            weblogicWsClient {
                webService {
                }
            }
        }


        Task task = projectHelper.project.tasks.getByName('genWebServiceWsClientSource')

        assert task.weblogicClasspath.files.contains(someJarFile)


    }


}
