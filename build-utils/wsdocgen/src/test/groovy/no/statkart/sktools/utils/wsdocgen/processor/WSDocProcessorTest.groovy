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
            Assert.assertEquals html.body.div[0].ul.li.size(), 3, "forventet antall metoder"
            Assert.assertEquals html.body.div[0].ul.li[0].a.text(), 'binary', "forventet metodenavn"
            Assert.assertEquals html.body.div[0].ul.li[1].a.text(), 'noPing', "forventet metodenavn"
            Assert.assertEquals html.body.div[0].ul.li[2].a.text(), 'ping', "forventet metodenavn"


            Assert.assertEquals html.body.div[1].div[0].p[0].text().trim(), 'Returnerer PONG', "forventet dokumentasjon"
            Assert.assertEquals html.body.div[1].div[0].h4[0].text().trim(), 'ping', "forventet overskrift"

            Assert.assertEquals html.body.div[1].div[1].p[0].text().trim(), 'Returnerer ikke noe', "forventet dokumentasjon"
            Assert.assertEquals html.body.div[1].div[1].h4[0].text().trim(), 'noPing', "forventet overskrift"

            Assert.assertEquals html.body.div[1].div[2].p[0].text().trim(), 'Returnerer noen bytes', "forventet dokumentasjon"
            Assert.assertEquals html.body.div[1].div[2].h4[0].text().trim(), 'binary', "forventet overskrift"

            Assert.assertEquals html.body.div[1].div.size(), 3, "forventet antall metoder for service"
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

    /**
     * Tester dokumentasjon av tagger
     */
    @Test
    void testReturnTaglets() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder('WsDocgenTest').build()
        def outputPath = 'build/gen/wsdoc'
        def sourcePath = 'src/main/java'
        def resourcePath = 'src/main/resources'

        def xslt;

        //generer eksempel-kildekode
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeCustomFile('src/main/java/TestWSBean.java') {
                """
                package test1;

                /**
                 * Service description.
                 * Second sentence.
                 * @since 1.0 - inception
                 * @author Leif Lislegård
                 **/
                 @javax.jws.WebService(
                         name = "TestService",
                         serviceName = "TestServiceWS",
                         targetNamespace = "http://test.statkart.no/test1")
                         /** jaja **/
                 public class TestWSBean {

                     /** Intended for asserting a conversion.
                     * @param base
                     * @param value value for conversion
                     * @return the converted value
                     * @since 1.0
                     * @hint
                     */
                     public long intToLong(int value, int base) {
                         return 0;
                     }
                 }
                """
            }

            xslt = projectHelper.writeCustomFile('minimal.xsl') {
                """
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/services/service">
<html><body>
   description=<span><xsl:value-of select="description"/></span>

    <div>
        <xsl:for-each select="methods/method">
          <div>
            <h4><xsl:value-of select="@name"/></h4>
            <p><xsl:value-of select="description"/></p>


            <h5>Response</h5>
            <ul>
              <xsl:for-each select="returns/parameter">
                <li>
                   <span><xsl:value-of select="@name"/></span>
                   <p><xsl:value-of select="description"/></p>
                   <div>
                     <span><xsl:value-of select="type/@name"/></span>
                     <span><xsl:value-of select="type"/></span>
                   </div>
                </li>
              </xsl:for-each>
            </ul>
          </div>
        </xsl:for-each>
    </div>

</body></html>
</xsl:template>
</xsl:stylesheet>
                """
            }

        }

        //setter opp testprosjekt
        projectHelper.configureProject {
            mkdir(outputPath)

            apply plugin: 'java'

            task('testWSDocProcessor', type: JavaCompile.class) {

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
            Assert.assertEquals html.body.span[0].text(), 'Service description.\nSecond sentence.', "service description"

            //sjekker dokumenterte metoder
            Assert.assertEquals html.body.div[0].div[0].h4[0].text().trim(), 'intToLong', "overskrift"
            Assert.assertEquals html.body.div[0].div[0].ul[0].li[0].p[0].text().trim(), 'the converted value', "dokumentasjon av retur"

            Assert.assertEquals html.body.div[0].div.size(), 1, "forventet antall metoder for service"

        }
    }


    /**
     * Tester dokumentasjon av tagger
     */
    @Test
    void testExceptionTaglets() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder('WsDocgenTest').build()
        def outputPath = 'build/gen/wsdoc'
        def sourcePath = 'src/main/java'
        def resourcePath = 'src/main/resources'

        def xslt;

        //generer eksempel-kildekode
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeCustomFile('src/main/java/TestWSBean.java') {
                """
                package test1;

                /**
                 * Service description.
                 * Second sentence.
                 * @since 1.0 - inception
                 * @author Leif Lislegård
                 **/
                 @javax.jws.WebService(
                         name = "TestService",
                         serviceName = "TestServiceWS",
                         targetNamespace = "http://test.statkart.no/test1")
                         /** jaja **/
                 public class TestWSBean {

                     /**
                      * Intended for asserting a conversion.
                      * @return the converted value
                      * @throws Exception ved feil i konvertering
                      * @since 1.0
                      */
                     public long intToLong(int value, int base) throws Exception {
                         return 0;
                     }

                     /**
                      * Intended for asserting a conversion.
                      * @return the converted value as int
                      * @throws Exception ved feil i konvertering
                      * @throws RuntimeException dersom base-verdi ikke validerer
                      * @since 1.0
                      */
                     public int longToInt(long value, int base) throws RuntimeException, Exception {
                         return 0;
                     }
                 }
                """
            }

            xslt = projectHelper.writeCustomFile('minimal.xsl') {
                """
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/services/service">
<html><body>
   description=<span><xsl:value-of select="description"/></span>

    <div>
        <xsl:for-each select="methods/method">
          <div>
            <h4><xsl:value-of select="@name"/></h4>
            <p><xsl:value-of select="description"/></p>


            <h5>Response</h5>
            <ul>
              <xsl:for-each select="returns/parameter">
                <li>
                   <span><xsl:value-of select="@name"/></span>
                   <p><xsl:value-of select="description"/></p>
                   <div>
                     <span><xsl:value-of select="type/@name"/></span>
                     <span><xsl:value-of select="type"/></span>
                   </div>
                </li>
              </xsl:for-each>
              <xsl:for-each select="exceptions/exception">
                <li>
                   <span><xsl:value-of select="@name"/></span>
                   <p><xsl:value-of select="description"/></p>
                   <div>
                     <span><xsl:value-of select="type/@name"/></span>
                     <span><xsl:value-of select="type"/></span>
                   </div>
                </li>
              </xsl:for-each>
            </ul>
          </div>
        </xsl:for-each>
    </div>

</body></html>
</xsl:template>
</xsl:stylesheet>
                """
            }

        }

        //setter opp testprosjekt
        projectHelper.configureProject {
            mkdir(outputPath)

            apply plugin: 'java'

            task('testWSDocProcessor', type: JavaCompile.class) {

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
            Assert.assertEquals html.body.span[0].text(), 'Service description.\nSecond sentence.', "service description"

            //sjekker dokumenterte metoder
            Assert.assertEquals html.body.div[0].div[0].h4[0].text().trim(), 'intToLong', "overskrift"
            Assert.assertEquals html.body.div[0].div[0].ul[0].li[0].p[0].text().trim(), 'the converted value', "dokumentasjon av retur"
            Assert.assertEquals html.body.div[0].div[0].ul[0].li[1].span[0].text().trim(), 'Exception', "navn for exception"
            Assert.assertEquals html.body.div[0].div[0].ul[0].li[1].p[0].text().trim(), 'ved feil i konvertering', "dokumentasjon for exception"

            Assert.assertEquals html.body.div[0].div[1].h4[0].text().trim(), 'longToInt', "overskrift"
            Assert.assertEquals html.body.div[0].div[1].ul[0].li[0].p[0].text().trim(), 'the converted value as int', "dokumentasjon av retur"
            Assert.assertEquals html.body.div[0].div[1].ul[0].li[2].span[0].text().trim(), 'Exception', "navn for exception"
            Assert.assertEquals html.body.div[0].div[1].ul[0].li[2].p[0].text().trim(), 'ved feil i konvertering', "dokumentasjon for exception"
            Assert.assertEquals html.body.div[0].div[1].ul[0].li[1].span[0].text().trim(), 'RuntimeException', "navn for exception"
            Assert.assertEquals html.body.div[0].div[1].ul[0].li[1].p[0].text().trim(), 'dersom base-verdi ikke validerer', "dokumentasjon for exception"

            Assert.assertEquals html.body.div[0].div.size(), 2, "forventet antall metoder for service"

        }
    }

    /**
     * Tester dokumentasjon av tagger
     */
    @Test
    void testParamTaglets() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder('WsDocgenTest').build()
        def outputPath = 'build/gen/wsdoc'
        def sourcePath = 'src/main/java'
        def resourcePath = 'src/main/resources'

        def xslt;

        //generer eksempel-kildekode
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeCustomFile('src/main/java/TestWSBean.java') {
                """
                package test1;

                /**
                 * Service description.
                 * Second sentence.
                 * @since 1.0 - inception
                 * @author Leif Lislegård
                 **/
                 @javax.jws.WebService(
                         name = "TestService",
                         serviceName = "TestServiceWS",
                         targetNamespace = "http://test.statkart.no/test1")
                         /** jaja **/
                 public class TestWSBean {

                     /** Intended for asserting a conversion.
                     * @param base
                     * @param value value for conversion
                     * @return the converted value
                     * @since 1.0
                     * @hint
                     */
                     public long intToLong(int value, int base) {
                         return 0;
                     }
                 }
                """
            }

            xslt = projectHelper.writeCustomFile('minimal.xsl') {
                """
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/services/service">
<html><body>
   description=<span><xsl:value-of select="description"/></span>

    <div>
        <xsl:for-each select="methods/method">
          <div>
            <h4><xsl:value-of select="@name"/></h4>
            <p><xsl:value-of select="description"/></p>

            <h5>Input</h5>
            <ul>
              <xsl:for-each select="parameters/parameter">
                <li>
                   <span><xsl:value-of select="@name"/></span>
                   <p><xsl:value-of select="description"/></p>
                   <div>
                     <span><xsl:value-of select="type/@name"/></span>
                     <span><xsl:value-of select="type"/></span>
                   </div>
                </li>
              </xsl:for-each>
            </ul>

          </div>
        </xsl:for-each>
    </div>

</body></html>
</xsl:template>
</xsl:stylesheet>
                """
            }

        }

        //setter opp testprosjekt
        projectHelper.configureProject {
            mkdir(outputPath)

            apply plugin: 'java'

            task('testWSDocProcessor', type: JavaCompile.class) {

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
            Assert.assertEquals html.body.span[0].text(), 'Service description.\nSecond sentence.', "service description"

            //sjekker dokumenterte metoder
            Assert.assertEquals html.body.div[0].div[0].p[0].text().trim(), 'Intended for asserting a conversion.', "forventet dokumentasjon"
            Assert.assertEquals html.body.div[0].div[0].h4[0].text().trim(), 'intToLong', "forventet overskrift"

            Assert.assertEquals html.body.div[0].div.size(), 1, "forventet antall metoder for service"

        }
    }


    /**
     * SKTOOLS-105
     * Tester generering av index fil
     */
    @Test
    void testSimpleIndex() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder('WsDocgenTest').build()
        def outputPath = 'build/gen/wsdoc'
        def sourcePath = 'src/main/java'
        def resourcePath = 'src/main/resources'

        def xslt;
        def indexXslt;

        //generer eksempel-kildekode
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeCustomFile('src/main/java/Service1WSBean.java') {
                """
                package test1;

                /**
                 * Ping test service.
                 **/
                 @javax.jws.WebService(
                         name = "TestService1",
                         serviceName = "TestServiceWS",
                         targetNamespace = "http://test.statkart.no/test1")
                 public class Service1WSBean {

                     /** Returnerer PONG **/
                     public String ping() {
                         return "PONG";
                     }
                 }
                """
            }

            projectHelper.writeCustomFile('src/main/java/Service2WSBean.java') {
                """
                package test2;

                /**
                 * Inception service.
                 **/
                 @javax.jws.WebService(
                         name = "TestService2",
                         serviceName = "TestServiceWS",
                         targetNamespace = "http://test.statkart.no/test2")
                 public class Service2WSBean {

                     /**
                      * Intended for asserting a conversion.
                      */
                     public long intToLong(int value, int base) throws Exception {
                         return 0;
                     }

                     /**
                      * Intended for asserting a conversion.
                      */
                     public int longToInt(long value, int base) throws RuntimeException, Exception {
                         return 0;
                     }
                 }
                """
            }

            xslt = projectHelper.writeCustomFile('minimal.xsl') {
                """
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/">
  <html><body>
  <xsl:apply-templates/>
  </body></html>
</xsl:template>

<xsl:template match="services/service">
   name=<span><xsl:value-of select="@name"/></span>
   href=<span><xsl:value-of select="@href"/></span>
   description=<p><xsl:value-of select="description"/></p>
</xsl:template>

</xsl:stylesheet>
                """
            }


            indexXslt = projectHelper.writeCustomFile('index.xsl') {
                """
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/">
  <html><body>
  <xsl:apply-templates/>
  </body></html>
</xsl:template>

<xsl:template match="services/service">
   Services: <br/>
   <a href="{@href}">
        name=<span><xsl:value-of select="@name"/></span>
        href=<span><xsl:value-of select="@href"/></span>
        description=<p><xsl:value-of select="description"/></p>
   </a>
</xsl:template>

</xsl:stylesheet>
                """
            }

        }

        //setter opp testprosjekt
        projectHelper.configureProject {
            mkdir(outputPath)

            apply plugin: 'java'

            task('testWSDocProcessor', type: JavaCompile.class) {

                options.compilerArgs = [
                        "-proc:only",
                        "-processor", WSDocProcessor.class.getName(),

                        "-Axslt=${xslt}", //xslt file
                        "-AindexXslt=${indexXslt}", //xslt file for generating index

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
        projectHelper.assertFileExists(outputPath + "/index.html") { File file ->

            println "Generert html: \n" + file.getText()

            //leser inn html dokumentasjon som xml - dette steget validerer derfor html-koden
            GPathResult html = parseXML(file)

            //sjekker innhold
            Assert.assertEquals html.body.a[0].@href.text(), "TestService1.html", "href service"
            Assert.assertEquals html.body.a[0].span[0].text(), "TestServiceWS", "service name"
            Assert.assertEquals html.body.a[0].span[1].text(), "TestService1.html", "service url"
            Assert.assertEquals html.body.a[0].p[0].text(), "Ping test service.", "service description"


            Assert.assertEquals html.body.a[1].@href.text(), "TestService2.html", "href service"
            Assert.assertEquals html.body.a[1].span[0].text(), "TestServiceWS", "service name"
            Assert.assertEquals html.body.a[1].span[1].text(), "TestService2.html", "service url"
            Assert.assertEquals html.body.a[1].p[0].text(), "Inception service.", "service description"
        }


        (1..2).each { int idx ->
            projectHelper.assertFileExists(outputPath + "/TestService${idx}.html") { File file ->

                println "Generert html: \n" + file.getText()

                //leser inn html dokumentasjon som xml - dette steget validerer derfor html-koden
                GPathResult html = parseXML(file)

                //sjekker innhold
                Assert.assertEquals html.body.span[0].text(), "TestServiceWS", "service name"
                Assert.assertEquals html.body.span[1].text(), "TestService${idx}.html", "service url"
            }
        }
    }


    /**
     * SKTOOLS-106
     * Tester inline @code taglet
     */
    @Test
    void testCodeTaglets() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder('WsDocgenTest').build()
        def outputPath = 'build/gen/wsdoc'
        def sourcePath = 'src/main/java'
        def resourcePath = 'src/main/resources'

        def xslt;

        //generer eksempel-kildekode
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeCustomFile('src/main/java/TestWSBean.java') {
                """
                package test1;

                /**
                 * Service {@code taglet} description.
                 *
                 * @since 1.0 - inception
                 * @author Leif Lislegård
                 **/
                 @javax.jws.WebService(
                         name = "TestService",
                         serviceName = "TestServiceWS",
                         targetNamespace = "http://test.statkart.no/test1")
                 public class TestWSBean {

                 }
                """
            }

            xslt = projectHelper.writeCustomFile('minimal.xsl') {
                """
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/services/service">
<html><body>
   description=<p><xsl:value-of select="description"/></p>
   formatted description=<p><xsl:apply-templates select="description"/></p>
</body></html>
</xsl:template>

<xsl:template match="description">
  <xsl:comment>DESCRIPTION:</xsl:comment>
  <xsl:for-each select="text()|*">
    <xsl:choose>
      <xsl:when test="name(.)">
        <xsl:comment>escaped</xsl:comment>
        <xsl:apply-templates select="." mode="escapedText"/>
      </xsl:when>

      <xsl:otherwise>
        <xsl:comment>no escaped</xsl:comment>
        <xsl:apply-templates select="." mode="#current" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:for-each>

</xsl:template>

<xsl:template match="span" mode="#all">
  <xsl:element name="span">
    <xsl:attribute name="class" select="@class" />

    <xsl:choose>
      <xsl:when test="@class='javadoc_tag_code'">
        <xsl:attribute name="debug">span with escaped contents!</xsl:attribute>
        <xsl:apply-templates mode="escapedText" />
      </xsl:when>

      <xsl:otherwise>
        <xsl:attribute name="debug">normal span element</xsl:attribute>
        <xsl:apply-templates mode="noEscapedText"/>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:element>
</xsl:template>

<xsl:template match="text()" mode="noEscapedText">
    <xsl:value-of select="." disable-output-escaping="yes" />
</xsl:template>


</xsl:stylesheet>
                """
            }

        }

        //setter opp testprosjekt
        projectHelper.configureProject {
            mkdir(outputPath)

            apply plugin: 'java'

            task('testWSDocProcessor', type: JavaCompile.class) {

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
            Assert.assertEquals html.body.p[0].text().trim(), 'Service taglet description.', "service description"
            Assert.assertEquals html.body.p[1].span[0].text().trim(), 'taglet', "code enclosed taglet"
        }
    }



    /**
     * SKTOOLS-107
     * Regresjonstest av return tag navn
     */
    @Test
    void testReturnName() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder('WsDocgenTest').build()
        def outputPath = 'build/gen/wsdoc'
        def sourcePath = 'src/main/java'
        def resourcePath = 'src/main/resources'

        def xslt;

        //generer eksempel-kildekode
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeCustomFile('src/main/java/TestWSBean.java') {
                """
                package test1;

                /**
                 * Viser forskjellig angivelse av retur parametere
                 **/
                 @javax.jws.WebService(
                         name = "TestService",
                         serviceName = "TestServiceWS",
                         targetNamespace = "http://test.statkart.no/test1")
                 public class TestWSBean {

                    /** @return ikke noe */
                    public void noReturn() {
                    }

                    /** @return withouth annotation */
                    public String ping1() {
                      return "";
                    }

                    /** @return with empty annotation */
                    @javax.jws.WebResult()
                    public String ping2() {
                      return "";
                    }

                    /** @return with annotation */
                    @javax.jws.WebResult(name = "youPingResult")
                    public String ping3() {
                      return "";
                    }

                 }
                """
            }

            xslt = projectHelper.writeCustomFile('minimal.xsl') {
                """
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/services/service">
<html><body>
   description=<p><xsl:value-of select="description"/></p>

    <div>
        <xsl:for-each select="methods/method">
          <div>
            <h4><xsl:value-of select="@name"/></h4>
            <p><xsl:value-of select="description"/></p>


            <h5>Response</h5>
            <ul>
              <xsl:for-each select="returns/parameter">
                <li>
                   <span><xsl:value-of select="@name"/></span>
                   <p><xsl:value-of select="description"/></p>
                   <div>
                     <span><xsl:value-of select="type/@name"/></span>
                     <span><xsl:value-of select="type"/></span>
                   </div>
                </li>
              </xsl:for-each>
            </ul>
          </div>
        </xsl:for-each>
    </div>

</body></html>
</xsl:template>
</xsl:stylesheet>
                """
            }

        }

        //setter opp testprosjekt
        projectHelper.configureProject {
            mkdir(outputPath)

            apply plugin: 'java'

            task('testWSDocProcessor', type: JavaCompile.class) {

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
            Assert.assertEquals html.body.div[0].div[0].h4[0].text(), 'noReturn', "method name"
            Assert.assertEquals html.body.div[0].div[0].ul[0].li[0].span[0].text(), '', "return tag element"
            Assert.assertEquals html.body.div[0].div[0].ul[0].li[0].p[0].text(), '', "return description"

            Assert.assertEquals html.body.div[0].div[1].h4[0].text(), 'ping1', "method name"
            Assert.assertEquals html.body.div[0].div[1].ul[0].li[0].span[0].text(), 'return', "return tag element"
            Assert.assertEquals html.body.div[0].div[1].ul[0].li[0].p[0].text(), 'withouth annotation', "return description"

            Assert.assertEquals html.body.div[0].div[2].h4[0].text(), 'ping2', "method name"
            Assert.assertEquals html.body.div[0].div[2].ul[0].li[0].span[0].text(), 'return', "return tag element"
            Assert.assertEquals html.body.div[0].div[2].ul[0].li[0].p[0].text(), 'with empty annotation', "return description"

            Assert.assertEquals html.body.div[0].div[3].h4[0].text(), 'ping3', "method name"
            Assert.assertEquals html.body.div[0].div[3].ul[0].li[0].span[0].text(), 'youPingResult', "return tag element"
            Assert.assertEquals html.body.div[0].div[3].ul[0].li[0].p[0].text(), 'with annotation', "return description"


        }
    }


    /**
     * Tester formatering med inline taglets
     * @since 1.3 - SKTOOLS-108
     */
    @Test
    void testInlineTaglets() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder('WsDocgenTest').build()
        def outputPath = 'build/gen/wsdoc'
        def sourcePath = 'src/main/java'
        def resourcePath = 'src/main/resources'

        def xslt;

        //generer eksempel-kildekode
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeCustomFile('src/main/java/TestWSBean.java') {
                """
                package test1;

                /**
                 * Service description. {@bold Bold sentence.}
                 * <p>wrapped text test of {@code GBOK-4872}</p>
                 * @since SKTOOLS-108
                 **/
                 @javax.jws.WebService(
                         name = "TestService",
                         serviceName = "TestServiceWS",
                         targetNamespace = "http://test.statkart.no/test1")
                 public class TestWSBean {

                     /**
                     * {@bold Bold sentence.}
                     * Intended for asserting a conversion.
                     * <ul><li>testlist</li></ul>
                     * @since SKTOOLS-108
                     * @hint
                     * @param value value in {@code base} system
                     * @return value typed as {@code long} {@code <encoded>}
                     */
                     public long intToLong(int value, int base) {
                         return 0;
                     }
                 }
                """
            }

            xslt = projectHelper.writeCustomFile('minimal.xsl') {
                """
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/services/service">
<html><body>

   <xsl:comment>description with no escaped javadoc: </xsl:comment>
   description=<div><xsl:apply-templates select="description"/></div>

   <xsl:comment>description without formatting: </xsl:comment>
   description=<div><xsl:value-of select="description"/></div>

    <div>
    <xsl:comment>methods...</xsl:comment>
        <xsl:for-each select="methods">
            <xsl:apply-templates />
        </xsl:for-each>
    <xsl:comment>end methods...</xsl:comment>
    </div>

</body></html>
</xsl:template>

<xsl:template match="method">
      <div>
        <h4><xsl:value-of select="@name"/></h4>
        <p><xsl:apply-templates select="description"/></p>

        <h5>Output</h5>
        <ul>
          <xsl:for-each select="returns/parameter">
            <li>
               <span><xsl:value-of select="@name"/></span>
               <p><xsl:apply-templates select="description"/></p>
            </li>
          </xsl:for-each>
        </ul>

      </div>
</xsl:template>

<xsl:template match="description">
  <xsl:comment>DESCRIPTION:</xsl:comment>
  <xsl:for-each select="text()|*">
    <xsl:choose>
      <xsl:when test="name(.)">
        <xsl:apply-templates select="." mode="#default"/>
      </xsl:when>

      <xsl:otherwise>
        <xsl:apply-templates select="." mode="noEscapedText"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:for-each>

</xsl:template>


<xsl:template match="span" mode="#all">
  <xsl:element name="span">
    <xsl:attribute name="class" select="@class" />

    <xsl:choose>
      <xsl:when test="@class='javadoc_tag_code'">
        <xsl:attribute name="debug">span with escaped contents!</xsl:attribute>
        <xsl:apply-templates mode="#default" />
      </xsl:when>

      <xsl:otherwise>
        <xsl:attribute name="debug">normal span element</xsl:attribute>
        <xsl:apply-templates mode="noEscapedText"/>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:element>
</xsl:template>

<xsl:template match="text()" mode="noEscapedText">
    <xsl:value-of select="." disable-output-escaping="yes" />
</xsl:template>

</xsl:stylesheet>
                """
            }

        }

        //setter opp testprosjekt
        projectHelper.configureProject {
            mkdir(outputPath)

            apply plugin: 'java'

            task('testWSDocProcessor', type: JavaCompile.class) {

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

            //sjekker innhold - example with no escaped text
            Assert.assertEquals html.body.div[0].text(), 'Service description. Bold sentence.wrapped text test of GBOK-4872', "plaintext service description"
            Assert.assertEquals html.body.div[0].span[0].text(), 'Bold sentence.', "{@bold ...} turns into <div>"
            Assert.assertEquals html.body.div[0].p.text(), 'wrapped text test of GBOK-4872', "nested <p>"
            Assert.assertEquals html.body.div[0].p.span[0].text(), 'GBOK-4872', "{@code GBOK-4872} turns into <div>"

            //sjekker innhold - example of escaped text
            Assert.assertEquals html.body.div[1].text().replaceAll('\n', ''), 'Service description. Bold sentence.<p>wrapped text test of GBOK-4872</p>', "escaped service description"

            //sjekker dokumenterte metoder
            Assert.assertEquals html.body.div[2].div.size(), 1, "forventet antall metoder for service"

            Assert.assertEquals html.body.div[2].div[0].h4[0].text().trim(), 'intToLong', "forventet overskrift"
            Assert.assertEquals html.body.div[2].div[0].p[0].text().trim().replaceAll("\\s+"," "), 'Bold sentence. Intended for asserting a conversion. testlist', "forventet dokumentasjon"

            Assert.assertEquals html.body.div[2].div[0].p[0].span[0].@class.text(), 'javadoc_tag_bold', "forventet CSS.class"
            Assert.assertEquals html.body.div[2].div[0].p[0].span[0].text().trim(), 'Bold sentence.', "forventet tekst for span"

            Assert.assertEquals html.body.div[2].div[0].p[0].ul[0].li[0].text().trim(), 'testlist', "forventet tekst for li"

            Assert.assertEquals html.body.div[2].div[0].ul[0].li[0].span[0].text(), 'return', "forventet tekst for retur"
            Assert.assertEquals html.body.div[2].div[0].ul[0].li[0].p[0].text().trim().replaceAll("\\s+"," "), 'value typed as long<encoded>', "forventet dokumentasjon av retur" //groovy substituerer &gt; og andre entiteter...
            Assert.assertEquals html.body.div[2].div[0].ul[0].li[0].p[0].span[0].text().trim(), 'long', "forventet formatert dokumentasjon av retur"
            Assert.assertEquals html.body.div[2].div[0].ul[0].li[0].p[0].span[1].text().trim(), '<encoded>', "forventet formatert dokumentasjon av retur" //groovy substituerer &gt; og andre entiteter...

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
