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
                mavenCentral()
            }

            dependencies {
                implementation platform('com.sun.xml.ws:jaxws-ri-bom:3.0.2') //Jakarta EE9.1
                implementation 'jakarta.xml.ws:jakarta.xml.ws-api'
                implementation 'jakarta.jws:jakarta.jws-api'
            }
        """)

        writeExceptiondemo()

        BuildResult buildResult = testGradleBuild("wsdlGen")

        assertThat(file('build/wsdlGen/ExceptionService1WS.wsdl')).exists()
        assertThat(file('build/wsdlGen/ExceptionService1WS_schema1.xsd')).exists()
        assertThat(file('build/wsdlGen/ExceptionService1WS_schema2.xsd')).exists()
    }

    private void writeExceptiondemo() {
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
<web-app
    xmlns="https://jakarta.ee/xml/ns/jakartaee"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd"
    version="5.0">
    <display-name>Jakarta EE9</display-name>
</web-app>
        ''')
    }
}
