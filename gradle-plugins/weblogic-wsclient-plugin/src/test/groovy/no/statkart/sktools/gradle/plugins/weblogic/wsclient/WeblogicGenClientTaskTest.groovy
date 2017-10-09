package no.statkart.sktools.gradle.plugins.weblogic.wsclient

import no.statkart.sktools.gradle.plugins.weblogic.WeblogicBasePlugin
import no.statkart.sktools.gradle.plugins.weblogic.wsclient.internal.WsClientGenerator
import no.statkart.sktools.gradle.plugins.weblogic.wswar.WeblogicWsWarPlugin
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import no.statkart.sktools.gradle.testutils.builder.WeblogicWsWarProjectBuilder
import no.statkart.sktools.gradle.testutils.filewriter.WeblogicWsWarTestutilFilewriter
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.util.PatternSet
import org.testng.Assert
import org.testng.annotations.Test

/**
 * @author Leif Lislegård
 */
class WeblogicGenClientTaskTest {

    /**
     * Tester compiler med standard verdier.
     *
     * Input blir generert og hentet ifra et WsWar prosjekt.
     *
     * Til sist blir en kompiler instans opprettet og konfigurert, deretter eksekvert og testet.
     */
    @Test
    void testDefaults() {
        //forks a new project in a temp folder
        ProjectHelper rootProjectHelper = GradleProjectBuilder.builder("rootProject").withConventionalWEBLOGIC().build()
        Project rootProject = rootProjectHelper.project

        //forks a new project in a temp folder
        ProjectHelper wsWarProjectHelper = WeblogicWsWarProjectBuilder.builder().applyWsWarPlugin(true).withName("wswar").withParent(rootProject).build()

        //oppretter to servicer
        use(WeblogicWsWarTestutilFilewriter) {
            wsWarProjectHelper.writeDemoServiceWSBean2('src/weblogic/java')
            wsWarProjectHelper.writePingServiceWSBean('src/weblogic/java')
        }

        wsWarProjectHelper.executeTask(WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME)
        wsWarProjectHelper.assertTaskExecutedNotSkipped(WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME)

        // pakker ut schema filer ifra generert war
        assert wsWarProjectHelper.assertFileExists(wsWarProjectHelper.project.configurations['weblogic'].artifacts.files.singleFile) { warFile ->
            rootProject.copy {
                into 'somedir'
                from wsWarProjectHelper.project.zipTree(warFile).matching { include '**/TestService*.wsdl', '**/TestService*.xsd' }.files
            }
            rootProject.copy {
                into 'additional'
                from wsWarProjectHelper.project.zipTree(warFile).matching { include '**/PingService*.wsdl', '**/PingService*.xsd' }.files
            }
        }

        rootProjectHelper.assertFileExists('somedir/TestServiceWS.wsdl')
        rootProjectHelper.assertFileExists('somedir/TestServiceWS_schema1.xsd')
        rootProjectHelper.assertFileExists('additional/PingServiceWS.wsdl')
        rootProjectHelper.assertFileExists('additional/PingServiceWS_schema1.xsd')

        FileCollection someDirFiles = rootProject.files('somedir')
        FileCollection additionalFiles = rootProject.files('additional')

        //konfigurerer compiler
        WeblogicGenClientTask genClientTask = rootProject.tasks.create('genClient', WeblogicGenClientTask)
        genClientTask.webServiceConfig = createWebServiceConfigFor(rootProject)
        genClientTask.webServiceConfig.schemaFiles someDirFiles, additionalFiles
        genClientTask.setWeblogicClasspath(rootProject.files(
                WeblogicWsClientPlugin.conventionalWeblogicDependencies(rootProject),
                WeblogicBasePlugin.toolsJar(rootProject)))
        genClientTask.setDestinationDir(rootProject.buildDir)
        genClientTask.source = genClientTask.webServiceConfig.schemaFiles

        //eksekverer
        genClientTask.gen()

        //tester at enkelte filer er generert
        rootProjectHelper.assertFileExists('build/META-INF/wsdls/TestServiceWS.wsdl')
        rootProjectHelper.assertFileExists('build/no/statkart/test/service/demotns/TestServiceWS.class')
        rootProjectHelper.assertFileExists('build/no/statkart/test/service/demotns/TestServiceWS.java')

        rootProjectHelper.assertFileExists('build/META-INF/wsdls/PingServiceWS_schema1.xsd')

    }

    /**
     * Tester compiler mtp exception rewrite funksjonalitet.
     *
     * Input blir generert og hentet ifra et WsWar prosjekt hvor to servicer blir definert.
     *
     * Til sist blir en kompiler instans opprettet og konfigurert, deretter eksekvert og testet.
     */
    @Test
    void testExceptionRewrite() {
        //forks a new project in a temp folder
        ProjectHelper rootProjectHelper = GradleProjectBuilder.builder("rootProject").withConventionalWEBLOGIC().build()
        Project rootProject = rootProjectHelper.project

        //forks a new project in a temp folder
        ProjectHelper wsWarProjectHelper = WeblogicWsWarProjectBuilder.builder().applyWsWarPlugin(true).withName("wswar").withParent(rootProject).applyJavaPlugin().build()
        wsWarProjectHelper.configureProject() {
            dependencies {
                weblogicCompile project(path: project.path, configuration: 'runtime')    //
            }
        }

        //oppretter servicer
        use(WeblogicWsWarTestutilFilewriter) {
            wsWarProjectHelper.writeExceptionDemoWithTwoServicesDomain('src/main/java')
            wsWarProjectHelper.writeExceptionDemoWithTwoServicesService('src/weblogic/java')
        }

        wsWarProjectHelper.executeTask(WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME)
        wsWarProjectHelper.assertTaskExecutedNotSkipped(WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME)

        // pakker ut schema filer ifra generert war
        assert wsWarProjectHelper.assertFileExists(wsWarProjectHelper.project.configurations['weblogic'].artifacts.files.singleFile) { warFile ->
            rootProject.copy {
                into 'somedir'
                from wsWarProjectHelper.project.zipTree(warFile).matching { include '**/*.wsdl', '**/*.xsd' }.files
            }
        }
        rootProjectHelper.assertFileExists('somedir/ExceptionService1WS.wsdl')

        FileCollection schemaFiles = rootProject.fileTree('somedir').matching(new PatternSet(includes: ['*.wsdl']))

        //konfigurerer compiler
        WeblogicGenClientTask genClientTask = rootProject.tasks.create('genClient', WeblogicGenClientTask)
        genClientTask.webServiceConfig = createWebServiceConfigFor(rootProject)
        genClientTask.webServiceConfig.schemaFiles(schemaFiles)
        genClientTask.webServiceConfig.exceptionReusePackage('no.statkart.test.exceptiondemo01.common')

        genClientTask.setWeblogicClasspath(rootProject.files(
                WeblogicWsClientPlugin.conventionalWeblogicDependencies(rootProject),
                WeblogicBasePlugin.toolsJar(rootProject)))
        genClientTask.setDestinationDir(rootProject.file('src/main/java'))
        genClientTask.source = schemaFiles

        //eksekverer
        genClientTask.gen()

        //tester at enkelte filer er generert
        rootProjectHelper.assertFileExists('src/main/java/no/statkart/test/exceptiondemo01/service/service1/ExceptionService1.java')
        rootProjectHelper.assertFileExists('src/main/java/no/statkart/test/exceptiondemo01/displaced/service/service2/ExceptionService2.java')

        rootProjectHelper.assertFileExists('src/main/java/no/statkart/test/exceptiondemo01/common/ServiceException.java')


    }

    /**
     * Tester {@link WsClientGenerator#fixResourceLoaders()}
     */
    @Test
    void testResourceRewrite() {

        //forks a new project in a temp folder
        ProjectHelper rootProjectHelper = GradleProjectBuilder.builder().withConventionalWEBLOGIC().build()
        Project project = rootProjectHelper.getProject()
        def dir = project.mkdir('build')


        File targetFile = new File(dir, 'BorettInformasjonServiceWS.java')
        targetFile.append(this.class.getResourceAsStream('BorettInformasjonServiceWS.orig'))
        targetFile.deleteOnExit()


        WeblogicGenClientTask genClientTask = project.tasks.create('genClient', WeblogicGenClientTask)
        genClientTask.setDestinationDir(dir)

        final WsClientGenerator generator = genClientTask.createGenerator()
        generator.fixResourceLoaders()

        Assert.assertEquals(this.class.getResourceAsStream('BorettInformasjonServiceWS.result').text, targetFile.text)

    }


    /**
     * Tester compiler med standardverdier, bortsett fra at apiPrefix blir satt til "sktoolstest".
     *
     * Input blir generert og hentet ifra et WsWar prosjekt.
     *
     * Til sist blir en kompiler instans opprettet og konfigurert, deretter eksekvert og testet.
     */
    @Test
    void testApiPrefix() {
        //forks a new project in a temp folder
        ProjectHelper rootProjectHelper = GradleProjectBuilder.builder("rootProject").withConventionalWEBLOGIC().build()
        Project rootProject = rootProjectHelper.project

        //forks a new project in a temp folder
        ProjectHelper wsWarProjectHelper = WeblogicWsWarProjectBuilder.builder().applyWsWarPlugin(true).withName("wswar").withParent(rootProject).build()

        //oppretter to servicer
        use(WeblogicWsWarTestutilFilewriter) {
            wsWarProjectHelper.writeDemoServiceWSBean2('src/weblogic/java')
            wsWarProjectHelper.writePingServiceWSBean('src/weblogic/java')
        }

        wsWarProjectHelper.executeTask(WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME)
        wsWarProjectHelper.assertTaskExecutedNotSkipped(WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME)

        // pakker ut schema filer ifra generert war
        assert wsWarProjectHelper.assertFileExists(wsWarProjectHelper.project.configurations['weblogic'].artifacts.files.singleFile) { warFile ->
            rootProject.copy {
                into 'somedir'
                from wsWarProjectHelper.project.zipTree(warFile).matching { include '**/TestService*.wsdl', '**/TestService*.xsd' }.files
            }
            rootProject.copy {
                into 'additional'
                from wsWarProjectHelper.project.zipTree(warFile).matching { include '**/PingService*.wsdl', '**/PingService*.xsd' }.files
            }
        }

        rootProjectHelper.assertFileExists('somedir/TestServiceWS.wsdl')
        rootProjectHelper.assertFileExists('somedir/TestServiceWS_schema1.xsd')
        rootProjectHelper.assertFileExists('additional/PingServiceWS.wsdl')
        rootProjectHelper.assertFileExists('additional/PingServiceWS_schema1.xsd')

        FileCollection someDirFiles = rootProject.files('somedir')
        FileCollection additionalFiles = rootProject.files('additional')

        //konfigurerer compiler
        WeblogicGenClientTask genClientTask = rootProject.tasks.create('genClient', WeblogicGenClientTask)
        genClientTask.webServiceConfig = createWebServiceConfigFor(rootProject)
        genClientTask.webServiceConfig.schemaFiles someDirFiles, additionalFiles
        genClientTask.webServiceConfig.apiPrefix 'sktoolstest'
        genClientTask.setWeblogicClasspath(rootProject.files(
                WeblogicWsClientPlugin.conventionalWeblogicDependencies(rootProject),
                WeblogicBasePlugin.toolsJar(rootProject)))
        genClientTask.setDestinationDir(rootProject.buildDir)
        genClientTask.source = genClientTask.webServiceConfig.schemaFiles

        //eksekverer
        genClientTask.gen()

        //tester at enkelte filer er generert
        rootProjectHelper.assertFileExists('build/META-INF/wsdls/sktoolstest/TestServiceWS.wsdl')
        rootProjectHelper.assertFileExists('build/no/statkart/test/service/demotns/TestServiceWS.class')
        rootProjectHelper.assertFileExists('build/no/statkart/test/service/demotns/TestServiceWS.java')

        rootProjectHelper.assertFileExists('build/META-INF/wsdls/sktoolstest/PingServiceWS_schema1.xsd')

    }


    /**
     * Kun for testing
     */
    private static WebServiceConfig createWebServiceConfigFor(Project project) {
        return new WebServiceConfig(new WeblogicWsClientConvention(project), null)
    }

}
