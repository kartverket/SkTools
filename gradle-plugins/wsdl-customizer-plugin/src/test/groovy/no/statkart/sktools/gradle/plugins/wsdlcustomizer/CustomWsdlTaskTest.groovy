package no.statkart.sktools.gradle.plugins.wsdlcustomizer

import groovy.util.slurpersupport.GPathResult
import no.statkart.sktools.gradle.testutils.ProjectHelper
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
        ProjectHelper projectHelper = new ProjectHelper(project)

        File destDir = project.file('result')

        // Dette virker ikke hvis man skulle finne på å kjøre testene fra jar-fil
        URL handmadeUrl = getClass().getResource("/handmade")
        File handmadeDir = new File(handmadeUrl.toURI())
        URL generatedUrl = getClass().getResource("/fakeGenerated")
        File generatedDir = new File(generatedUrl.toURI())

        project.task('customWsdl', type: CustomWsdlTask) {
            destinationDir = destDir

            originalSchemaFiles project.fileTree(handmadeDir)
            generatedWsdlAndSchemaFiles project.fileTree(generatedDir)

            excludeNamespaces 'http://statkart.no/sktools/wsapi/v1/domain/register/person'
        }

        Assert.assertEquals(project.customWsdl.originalSchemaFiles.files.size(), 4, 'Antall skjemafiler')
        Assert.assertEquals(project.customWsdl.generatedWsdlAndSchemaFiles.files.size(), 6, 'Antall genererte file')

        projectHelper.executeTask('customWsdl')

        FileTree result = project.fileTree(destDir)

        result.each { File f ->
            String name = f.name
            if (name == 'StoreServiceWS.wsdl') {
                checkWsdl(f)
            } else if (name == 'StoreServiceWS_schema1.xsd') {
                checkServiceSchema(f)
            } else if (name != 'exception.xsd' && name != 'kommune.xsd' && name != 'basistyper.xsd') {
                Assert.fail('Forventet ikke fil: ' + f)
            }
        }
    }

    private void checkWsdl(File f) {
        GPathResult wsdl = xmlSlurper.parse(f)
        wsdl.types[0].schema.each {
            Assert.assertEquals(it.import.size(), 1, 'Antall imports')
            def imp = it.import[0]
            if (imp.@namespace == 'http://statkart.no/sktools/wsapi/v1/service/store') {
                Assert.assertEquals(imp.@schemaLocation as String, 'StoreServiceWS_schema1.xsd')
            } else if (imp.@namespace == 'http://statkart.no/sktools/wsapi/v1/exception') {
                Assert.assertEquals(imp.@schemaLocation as String, 'exception.xsd')
            } else if (imp.@namespace == 'http://statkart.no/sktools/wsapi/v1/domain/basistyper') {
                Assert.assertEquals(imp.@schemaLocation as String, 'basistyper.xsd')
            } else if (imp.@namespace == 'http://statkart.no/sktools/wsapi/v1/domain/register') {
                Assert.assertEquals(imp.@schemaLocation as String, 'kommune.xsd')
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
