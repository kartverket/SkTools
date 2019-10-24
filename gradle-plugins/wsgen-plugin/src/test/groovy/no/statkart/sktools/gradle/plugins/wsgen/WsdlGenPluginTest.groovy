package no.statkart.sktools.gradle.plugins.wsgen

import no.statkart.sktools.gradle.testutils.TestKitBase
import org.assertj.core.api.Condition
import org.gradle.testkit.runner.BuildResult
import org.testng.annotations.Test

import java.nio.file.Files
import java.util.zip.ZipFile

import static java.util.Collections.list
import static org.assertj.core.api.Assertions.assertThat

class WsdlGenPluginTest extends TestKitBase {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void applyPlugin() {

        writeFile("build.gradle", """
            plugins {
              id 'sktools-wsgen-plugin'
            }
        """)

        assertNoFailures(testGradleBuild("classes"))
    }


    @Test
    void genWsdl_generates_sources() {
        writeFile("build.gradle", """
            plugins {
              id 'sktools-wsgen-plugin'
            }
            
            repositories {
                maven { url = '${testProperties.MAVEN_REPO}' }
            }
        """)

        writeExceptiondemo01()

        BuildResult buildResult = testGradleBuild("genWsdl")

        assertThat(file('build/genWsdls/ExceptionService1WS.wsdl')).exists()
        assertThat(file('build/genWsdls/ExceptionService1WS_schema1.xsd')).exists()
        assertThat(file('build/genWsdls/ExceptionService1WS_schema2.xsd')).exists()
    }


    @Test
    void warFileIncludesResources() {
        writeFile("build.gradle", """
            plugins {
              id 'sktools-wsgen-plugin'
            }
            
            repositories {
                maven { url = '${testProperties.MAVEN_REPO}' }
            }
        """)

        writeExceptiondemo01()
        writeFile('src/main/java/WebConfig.java', "public class WebConfig {} //dummy class")

        BuildResult buildResult = testGradleBuild("war")

        File file = file("build/libs/${rootProjectName()}.war")
        assertThat(file).exists()

        def NO_META_INF_FILES = new Condition<String>() {
            @Override
            boolean matches(String name) {
                return !name.startsWith('META-INF/')
            }
        }
        def NO_FOLDERS = new Condition<String>() {
            @Override
            boolean matches(String name) {
                return !name.endsWith('/')
            }
        }

        ZipFile war = new ZipFile(file)
        try {
            assertThat(list(war.entries()))
                .extractingResultOf("getName")
                .filteredOn(NO_META_INF_FILES)
                .filteredOn(NO_FOLDERS)
                .as("Contents of war file (ignoring META-INF)")
                .containsOnly(
                    'WEB-INF/classes/WebConfig.class',
                    'WEB-INF/classes/exceptiondemo01/ExceptionService1WSBean.class',
                    'WEB-INF/classes/exceptiondemo01/ExceptionService2WSBean.class',
                    'WEB-INF/classes/exceptiondemo01/exception/ServiceException.class',
                    'WEB-INF/classes/exceptiondemo01/exception/ServiceFaultInfo.class',
                )
        } finally {
            war.close()
        }
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

        writeFile('source/main/webapp/WEB-INF/web.xml', '''<?xml version='1.0' encoding='UTF-8'?>
<web-app xmlns="http://java.sun.com/xml/ns/javaee" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="3.0">
    <display-name>dummy</display-name>
</web-app>
''')
    }
}
