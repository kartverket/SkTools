package no.statkart.sktools.utils.wsdocgen.processor

import groovy.util.slurpersupport.GPathResult
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import no.statkart.sktools.gradle.testutils.filewriter.WsDocgenTestutilFilewriter
import no.statkart.sktools.gradle.testutils.xml.XmlTestUtils
import org.gradle.api.tasks.compile.JavaCompile
import org.testng.Assert
import org.testng.annotations.Test

/**
 * Tester {@link WSDocProcessor}
 *
 * @since 1.3 - ny grunnbok sprint 30
 * @author Leif Lislegård
 */
class WSDocProcessorTest {


    /**
     * Tester eksekvering med default parametere
     */
    @Test
    void testDefaultConfiguration() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder('WsDocgenTest').build()
        def outputPath = 'build/gen/wsdoc'
        def sourcePath = 'src/main/java'
        def resourcePath = 'src/main/resources'

        def xslt;

        //generer eksempel-kildekode
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeSimpleDemoServiceWSBean(sourcePath)
            xslt = projectHelper.writeSimpleXSLT(resourcePath)
        }


        //setter opp testprosjekt
        projectHelper.configureProject {
            mkdir(outputPath)

            apply plugin:'java'

            task('testWSDocProcessor', type:JavaCompile.class) {

                options.compilerArgs = [
                        "-proc:only",
                        "-processor", WSDocProcessor.class.getName(),

                        "-Axslt=${xslt}", //xslt file

                ]

                // specify output of generated code
                destinationDir = file(outputPath)

                // specify source files
                source = sourceSets.main.java
                include('**/*WSBean.java')

                classpath = configurations.compile

            }
        } //end configure

        //utfører task
        projectHelper.executeTask('testWSDocProcessor')


        //tester resultat
        projectHelper.assertFileExists(outputPath + '/TestService.html') { File file ->


            println "Generert html: \n" + file.getText()

            //leser inn html dokumentasjon som xml - dette steget validerer derfor html-koden
            GPathResult html = parseXML(file)

            //sjekker innhold
            assertStringContains(html.head.title.text(), 'TestService', "service name")
            assertStringContains(html.body.span[0].text(), 'TestService', "service name")
            assertStringContains(html.body.span[1].text(), 'beskrivelse av service', "service description")
            Assert.assertEquals(html.body.span[2].text(), 'http://test.statkart.no/test1', "service namespace")

            //sjekker dokumenterte metoder
            Assert.assertEquals html.body.div[0].ul.li.size(), 2, "forventet antall metoder"
            Assert.assertEquals html.body.div[0].ul.li[0].a.text(), 'noPing', "forventet metodenavn"
            Assert.assertEquals html.body.div[0].ul.li[1].a.text(), 'ping', "forventet metodenavn"


            Assert.assertEquals html.body.div[1].div[0].p[0].text().trim(), 'Returnerer PONG', "forventet dokumentasjon"
            Assert.assertEquals html.body.div[1].div[0].h4[0].text().trim(), 'ping', "forventet overskrift"

            Assert.assertEquals html.body.div[1].div[1].p[0].text().trim(), 'Returnerer ikke noe', "forventet dokumentasjon"
            Assert.assertEquals html.body.div[1].div[1].h4[0].text().trim(), 'noPing', "forventet overskrift"

            Assert.assertEquals html.body.div[1].div.size(), 2, "forventet antall metoder for service"
        }

    }

    /**
     * Tester angivelse av {@code javaDocLookupPath}
     */
    @Test
    void testJavaDocLookupPath() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder('WsDocgenTest').build()
        def outputPath = 'build/gen/wsdoc'
        def sourcePath = 'src/main/java'
        def resourcePath = 'src/main/resources'

        def xslt;

        //generer eksempel-kildekode
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeCustomFile('src/main/java/TestWSBean.java') {
                """
                 @javax.jws.WebService(
                     name = "TestService",
                     serviceName = "TestServiceWS",
                     targetNamespace = "http://test.no/unit")
                 public class TestWSBean {

                     /** Returnerer PONG **/
                     @javax.jws.WebMethod
                     public String ping() {
                         return "PONG";
                     }
                 }
                """
            }

            xslt = projectHelper.writeCustomFile('minimal.xsl') {
                """
                <xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

                <xsl:output method="text" version="1.0" media-type="text/plain" omit-xml-declaration="yes" />

                <xsl:template match="/services/service">
                  <xsl:for-each select="methods/method/returns/parameter">
                    <xsl:value-of select="type/@javadocPath"/>
                  </xsl:for-each>
                </xsl:template>

                </xsl:stylesheet>
                """
            }

        }

        //setter opp testprosjekt
        projectHelper.configureProject {
            mkdir(outputPath)

            apply plugin:'java'

            task('testWSDocProcessor', type:JavaCompile.class) {

                options.compilerArgs = [
                        "-proc:only",
                        "-processor", WSDocProcessor.class.getName(),

                        "-Axslt=${xslt}", //xslt file
                        "-AjavaDocLookupPath=../uniktNavn/for/test/index.html", //lookup path
                ]

                // specify output of generated code
                destinationDir = file(outputPath)

                // specify source files
                source = sourceSets.main.java
                include('**/*WSBean.java')

                classpath = configurations.compile

            }
        } //end configure

        //utfører task
        projectHelper.executeTask('testWSDocProcessor')


        //tester resultat
        projectHelper.assertFileExists(outputPath + '/TestService.html') { File file ->


            println "Generert html: \n" + file.getText()

            //sjekker innhold
            def lines = []
            file.eachLine { lines += it}

            assert lines.find { def line -> line.contains('../uniktNavn/for/test/index.html?')} //forventer å finne denne i output

        }

    }

    /**
     * Tester bruk av primitiver
     */
    @Test
    void testPrimitives() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder('WsDocgenTest').build()
        def outputPath = 'build/gen/wsdoc'
        def sourcePath = 'src/main/java'
        def resourcePath = 'src/main/resources'

        def xslt;

        //generer eksempel-kildekode
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeCustomFile('src/main/java/TestWSBean.java') {
                """
                 @javax.jws.WebService(
                     name = "TestService",
                     serviceName = "TestServiceWS",
                     targetNamespace = "http://test.no/unit")
                 public class TestWSBean {

                     /** Returnerer PONG **/
                     @javax.jws.WebMethod
                     public long intToLong(int value) {
                         return 0;
                     }
                 }
                """
            }

            xslt = projectHelper.writeCustomFile('minimal.xsl') {
                """
                <xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

                <xsl:output method="text" version="1.0" media-type="text/plain" omit-xml-declaration="yes" />

                <xsl:template match="/services/service">
                  <xsl:for-each select="methods/method/returns/parameter">
                    <xsl:value-of select="type/@name"/>
                  </xsl:for-each>
                </xsl:template>

                </xsl:stylesheet>
                """
            }

        }

        //setter opp testprosjekt
        projectHelper.configureProject {
            mkdir(outputPath)

            apply plugin:'java'

            task('testWSDocProcessor', type:JavaCompile.class) {

                options.compilerArgs = [
                        "-proc:only",
                        "-processor", WSDocProcessor.class.getName(),

                        "-Axslt=${xslt}", //xslt file
                        "-AjavaDocLookupPath=../uniktNavn/for/test/index.html", //lookup path
                ]

                // specify output of generated code
                destinationDir = file(outputPath)

                // specify source files
                source = sourceSets.main.java
                include('**/*WSBean.java')

                classpath = configurations.compile

            }
        } //end configure

        //utfører task
        projectHelper.executeTask('testWSDocProcessor')


        //tester resultat
        projectHelper.assertFileExists(outputPath + '/TestService.html') { File file ->


            println "Generert html: \n" + file.getText()

            //sjekker innhold
            def lines = []
            file.eachLine { lines += it}

            assert lines.find { def line -> line.contains('long')} //forventer å finne denne i output

        }

    }

    public static GPathResult parseXML(File file) {
        XmlSlurper slurper = XmlTestUtils.defaultXmlSlurper()
        return slurper.parse(file)
    }

    public static void assertStringContains(CharSequence actual, String expected, String message) {
        if (!actual.contains(expected)) {
            Assert.fail("Expected that \"${actual}\" contains \"${expected}\": ${message}");
        }
    }
}
