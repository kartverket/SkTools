package no.statkart.sktools.gradle.plugins.wsdlcustomizer

import groovy.util.slurpersupport.GPathResult
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import org.testng.annotations.Test

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

        Assert.assertEquals(customWsdl.originalSchemaFiles.files.size(), 4, 'Antall skjemafiler')
        Assert.assertEquals(customWsdl.generatedWsdlAndSchemaFiles.files.size(), 12, 'Antall genererte file')

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
                Assert.fail('Forventet ikke fil: ' + f)
            }
        }

        Assert.assertEquals(antallFunnet, 4, "Forventet wsdl og xsd for hver tjeneste!");
    }

    private void checkWsdl(File f) {
        GPathResult wsdl = xmlSlurper.parse(f)
        wsdl.types[0].schema.each {
            Assert.assertEquals(it.import.size(), 1, 'Antall imports')
            def imp = it.import[0]
            def name = imp.@schemaLocation as String
            if (imp.@namespace == 'http://statkart.no/sktools/wsapi/v1/service/store') {
                Assert.assertTrue(name.contains('StoreService'))
                Assert.assertTrue(name.contains('WS_schema1.xsd'))
            } else if (imp.@namespace == 'http://statkart.no/sktools/wsapi/v1/exception') {
                Assert.assertEquals(name, 'exception.xsd')
            } else if (imp.@namespace == 'http://statkart.no/sktools/wsapi/v1/domain/basistyper') {
                Assert.assertEquals(name, 'basistyper.xsd')
            } else if (imp.@namespace == 'http://statkart.no/sktools/wsapi/v1/domain/register') {
                Assert.assertEquals(name, 'kommune.xsd')
            } else {
                Assert.fail('Uventet namespace: ' + imp.@namespace)
            }
        }
    }

    private void checkServiceSchema(File f) {
        GPathResult schema = xmlSlurper.parse(f)
        schema.import.each {
            if (it.@namespace == 'http://statkart.no/sktools/wsapi/v1/domain/basistyper') {
                Assert.assertEquals(it.@schemaLocation as String, 'basistyper.xsd')
            }
        }
    }
}
