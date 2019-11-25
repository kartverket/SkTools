package no.statkart.sktools.gradle.plugins.wsimport


import no.statkart.sktools.gradle.testutils.TestKitBase
import org.gradle.api.Project
import org.testng.annotations.Test

import java.nio.file.Files
import java.util.jar.JarFile

import static java.util.Collections.list
import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.contentOf

class WsImportPluginTest extends TestKitBase {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void applyPlugin() {
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-wsimport-plugin'
        }

        assertThat(project.getPlugins().getPlugin(WsImportPlugin.class)).isNotNull()
        assertThat(project.plugins.hasPlugin('sktools-wsimport-plugin')).isTrue()
    }

    @Test
    void ideaIntegration() {

        writeFile("build.gradle", """
            plugins {
              id 'sktools.wsimport'
              id 'idea'
            }
        """)

        testGradleBuild("ideaModule")

        assertThat(contentOf(file(rootProjectName() + ".iml")))
            .contains('"file://$MODULE_DIR$/build/wsimport"') //generatedSourceDir
    }

    @Test
    void wsimport_generates_sources() {
        writeFile("build.gradle", """
            plugins {
              id 'sktools.wsimport'
            }
            
            repositories {
                maven { url = '${testProperties.MAVEN_REPO}' }
            }
        """)

        writeTestSchemaTo('src/main/resources/META-INF/wsdls')

        testGradleBuild("wsimport")

        assertThat(file('build/wsimport/no/statkart/test/service/v1/TestServiceWS.java')).exists()
    }


    @Test
    void jarFileIncludesResources() {
        writeFile("build.gradle", """
            plugins {
              id 'sktools.wsimport'
            }
            
            repositories {
                maven { url = '${testProperties.MAVEN_REPO}' }
            }
        """)

        writeTestSchemaTo('src/main/resources/META-INF/wsdls')

        testGradleBuild("jar")

        File file = file("build/libs/${rootProjectName()}.jar")
        assertThat(file).exists()

        JarFile jar = new JarFile(file)
        try {
            def jarFileEntryNames = list(jar.entries()).collect { it.getName() }
            assertThat(jarFileEntryNames)
                .as("Contents of jar file")
                .contains(
                    'META-INF/wsdls/TestServiceWS.wsdl',
                    'META-INF/wsdls/TestServiceWS_schema1.xsd',
                    'no/statkart/test/service/v1/TestServiceWS.class',
                )
        } finally {
            jar.close()
        }
    }

    private void writeTestSchemaTo(String path) {
        file(path).mkdirs()
        Files.copy(WsImportPluginTest.class.getResourceAsStream('/TestServiceWS.wsdl'), file("$path/TestServiceWS.wsdl").toPath())
        Files.copy(WsImportPluginTest.class.getResourceAsStream('/TestServiceWS_schema1.xsd'), file("$path/TestServiceWS_schema1.xsd").toPath())
        Files.copy(WsImportPluginTest.class.getResourceAsStream('/TestServiceWS_schema2.xsd'), file("$path/TestServiceWS_schema2.xsd").toPath())
    }
}
