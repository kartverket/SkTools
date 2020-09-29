package no.statkart.sktools.gradle.plugins.wsdlgen

import no.statkart.sktools.gradle.testutils.TestKitBase
import org.gradle.api.Project
import org.gradle.testkit.runner.BuildResult
import org.testng.annotations.Test

import java.nio.file.Files

import static org.assertj.core.api.Assertions.assertThat

class WsdlGenPluginTest extends TestKitBase {
    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void applyPlugin() {
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools.wsdlgen'
        }

        assertThat(project.getPlugins().getPlugin(WsdlGenPlugin.class)).isNotNull()
    }


    @Test
    void genWsdl_generates_sources() {
        writeFileUTF8("build.gradle", """\
            plugins {
              id 'sktools.wsdlgen'
            }

            repositories {
                maven { url = '${testProperties.MAVEN_REPO}' }
            }

            dependencies {
                implementation group: 'jakarta.xml.ws', name: 'jakarta.xml.ws-api', version: '2.3.3'
                implementation group: 'jakarta.jws', name: 'jakarta.jws-api', version: '1.1.1'
            }
        """)

        writeExceptiondemo01()

        BuildResult buildResult = testGradleBuild("wsdlGen")

        assertThat(file('build/wsdlGen/ExceptionService1WS.wsdl')).exists()
        assertThat(file('build/wsdlGen/ExceptionService1WS_schema1.xsd')).exists()
        assertThat(file('build/wsdlGen/ExceptionService1WS_schema2.xsd')).exists()
    }

    private void writeExceptiondemo01() {
        def javaSrc = 'src/main/java'
        [
            '/exceptiondemo01/exception/ServiceException.java',
            '/exceptiondemo01/exception/ServiceFaultInfo.java',
            '/exceptiondemo01/ExceptionService1WSBean.java',
            '/exceptiondemo01/ExceptionService2WSBean.java',
        ].each {
            File destinationFile = file("$javaSrc/$it")
            destinationFile.getParentFile().mkdirs()
            def testResources = Objects.requireNonNull(getClass().getResourceAsStream(it), "Missing testResource: " + it)
            Files.copy(testResources, destinationFile.toPath())
        }

        writeFileUTF8('src/main/webapp/WEB-INF/web.xml', '''<?xml version='1.0' encoding='UTF-8'?>
<web-app xmlns="http://java.sun.com/xml/ns/javaee" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="3.0">
    <display-name>dummy</display-name>
</web-app>
        ''')
    }
}
