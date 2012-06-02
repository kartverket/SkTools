package no.statkart.sktools.gradle.plugins.weblogic.wsclient

import no.statkart.sktools.gradle.plugins.weblogic.wswar.WeblogicWsWarPlugin
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.WeblogicWsClientProjectBuilder
import no.statkart.sktools.gradle.testutils.builder.WeblogicWsWarProjectBuilder
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import org.testng.annotations.Test
import org.gradle.api.plugins.BasePlugin
import no.statkart.sktools.gradle.testutils.filewriter.WeblogicWsWarTestutilFilewriter
import org.gradle.api.Task

/**
 * Test  av {@link WeblogicWsClientPlugin}-funksjonalitet.
 *
 * @author Leif Lislegård
 */
class WeblogicWsClientPluginTest {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testAppplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-weblogic-wsclient-plugin'


        assert project.convention.plugins.weblogicWsClient != null
        assert project.convention.plugins.weblogicWsClient instanceof WeblogicWsClientConvention

    }

    /**
     *
     * Tester og demonstrerer ulike måter å konfigurere opp pluginen på.
     */
    @Test
    void testConventionConfiguration() {
        //forks a new project in a temp folder
        ProjectHelper wsClientProjectHelper = WeblogicWsClientProjectBuilder.builder().applyWsClientPlugin(false).build()
        Project wsClientProject = wsClientProjectHelper.project


        //konfigurerer prosjekt
        wsClientProjectHelper.configureProject {
            weblogicWsClient {
                webService {
                    name 'example1'
                    baseWar { 'org.organisation:someproject:1.1' }
                }
                webService {
                    name 'example2'
                    baseWar 'org.organisation:someproject:1.1'
                    exceptionReusePackage 'reduce.to.this.pkg'
                }
                webService {
                    baseWar "org.organisation:someproject:1.1"
                    schemaFiles project.files('some.wsdl', 'some.xsd')
                }
                webService {
                    schemaFiles 'some.wsdl', 'some.xsd'
                }
                webService {
                    schemaFiles {
                        project.files('some.wsdl', 'some.xsd')
                    }
                }
            }
        }

        wsClientProjectHelper.initializeProject()

        def convention = wsClientProject.convention.plugins.get(WeblogicWsClientPlugin.CONVENTION_NAME)

        assert convention != null
        assert convention.genDir == wsClientProject.file('gen/weblogic/wsclient')
        assert convention.webService[1].exception.packageOrPathString != null

        //tester baseWar
        (0..2).each {
            assert convention.webService[it].baseWar instanceof org.gradle.api.artifacts.Dependency
            assert convention.webService[it].baseWar.version == '1.1'
            assert convention.webService[it].baseWar.name == 'someproject'
            assert convention.webService[it].baseWar.group == 'org.organisation'
        }

        //tester schema files
        Set<File> someFiles = wsClientProject.files('some.wsdl', 'some.xsd').files
        (2..4).each {
            assert convention.webService[it] != null
            assert convention.webService[it].schemaFiles.files.containsAll(someFiles)   //forventer at filer finnes
        }

        //tester name
        (0..1).each {
            assert convention.webService[it].name == 'example' + (it + 1)
        }


    }

    /**
     * Tester generering av wsclient der man peker til war modul i et annet gradle prosjekt.
     */
    @Test
    void testDependency() {
        //forks a new wsClientProject in a temp folder
        ProjectHelper wsClientProjectHelper = WeblogicWsClientProjectBuilder.builder().withName('wsclient').applyWsClientPlugin(true).build()
        Project wsClientProject = wsClientProjectHelper.project

        //forks a child wsClientProject within the same temp folder
        ProjectHelper wsWarProjectHelper = WeblogicWsWarProjectBuilder.builder().withName('wswar').withParent(wsClientProjectHelper).applyWsWarPlugin(true).build()

        //oppretter to servicer
        use(WeblogicWsWarTestutilFilewriter) {
            wsWarProjectHelper.writeDemoServiceWSBean2('src/weblogic/java')
            wsWarProjectHelper.writePingServiceWSBean('src/weblogic/java')
        }

        //konfigurerer prosjekt
        wsClientProjectHelper.configureProject {
            weblogicWsClient {
                webService {
                    baseWar { project([path: ':wswar', configuration: 'weblogic']) }
                }
            }
        }

        wsClientProjectHelper.initializeProject()

        //eksekverer

        //genererer wsclient artifakt
        wsClientProjectHelper.executeTask(WeblogicWsClientPlugin.WEBLOGIC_JAR_TASK_NAME)
        //forventer at ovenstående kaller {@code WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME }

        //tester at avhengighet er blit bygd.
        wsWarProjectHelper.assertFileExists('build/libs/wswar-weblogic.war', 'Forventer at war modul er blitt generert og pakket.')

        //tester at tasker er blitt eksekvert.
        wsWarProjectHelper.assertTaskExecutedNotSkipped(WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME)

        wsClientProjectHelper.assertTaskExecutedNotSkipped(WeblogicWsClientPlugin.GEN_CLIENT_TASK_NAME)
        wsClientProjectHelper.assertTaskExecutedNotSkipped(WeblogicWsClientPlugin.PROCESS_WEBLOGIC_RESOURCES_TASK_NAME)
        wsClientProjectHelper.assertTaskExecutedNotSkipped(WeblogicWsClientPlugin.COMPILE_WEBLOGIC_TASK_NAME)
        wsClientProjectHelper.assertTaskExecutedNotSkipped(WeblogicWsClientPlugin.JAR_WEBLOGIC_TASK_NAME)

        //tester at enkelte filer er generert
        wsClientProjectHelper.assertFileExists('gen/weblogic/wsclient/META-INF/wsdls/TestServiceWS.wsdl')
        wsClientProjectHelper.assertFileExists('gen/weblogic/wsclient/no/statkart/test/service/demotns/TestServiceWS.class')
        wsClientProjectHelper.assertFileExists('gen/weblogic/wsclient/no/statkart/test/service/demotns/TestServiceWS.java')

        wsClientProjectHelper.assertFileExists('gen/weblogic/wsclient/META-INF/wsdls/PingServiceWS_schema1.xsd')
    }

    /**
     * Verifiserer at build task er avhengig av forventede tasker.
     */
    @Test()
    void testBuildTaskDependsOn() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = WeblogicWsClientProjectBuilder.builder().applyWsClientPlugin(false).build()

        List<String> taskDependencies = projectHelper.findDependsOnTaskNames('build')

        assert taskDependencies.contains('build')
        assert taskDependencies.contains(WeblogicWsClientPlugin.GEN_CLIENT_TASK_NAME)
        assert taskDependencies.contains(WeblogicWsClientPlugin.PROCESS_WEBLOGIC_RESOURCES_TASK_NAME)
        assert taskDependencies.contains(WeblogicWsClientPlugin.COMPILE_WEBLOGIC_TASK_NAME)
        assert taskDependencies.contains(BasePlugin.ASSEMBLE_TASK_NAME)

    }

    /**
     * Tester weblogicClasspath
     */
    @Test
    void testWeblogicClasspath() {

        //forks a new project in a temp folder
        ProjectHelper projectHelper = WeblogicWsClientProjectBuilder.builder().applyWsClientPlugin(false).build()

        File someJarFile = projectHelper.project.file('weblogic.jar')

        projectHelper.configureProject {
            dependencies {
                weblogic files(someJarFile)
            }
        }


        Task task = projectHelper.project.tasks.getByName(WeblogicWsClientPlugin.GEN_CLIENT_TASK_NAME)

        assert task.weblogicClasspath.files.contains(someJarFile)



    }



    /**
     * Tester at man kan legge til ekstra java klasser
     */
    @Test(enabled = false)
    void testSourceSetJava() {
        Assert.fail('todo')
    }

    /**
     * Tester at man kan legge til ekstra ressursfiler
     */
    @Test(enabled = false)
    void testSourceSetResources() {
        Assert.fail('todo')
    }



}
