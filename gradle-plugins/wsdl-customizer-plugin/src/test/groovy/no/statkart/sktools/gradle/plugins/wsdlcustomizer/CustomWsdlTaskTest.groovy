package no.statkart.sktools.gradle.plugins.wsdlcustomizer

import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.Test

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.fail

/**
 * Tester selve prosesseringen.
 *
 * @author Tor Egil R. Strand
 * @since 1.3
 */
class CustomWsdlTaskTest {
    private xmlSlurper = new XmlSlurper()

    @Test
    void testCustomWsdl() {
        Project project = ProjectBuilder.builder().build()

        File destDir = project.file('result')

        // Dette virker ikke hvis man skulle finne på å kjøre testene fra jar-fil
        URL handmadeUrl = getClass().getResource("/handmade")
        File handmadeDir = new File(handmadeUrl.toURI())
        URL generatedUrl = getClass().getResource("/fakeGenerated")
        File generatedDir = new File(generatedUrl.toURI())

        CustomWsdlTask customWsdl = project.task('customWsdl', type: CustomWsdlTask) {
            destinationDir = destDir

            originalSchemaFiles project.fileTree(handmadeDir)
            generatedWsdlAndSchemaFiles project.fileTree(generatedDir)

            excludeNamespaces 'http://statkart.no/sktools/wsapi/v1/domain/register/person'
        }

        assertThat(customWsdl.originalSchemaFiles.files).as('skjemafiler').hasSize(4)
        assertThat(customWsdl.generatedWsdlAndSchemaFiles.files).as('genererte file').hasSize(12)

        customWsdl.generate()

        FileTree result = project.fileTree(destDir)

        int antallFunnet = 0
        result.each { File f ->
            String name = f.name
            if (name == 'StoreServiceWS.wsdl') {
                checkWsdl(f)
                antallFunnet++
            } else if (name == 'StoreServiceWS_schema1.xsd') {
                checkServiceSchema(f)
                antallFunnet++
            } else if (name == 'StoreService2WS.wsdl') {
                checkWsdl(f)
                antallFunnet++
            } else if (name == 'StoreService2WS_schema1.xsd') {
                checkServiceSchema(f)
                antallFunnet++
            } else if (name != 'exception.xsd' && name != 'kommune.xsd' && name != 'basistyper.xsd') {
                fail('Forventet ikke fil: ' + f)
            }
        }

        assertThat(antallFunnet).isEqualTo(4)
    }

    private void checkWsdl(File f) {
        GPathResult wsdl = xmlSlurper.parse(f)
        wsdl.types[0].schema.each {
            assertThat(it.import.size() as int).as("Antall imports").isEqualTo(1)
            def imp = it.import[0]
            def name = imp.@schemaLocation as String
            if (imp.@namespace == 'http://statkart.no/sktools/wsapi/v1/service/store') {
                assertThat(name)
                    .contains('StoreService')
                    .contains('WS_schema1.xsd')
            } else if (imp.@namespace == 'http://statkart.no/sktools/wsapi/v1/exception') {
                assertThat(name).contains('exception.xsd')
            } else if (imp.@namespace == 'http://statkart.no/sktools/wsapi/v1/domain/basistyper') {
                assertThat(name).contains('basistyper.xsd')
            } else if (imp.@namespace == 'http://statkart.no/sktools/wsapi/v1/domain/register') {
                assertThat(name).contains('kommune.xsd')
            } else {
                fail('Uventet namespace: ' + imp.@namespace)
            }
        }
    }

    private void checkServiceSchema(File f) {
        GPathResult schema = xmlSlurper.parse(f)
        schema.import.each {
            if (it.@namespace == 'http://statkart.no/sktools/wsapi/v1/domain/basistyper') {
                assertThat(it.@schemaLocation as String).isEqualTo('basistyper.xsd')
            }
        }
    }
}
