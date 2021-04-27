package no.statkart.sktools.utils.wsdocgen.processor

import groovy.util.slurpersupport.GPathResult
import no.statkart.sktools.gradle.testutils.TestKitBase
import no.statkart.sktools.gradle.testutils.filewriter.WsDocgenTestutilFilewriter
import no.statkart.sktools.gradle.testutils.xml.XmlTestUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testng.Assert
import org.testng.annotations.Test

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.contentOf
import static org.testng.Assert.fail

/**
 * Tester {@link WSDocProcessor}
 */
class WSDocProcessorTest extends TestKitBase {
    static final Logger log = LoggerFactory.getLogger(WSDocProcessorTest)

    static final String processorPath = WSDocProcessorTest.class.getResource("/processor-classpath.txt").text
    static final String classpath = WSDocProcessorTest.class.getResource("/processor-classpath.txt").text

    void test(String... command) {
        Process javac = Runtime.getRuntime().exec(command)
        javac.consumeProcessErrorStream(System.err)
        javac.consumeProcessOutputStream(System.out)
        javac.waitFor()

        if (javac.exitValue() == 0) {
            return;
        }

        fail("Error executing command: " + command.join(" "))
    }

    /**
     * Tester eksekvering med default parametere
     */
    @Test
    void testDefaultConfiguration() {
        File outputPath = file('gen/source')
        File resourcePath = file('src/main/resources')

        //eksempel-kildekode
        File javaFile = WsDocgenTestutilFilewriter.writeSimpleDemoServiceWSBean(file('src/main/java'))
        File xslt = WsDocgenTestutilFilewriter.writeSimpleXSLT(resourcePath)

        //utfører annotasjonsprosessering
        outputPath.mkdirs()
        test(
            "javac",
            "-encoding", "UTF-8",
            "-proc:only",
            "-processor", WSDocProcessor.class.getName(),
            "-processorpath", processorPath,
            "-classpath", classpath,
            "-sourcepath", resourcePath.toString(),
            "-d", outputPath.toString(), //d = generated class files

            "-Axslt=${xslt}", //xslt file
            javaFile.toString()
        )


        //tester resultat
        File file = new File(outputPath, 'TestService.html')
        log.debug("Generert html: \n{}", contentOf(file))

        //leser inn html dokumentasjon som xml - dette steget validerer derfor html-koden
        GPathResult html = parseXML(file)

        //sjekker innhold
        assertThat(html.head.title.text() as String).isEqualTo('TestServiceWS')
        assertThat(html.body.h1[0].text() as String).isEqualTo('name=TestServiceWS')
        assertThat(html.body.h1[1].text() as String).isEqualTo('description=Bla bla bla beskrivelse av service.')
        assertThat(html.body.h1[2].text() as String).isEqualTo('namespace=http://test.statkart.no/test1')

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

    /**
     * Tester angivelse av {@code javaDocLookupPath}
     */
    @Test
    void testJavaDocLookupPath() {
        File outputPath = file('gen/source')
        File resourcePath = file('src/main/resources')

        //eksempel-kildekode
        File javaFile = writeFileUTF8('src/main/java/TestWSBean.java', """\
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
                """)

        File xslt = writeFileUTF8('minimal.xsl',
            """<?xml version="1.0" encoding="UTF-8"?>
                <xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

                <xsl:output method="text" version="1.0" media-type="text/plain" omit-xml-declaration="yes" />

                <xsl:template match="/services/service">
                  <xsl:for-each select="methods/method/returns/parameter">
                    <xsl:value-of select="type/@javadocPath"/>
                  </xsl:for-each>
                </xsl:template>

                </xsl:stylesheet>
                """)

        //utfører annotasjonsprosessering
        outputPath.mkdirs()
        test(
            "javac",
            "-encoding", "UTF-8",
            "-proc:only",
            "-processor", WSDocProcessor.class.getName(),
            "-processorpath", processorPath,
            "-classpath", classpath,
            "-sourcepath", resourcePath.toString(),
            "-d", outputPath.toString(), //d = generated class files

            "-Axslt=${xslt}", //xslt file
            "-AjavaDocLookupPath=../uniktNavn/for/test/index.html", //lookup path
            javaFile.toString()
        )


        //tester resultat
        File file = new File(outputPath, 'TestService.html')

        //sjekker innhold
        assertThat(contentOf(file))
            .contains('../uniktNavn/for/test/index.html?') //forventer å finne denne i output
    }

    /**
     * Tester bruk av primitiver
     */
    @Test
    void testPrimitives() {
        File outputPath = file('gen/source')
        File resourcePath = file('src/main/resources')

        // eksempel-kildekode
        File javaFile = writeFileUTF8('src/main/java/TestWSBean.java', """\
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
                """)

        File xslt = writeFileUTF8('minimal.xsl',
            """<?xml version="1.0" encoding="UTF-8"?>
                <xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

                <xsl:output method="text" version="1.0" media-type="text/plain" omit-xml-declaration="yes" />

                <xsl:template match="/services/service">
                  <xsl:for-each select="methods/method/returns/parameter">
                    <xsl:value-of select="type/@name"/>
                  </xsl:for-each>
                </xsl:template>

                </xsl:stylesheet>
                """)

        //utfører annotasjonsprosessering
        outputPath.mkdirs()
        test(
            "javac",
            "-encoding", "UTF-8",
            "-proc:only",
            "-processor", WSDocProcessor.class.getName(),
            "-processorpath", processorPath,
            "-classpath", classpath,
            "-sourcepath", resourcePath.toString(),
            "-d", outputPath.toString(), //d = generated class files

            "-Axslt=${xslt}", //xslt file
            "-AjavaDocLookupPath=../uniktNavn/for/test/index.html", //lookup path
            javaFile.toString()
        )

        //tester resultat
        File file = new File(outputPath, 'TestService.html')

        assertThat(contentOf(file))
            .contains('long') //forventer å finne denne i output
    }

    /**
     * Tester dokumentasjon av tagger
     */
    @Test
    void testReturnTaglets() {
        File outputPath = file('gen/source')
        File resourcePath = file('src/main/resources')

        // eksempel-kildekode
        File javaFile = writeFileUTF8('src/main/java/TestWSBean.java', """\
                package test1;

                /**
                 * Service Æøå description.
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
                """)

        File xslt = writeFileUTF8('minimal.xsl',
            """<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/">
<html><head><title></title></head>
<body>
    <xsl:apply-templates select="services/service"/>
</body>
</html>
</xsl:template>

<xsl:template match="/services/service">
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
</xsl:template>
</xsl:stylesheet>
                """)


        //utfører annotasjonsprosessering
        outputPath.mkdirs()
        test(
            "javac",
            "-encoding", 'UTF-8',
            "-proc:only",
            "-processor", WSDocProcessor.class.getName(),
            "-processorpath", processorPath,
            "-classpath", classpath,
            "-sourcepath", resourcePath.toString(),
            "-d", outputPath.toString(), //d = generated class files

            "-Axslt=${xslt}", //xslt file
            javaFile.toString()
        )


        //tester resultat
        File file = new File(outputPath, 'TestService.html')
        log.debug("Generert html: \n{}", contentOf(file))

        //leser inn html dokumentasjon som xml - dette steget validerer derfor html-koden
        GPathResult html = parseXML(file)

        //sjekker dokumenterte metoder
        Assert.assertEquals html.body.div[0].div[0].h4[0].text().trim(), 'intToLong', "overskrift"
        Assert.assertEquals html.body.div[0].div[0].ul[0].li[0].p[0].text().trim(), 'the converted value', "dokumentasjon av retur"

        Assert.assertEquals html.body.div[0].div.size(), 1, "forventet antall metoder for service"
    }


    /**
     * Tester dokumentasjon av tagger
     */
    @Test
    void testExceptionTaglets() {
        File outputPath = file('gen/source')
        File resourcePath = file('src/main/resources')

        // eksempel-kildekode
        File javaFile = writeFileUTF8('src/main/java/TestWSBean.java', """\
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
                """)

        File xslt = writeFileUTF8('minimal.xsl',
            """<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/">
<html><head><title></title></head>
<body>
    <xsl:apply-templates select="services/service"/>
</body>
</html>
</xsl:template>


<xsl:template match="/services/service">
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
</xsl:template>
</xsl:stylesheet>
                """)


        //utfører annotasjonsprosessering
        outputPath.mkdirs()
        test(
            "javac",
            "-encoding", "UTF-8",
            "-proc:only",
            "-processor", WSDocProcessor.class.getName(),
            "-processorpath", processorPath,
            "-classpath", classpath,
            "-sourcepath", resourcePath.toString(),
            "-d", outputPath.toString(), //d = generated class files

            "-Axslt=${xslt}", //xslt file
            javaFile.toString()
        )


        //tester resultat
        File file = new File(outputPath, 'TestService.html')
        log.debug("Generert html: \n{}", contentOf(file))

        //leser inn html dokumentasjon som xml - dette steget validerer derfor html-koden
        GPathResult html = parseXML(file)

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

    /**
     * Tester dokumentasjon av tagger
     */
    @Test
    void testParamTaglets() {
        File outputPath = file('gen/source')
        File resourcePath = file('src/main/resources')

        // eksempel-kildekode
        File javaFile = writeFileUTF8('src/main/java/TestWSBean.java', """\
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
                """)

        File xslt = writeFileUTF8('minimal.xsl',
            """<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/">
<html><head><title></title></head>
<body>
    <xsl:apply-templates select="services/service"/>
</body>
</html>
</xsl:template>

<xsl:template match="/services/service">
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
</xsl:template>
</xsl:stylesheet>
                """)


        //utfører annotasjonsprosessering
        outputPath.mkdirs()
        test(
            "javac",
            "-encoding", "UTF-8",
            "-proc:only",
            "-processor", WSDocProcessor.class.getName(),
            "-processorpath", processorPath,
            "-classpath", classpath,
            "-sourcepath", resourcePath.toString(),
            "-d", outputPath.toString(), //d = generated class files

            "-Axslt=${xslt}", //xslt file
            javaFile.toString()
        )


        //tester resultat
        File file = new File(outputPath, 'TestService.html')
        log.debug("Generert html: \n{}", contentOf(file))

        //leser inn html dokumentasjon som xml - dette steget validerer derfor html-koden
        GPathResult html = parseXML(file)

        //sjekker dokumenterte metoder
        Assert.assertEquals html.body.div[0].div[0].p[0].text().trim(), 'Intended for asserting a conversion.', "forventet dokumentasjon"
        Assert.assertEquals html.body.div[0].div[0].h4[0].text().trim(), 'intToLong', "forventet overskrift"

        Assert.assertEquals html.body.div[0].div.size(), 1, "forventet antall metoder for service"
    }


    /**
     * SKTOOLS-105
     * Tester generering av index fil
     */
    @Test
    void testSimpleIndex() {
        File outputPath = file('gen/source')
        File resourcePath = file('src/main/resources')

        // eksempel-kildekode
        File javaFile1 = writeFileUTF8('src/main/java/Service1WSBean.java', """\
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
                """)

        File javaFile2 = writeFileUTF8('src/main/java/Service2WSBean.java', """\
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
                      *
                      * @throws Exception ved feil i konvertering
                      */
                     public long intToLong(int value, int base) throws Exception {
                         return 0;
                     }

                     /**
                      * Intended for asserting a conversion.
                      *
                      * @throws Exception ved feil i konvertering
                      * @throws RuntimeException ved andre mystiske feil
                      */
                     public int longToInt(long value, int base) throws RuntimeException, Exception {
                         return 0;
                     }
                 }
                """)

        File xslt = writeFileUTF8('minimal.xsl',
            """<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/">
<html>
<head><title></title></head>
<body>
  <xsl:apply-templates/>
  </body></html>
</xsl:template>

<xsl:template match="services/service">
   <div>name=<xsl:value-of select="@name"/></div>
   <div>href=<xsl:value-of select="@href"/></div>
   <div>description=<xsl:value-of select="description"/></div>
</xsl:template>

</xsl:stylesheet>
                """)


        File indexXslt = writeFileUTF8('index.xsl',
            """<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/">
<html><head><title></title></head>
<body>
    <h1>Services:</h1>
    <xsl:apply-templates select="services/service"/>
</body>
</html>
</xsl:template>

<xsl:template match="services/service">
 <div>
   <a href="{@href}">
        <span>name=<xsl:value-of select="@name"/></span>
        <span>description=<xsl:value-of select="description"/></span>
   </a>
 </div>
</xsl:template>

</xsl:stylesheet>
                """)


        //utfører annotasjonsprosessering
        outputPath.mkdirs()
        test(
            "javac",
            "-encoding", "UTF-8",
            "-proc:only",
            "-processor", WSDocProcessor.class.getName(),
            "-processorpath", processorPath,
            "-classpath", classpath,
            "-sourcepath", resourcePath.toString(),
            "-d", outputPath.toString(), //d = generated class files

            "-Axslt=${xslt}", //xslt file
            "-AindexXslt=${indexXslt}", //xslt file for generating index
            javaFile1.toString(),
            javaFile2.toString()
        )


        //tester resultat
        file("$outputPath/index.html").with { File file ->

            log.debug("Generert html: \n{}", contentOf(file))

            //leser inn html dokumentasjon som xml - dette steget validerer derfor html-koden
            GPathResult html = parseXML(file)

            //sjekker innhold
            assertThat(html.body.div[0].a[0].@href.text() as String).isEqualTo("TestService1.html")
            assertThat(html.body.div[0].a[0].span[0].text() as String).isEqualTo("name=TestServiceWS")
            assertThat(html.body.div[0].a[0].span[1].text() as String).isEqualTo("description=Ping test service.")


            assertThat(html.body.div[1].a[0].@href.text() as String).isEqualTo("TestService2.html")
            assertThat(html.body.div[1].a[0].span[0].text() as String).isEqualTo("name=TestServiceWS")
            assertThat(html.body.div[1].a[0].span[1].text() as String).isEqualTo("description=Inception service.")
        }


        (1..2).each { int idx ->
            file("$outputPath/TestService${idx}.html").with { File file ->

                log.debug("Generert html: \n{}", contentOf(file))

                //leser inn html dokumentasjon som xml - dette steget validerer derfor html-koden
                GPathResult html = parseXML(file)

                //sjekker innhold
                assertThat(html.body.div[0].text() as String).isEqualTo("name=TestServiceWS")
                assertThat(html.body.div[1].text() as String).isEqualTo("href=TestService${idx}.html")
            }
        }
    }


    /**
     * SKTOOLS-106
     * Tester inline @code taglet
     */
    @Test
    void testCodeTaglets() {
        File outputPath = file('gen/source')
        File resourcePath = file('src/main/resources')

        // eksempel-kildekode
        File javaFile = writeFileUTF8('src/main/java/TestWSBean.java', """\
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
                """)

        File xslt = writeFileUTF8('minimal.xsl',
            """<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/">
<html><head><title></title></head>
<body>
    <xsl:apply-templates select="services/service"/>
</body>
</html>
</xsl:template>

<xsl:template match="/services/service">
 <div>
   <p>description=<xsl:value-of select="description"/></p>
   <p>formatted description=<xsl:apply-templates select="description"/></p>
 </div>
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
        <xsl:attribute name="title">DEBUG: span with escaped contents!</xsl:attribute>
        <xsl:apply-templates mode="escapedText" />
      </xsl:when>

      <xsl:otherwise>
        <xsl:attribute name="title">DEBUG: normal span element</xsl:attribute>
        <xsl:apply-templates mode="noEscapedText"/>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:element>
</xsl:template>

<xsl:template match="text()" mode="noEscapedText">
    <xsl:value-of select="." disable-output-escaping="yes" />
</xsl:template>


</xsl:stylesheet>
                """)


        //utfører annotasjonsprosessering
        outputPath.mkdirs()
        test(
            "javac",
            "-encoding", "UTF-8",
            "-proc:only",
            "-processor", WSDocProcessor.class.getName(),
            "-processorpath", processorPath,
            "-classpath", classpath,
            "-sourcepath", resourcePath.toString(),
            "-d", outputPath.toString(), //d = generated class files

            "-Axslt=${xslt}", //xslt file
            javaFile.toString()
        )


        //tester resultat
        File file = new File(outputPath, 'TestService.html')
        log.debug("Generert html: \n{}", contentOf(file))

        //leser inn html dokumentasjon som xml - dette steget validerer derfor html-koden
        GPathResult html = parseXML(file)

        //sjekker innhold
        assertThat(html.body.div[0].p[0].text().trim() as String).isEqualTo('description=Service taglet description.')
        assertThat(html.body.div[0].p[1].span[0].text().trim() as String).isEqualTo('taglet')
        assertThat(html.body.div[0].p[1].text() as String).isEqualToIgnoringWhitespace('formatted description=Service taglet description.')
    }



    /**
     * SKTOOLS-107
     * Regresjonstest av return tag navn
     */
    @Test
    void testReturnName() {
        File outputPath = file('gen/source')
        File resourcePath = file('src/main/resources')

        // eksempel-kildekode
        File javaFile = writeFileUTF8('src/main/java/TestWSBean.java', """\
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
                """)

        File xslt = writeFileUTF8('minimal.xsl',
            """<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/">
<html><head><title></title></head>
<body>
    <xsl:apply-templates select="services/service"/>
</body>
</html>
</xsl:template>

<xsl:template match="/services/service">
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
</xsl:template>
</xsl:stylesheet>
                """)


        //utfører annotasjonsprosessering
        outputPath.mkdirs()
        test(
            "javac",
            "-encoding", "UTF-8",
            "-proc:only",
            "-processor", WSDocProcessor.class.getName(),
            "-processorpath", processorPath,
            "-classpath", classpath,
            "-sourcepath", resourcePath.toString(),
            "-d", outputPath.toString(), //d = generated class files

            "-Axslt=${xslt}", //xslt file
            javaFile.toString()
        )



        //tester resultat
        File file = new File(outputPath, 'TestService.html')
        log.debug("Generert html: \n{}", contentOf(file))

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


    /**
     * Tester formatering med inline taglets
     * @since 1.3 - SKTOOLS-108
     */
    @Test
    void testInlineTaglets() {
        File outputPath = file('gen/source')
        File resourcePath = file('src/main/resources')

        // eksempel-kildekode
        File javaFile = writeFileUTF8('src/main/java/TestWSBean.java', """\
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
                """)

        File xslt = writeFileUTF8('minimal.xsl',
            """<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/">
<html><head><title></title></head>
<body>
    <xsl:apply-templates select="services/service"/>
</body>
</html>
</xsl:template>

<xsl:template match="/services/service">
   <xsl:comment>description with no escaped javadoc: </xsl:comment>
   <div>formatted-description=<xsl:apply-templates select="description"/></div>

   <xsl:comment>description without formatting: </xsl:comment>
   <div>unformatted-description=<xsl:value-of select="description"/></div>

    <div>
    <xsl:comment>methods...</xsl:comment>
        <xsl:for-each select="methods">
            <xsl:apply-templates />
        </xsl:for-each>
    <xsl:comment>end methods...</xsl:comment>
    </div>
</xsl:template>

<xsl:template match="method">
        <h4><xsl:value-of select="@name"/></h4>
        <div><xsl:apply-templates select="description"/></div>

        <h5>Output</h5>
        <ul>
          <xsl:for-each select="returns/parameter">
            <li>
               <span><xsl:value-of select="@name"/></span>
               <div><xsl:apply-templates select="description"/></div>
            </li>
          </xsl:for-each>
        </ul>
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
        <xsl:attribute name="title">DEBUG: span with escaped contents!</xsl:attribute>
        <xsl:apply-templates mode="#default" />
      </xsl:when>

      <xsl:otherwise>
        <xsl:attribute name="title">DEBUG: normal span element</xsl:attribute>
        <xsl:apply-templates mode="noEscapedText"/>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:element>
</xsl:template>

<xsl:template match="text()" mode="noEscapedText">
    <xsl:value-of select="." disable-output-escaping="yes" />
</xsl:template>

</xsl:stylesheet>
                """)


        //utfører annotasjonsprosessering
        outputPath.mkdirs()
        test(
            "javac",
            "-encoding", "UTF-8",
            "-proc:only",
            "-processor", WSDocProcessor.class.getName(),
            "-processorpath", processorPath,
            "-classpath", classpath,
            "-sourcepath", resourcePath.toString(),
            "-d", outputPath.toString(), //d = generated class files

            "-Axslt=${xslt}", //xslt file
            javaFile.toString()
        )


        //tester resultat
        File file = new File(outputPath, 'TestService.html')
        log.debug("Generert html: \n{}", contentOf(file))

        //leser inn html dokumentasjon som xml - dette steget validerer derfor html-koden
        GPathResult html = parseXML(file)

        //sjekker innhold - example with no escaped text
        Assert.assertEquals html.body.div[0].text(), 'formatted-description=Service description. Bold sentence.wrapped text test of GBOK-4872', "plaintext service description"
        Assert.assertEquals html.body.div[0].span[0].text(), 'Bold sentence.', "{@bold ...} turns into <div>"
        Assert.assertEquals html.body.div[0].p.text(), 'wrapped text test of GBOK-4872', "nested <p>"
        Assert.assertEquals html.body.div[0].p.span[0].text(), 'GBOK-4872', "{@code GBOK-4872} turns into <div>"

        //sjekker innhold - example of escaped text
        assertThat(html.body.div[1].text() as String).isEqualToIgnoringWhitespace('unformatted-description=Service description. Bold sentence.<p>wrapped text test of GBOK-4872</p>')

        //sjekker dokumenterte metoder
        Assert.assertEquals html.body.div[2].h4.size(), 1, "forventet antall metoder for service"

        Assert.assertEquals html.body.div[2].h4[0].text().trim(), 'intToLong', "forventet overskrift"
        Assert.assertEquals html.body.div[2].div[0].text().trim().replaceAll("\\s+"," "), 'Bold sentence. Intended for asserting a conversion. testlist', "forventet dokumentasjon"

        Assert.assertEquals html.body.div[2].div[0].span[0].@class.text(), 'javadoc_tag_bold', "forventet CSS.class"
        Assert.assertEquals html.body.div[2].div[0].span[0].text().trim(), 'Bold sentence.', "forventet tekst for span"

        Assert.assertEquals html.body.div[2].div[0].ul[0].li[0].text().trim(), 'testlist', "forventet tekst for li"

        Assert.assertEquals html.body.div[2].ul[0].li[0].span[0].text(), 'return', "forventet tekst for retur"
        Assert.assertEquals html.body.div[2].ul[0].li[0].div[0].text().trim().replaceAll("\\s+"," "), 'value typed as long<encoded>', "forventet dokumentasjon av retur" //groovy substituerer &gt; og andre entiteter...
        Assert.assertEquals html.body.div[2].ul[0].li[0].div[0].span[0].text().trim(), 'long', "forventet formatert dokumentasjon av retur"
        Assert.assertEquals html.body.div[2].ul[0].li[0].div[0].span[1].text().trim(), '<encoded>', "forventet formatert dokumentasjon av retur" //groovy substituerer &gt; og andre entiteter...
    }

    /**
     * Parameter description skal ha formatert description
     * med inline taglet.
     * @since 1.4 - SKTOOLS-134
     */
    @Test
    void parameterCanHaveFormattedDescription() {
        File outputPath = file('gen/source')
        File resourcePath = file('src/main/resources')

        // eksempel-kildekode
        File javaFile = writeFileUTF8('src/main/java/TestWSBean.java', """\
                package test1;

                 @javax.jws.WebService(
                         name = "TestService",
                         serviceName = "TestServiceWS",
                         targetNamespace = "http://test.statkart.no/test1")
                 public class TestWSBean {

                     /**
                     * @param value value in {@code base} system
                     * @return value typed as {@code long} {@code <encoded>}
                     */
                     public long intToLong(int value, int base) {
                         return 0;
                     }
                 }
                """)

        File xslt = writeFileUTF8('minimal.xsl',
            """<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/">
<html><head><title></title></head>
<body>
    <xsl:apply-templates select="services/service"/>
</body>
</html>
</xsl:template>

<xsl:template match="/services/service">
    <div>
        <xsl:for-each select="methods/method">
        <div>
            <h4><xsl:value-of select="@name"/></h4>

            <h5>Input</h5>
            <ul>
                <xsl:for-each select="parameters/parameter">
                <li>
                   <span><xsl:value-of select="@name"/></span>
                   <p><xsl:apply-templates select="description"/></p>
                </li>
                </xsl:for-each>
            </ul>

        </div>
        </xsl:for-each>
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
        <xsl:attribute name="title">DEBUG: span with escaped contents!</xsl:attribute>
        <xsl:apply-templates mode="#default" />
      </xsl:when>

      <xsl:otherwise>
        <xsl:attribute name="title">DEBUG: normal span element</xsl:attribute>
        <xsl:apply-templates mode="noEscapedText"/>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:element>
</xsl:template>

<xsl:template match="text()" mode="noEscapedText">
    <xsl:value-of select="." disable-output-escaping="yes" />
</xsl:template>

</xsl:stylesheet>
                """)


        //utfører annotasjonsprosessering
        outputPath.mkdirs()
        test(
            "javac",
            "-encoding", "UTF-8",
            "-proc:only",
            "-processor", WSDocProcessor.class.getName(),
            "-processorpath", processorPath,
            "-classpath", classpath,
            "-sourcepath", resourcePath.toString(),
            "-d", outputPath.toString(), //d = generated class files

            "-Axslt=${xslt}", //xslt file
            javaFile.toString()
        )


        File file = new File(outputPath, 'TestService.html')
        log.debug("Generert html: \n{}", contentOf(file))

        //leser inn html dokumentasjon som xml - dette steget validerer derfor html-koden
        GPathResult html = parseXML(file)

        Assert.assertEquals html.body.div[0].div[0].h4[0].text().trim(), 'intToLong', "forventet overskrift"
        Assert.assertEquals html.body.div[0].div[0].h5[0].text().trim(), 'Input', "forventet overskrift"

        Assert.assertEquals html.body.div[0].div[0].ul[0].li[0].span[0].text(), 'value', "forventet parameternavn"
        Assert.assertEquals html.body.div[0].div[0].ul[0].li[0].p[0].text().trim(), 'value in base system', "forventet tekstlig dokumentasjon"
        Assert.assertEquals html.body.div[0].div[0].ul[0].li[0].p[0].span[0].text().trim(), 'base', "{@code base} wraps to <span>"
    }

    public static GPathResult parseXML(File file) {
        XmlSlurper slurper = XmlTestUtils.defaultXmlSlurper()
        return slurper.parse(file)
    }

}
