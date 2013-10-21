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
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.SourceSet

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
    void testApplyPlugin() {
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

        def convention = wsClientProject.convention.plugins.get(WeblogicWsClientPlugin.CONVENTION_NAME) as WeblogicWsClientConvention

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
        Set<File> someFiles = wsClientProject.files('some.wsdl', 'some.xsd').files
        ['example3', 'some1', 'some2'].each {
            assert convention.webService[it] != null
            assert convention.webService[it].schemaFiles.files.containsAll(someFiles)   //forventer at filer finnes
        }

    }

    /**
     * Tester generering av wsclient der man peker til war modul i et annet gradle prosjekt.
     * Benytter her JavaPlugin oppsett.
     */
    @Test
    void testDependency() {
        //forks a new wsClientProject in a temp folder
        ProjectHelper wsClientProjectHelper = WeblogicWsClientProjectBuilder.builder().withName('wsclient').applyJavaPlugin().applyWsClientPlugin(true).build()
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


        //eksekverer - genererer wsclient artifakt
        wsClientProjectHelper.executeTask('assemble')
        //forventer at ovenstående kaller {@code WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME }

        //tester at avhengighet er blit bygd.
        wsWarProjectHelper.assertFileExists('build/libs/wswar-weblogic.war', 'Forventer at war modul er blitt generert og pakket.')

        //tester at tasker er blitt eksekvert.
        wsWarProjectHelper.assertTaskExecutedNotSkipped(WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME)

        wsClientProjectHelper.assertTaskExecutedNotSkipped(WeblogicWsClientPlugin.GEN_CLIENT_TASK_NAME)
        assert wsClientProjectHelper.project.tasks['processResources'].state.executed
        assert wsClientProjectHelper.project.tasks['compileJava'].state.executed
        assert wsClientProjectHelper.project.tasks['assemble'].state.executed


        //tester sourceSet
        SourceSet sourceSet = wsClientProject.sourceSets.main;

        //tester sourceSet.output
        assert sourceSet.output.asFileTree.files.find { it.toURI().path.endsWith('/no/statkart/test/service/demotns/TestServiceWS.class')}
        assert sourceSet.output.asFileTree.files.find { it.toURI().path.endsWith('/META-INF/wsdls/TestServiceWS.wsdl')}

        //tester sourceSet.source
        assert sourceSet.allSource.asFileTree.files.find { it.toURI().path.endsWith('/META-INF/wsdls/TestServiceWS.wsdl')}
        assert sourceSet.allSource.asFileTree.files.find { it.toURI().path.endsWith('/META-INF/wsdls/PingServiceWS_schema1.xsd')}
        assert sourceSet.allSource.asFileTree.files.find { it.toURI().path.endsWith('/no/statkart/test/service/demotns/TestServiceWS.java')}


        //tester artifakt
        wsClientProjectHelper.assertFileExists('build/libs/wsclient.jar') { File archiveFile ->
            FileTree archiveFileTree = project.zipTree(archiveFile)
            assert archiveFileTree.files.find { it.toURI().path.endsWith('/META-INF/wsdls/TestServiceWS.wsdl')}
            assert archiveFileTree.files.find { it.toURI().path.endsWith('/no/statkart/test/service/demotns/TestServiceWS.class')}
            assert !archiveFileTree.files.find { it.toURI().path.endsWith('/no/statkart/test/service/demotns/TestServiceWS.java')}

            assert archiveFileTree.files.find { it.toURI().path.endsWith('/META-INF/wsdls/PingServiceWS_schema1.xsd')}
        }


    }

    /**
     * Verifiserer at build task er avhengig av forventede tasker.
     */
    @Test()
    void testBuildTaskDependsOn() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = WeblogicWsClientProjectBuilder.builder().applyJavaPlugin().applyWsClientPlugin(false).build()

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
        ProjectHelper projectHelper = WeblogicWsClientProjectBuilder.builder().applyWsClientPlugin(false).build()

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
