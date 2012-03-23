package no.statkart.sktools.gradle.plugins.weblogic.wsclient

import no.statkart.sktools.gradle.testutils.ProjectHelper
import org.testng.annotations.Test
import org.gradle.api.Project
import no.statkart.sktools.gradle.testutils.builder.WeblogicWsWarProjectBuilder
import no.statkart.sktools.gradle.plugins.weblogic.wswar.WeblogicWsWarPlugin

import org.apache.commons.io.FileUtils
import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import org.gradle.api.file.FileCollection

import org.gradle.api.tasks.util.PatternSet
import no.statkart.sktools.gradle.testutils.filewriter.WeblogicWsWarTestutilFilewriter
import org.gradle.api.internal.file.UnionFileCollection

/**
 * Tester implementasjon av {@link WeblogicJaxWsClientCompiler}
 * @author Leif Lislegård
 */
class WeblogicJaxWsClientCompilerTest {

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
        ProjectHelper rootProjectHelper = GradleProjectBuilder.builder("rootProject").build()
        Project rootProject = rootProjectHelper.project

        //forks a new project in a temp folder
        ProjectHelper wsWarProjectHelper = WeblogicWsWarProjectBuilder.builder().applyWsWarPlugin(true).withName("wswar").withParent(rootProject).build()

        //oppretter to servicer
        use (WeblogicWsWarTestutilFilewriter) {
            wsWarProjectHelper.writeDemoServiceWSBean2('src/weblogic/java')
            wsWarProjectHelper.writePingServiceWSBean('src/weblogic/java')
        }

        wsWarProjectHelper.executeTask(WeblogicWsWarPlugin.COMPILE_WEBLOGIC_TASK_NAME)
        wsWarProjectHelper.assertTaskExecutedNotSkipped(WeblogicWsWarPlugin.COMPILE_WEBLOGIC_TASK_NAME)

        rootProject.file('somedir').mkdir()
        FileUtils.copyFileToDirectory(wsWarProjectHelper.assertFileExists('build/classes/weblogic/wswar.war/WEB-INF/TestServiceWS.wsdl'), rootProject.file('somedir'))
        FileUtils.copyFileToDirectory(wsWarProjectHelper.assertFileExists('build/classes/weblogic/wswar.war/WEB-INF/TestServiceWS_schema1.xsd'), rootProject.file('somedir'))

        FileCollection someDirFiles = rootProject.files('somedir')
        FileCollection additionalFiles = rootProject.files(
                wsWarProjectHelper.assertFileExists('build/classes/weblogic/wswar.war/WEB-INF/PingServiceWS.wsdl'),
                wsWarProjectHelper.assertFileExists('build/classes/weblogic/wswar.war/WEB-INF/PingServiceWS_schema1.xsd'),
        )

        //konfigurerer compiler
        WeblogicJaxWsClientCompiler compiler = new WeblogicJaxWsClientCompiler()
        compiler.ant = rootProject.createAntBuilder()
        compiler.setWeblogicClasspath(wsWarProjectHelper.project.configurations.getByName('weblogic'))
        compiler.setDestinationDir(rootProject.buildDir)
        compiler.source = rootProject.files('somedir')

        compiler.webServices = [new WebServiceConfig(null)].each { WebServiceConfig webServiceConfig ->
            webServiceConfig.schemaFiles = new UnionFileCollection(someDirFiles, additionalFiles)

        }

        //eksekverer
        compiler.execute()

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
        ProjectHelper rootProjectHelper = GradleProjectBuilder.builder("rootProject").build()
        Project rootProject = rootProjectHelper.project

        //forks a new project in a temp folder
        ProjectHelper wsWarProjectHelper = WeblogicWsWarProjectBuilder.builder().applyWsWarPlugin(true).withName("wswar").withParent(rootProject).applyJavaPlugin().build()
        wsWarProjectHelper.configureProject() {
            dependencies {
                weblogic project(path: project.path, configuration: 'runtime')    //
            }
        }

        //oppretter servicer
        use (WeblogicWsWarTestutilFilewriter) {
            wsWarProjectHelper.writeExceptionDemoWithTwoServicesDomain('src/main/java')
            wsWarProjectHelper.writeExceptionDemoWithTwoServicesService('src/weblogic/java')
        }

        wsWarProjectHelper.executeTask(WeblogicWsWarPlugin.COMPILE_WEBLOGIC_TASK_NAME)
        wsWarProjectHelper.assertTaskExecutedNotSkipped(WeblogicWsWarPlugin.COMPILE_WEBLOGIC_TASK_NAME)
        wsWarProjectHelper.assertFileExists('build/classes/weblogic/wswar.war/WEB-INF/ExceptionService1WS.wsdl')

        FileCollection schemaFiles = rootProject.files('wswar/build/classes/weblogic/wswar.war/WEB-INF').getAsFileTree().matching(new PatternSet(includes: ['*.wsdl', '*.xsd']))

        //konfigurerer compiler
        WeblogicJaxWsClientCompiler compiler = new WeblogicJaxWsClientCompiler()
        compiler.ant = rootProject.createAntBuilder()
        compiler.setWeblogicClasspath(wsWarProjectHelper.project.configurations.getByName('weblogic'))
        compiler.setDestinationDir(rootProject.buildDir)
        compiler.source = schemaFiles
        compiler.webServices = [new WebServiceConfig(null)].each { WebServiceConfig webServiceConfig ->
                webServiceConfig.schemaFiles = schemaFiles
                webServiceConfig.exceptionReusePackage('no.statkart.test.exceptiondemo01.common')
        }

        //eksekverer
        compiler.execute()



        //tester at enkelte filer er generert
        rootProjectHelper.assertFileExists('build/no/statkart/test/exceptiondemo01/service/service1/ExceptionService1.java')
        rootProjectHelper.assertFileExists('build/no/statkart/test/exceptiondemo01/displaced/service/service2/ExceptionService2.java')

        rootProjectHelper.assertFileExists('build/no/statkart/test/exceptiondemo01/common/ServiceException.java')

    }






}
