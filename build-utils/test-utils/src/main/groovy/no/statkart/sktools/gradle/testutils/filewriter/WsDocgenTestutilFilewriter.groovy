package no.statkart.sktools.gradle.testutils.filewriter

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * Statiske understøttende hjelpemetoder for generering av kildekode for bruk i testing.
 *
 * @author Leif Lislegård
 */
class WsDocgenTestutilFilewriter {

    /**
     * Skriver kildekode for en simpel testservice implementasjon (WebService) til fil.
     * <br>
     * <br>Klasse: {@code no.statkart.sktools.test.SimpleDemoServiceWSBean}
     * <br>targetNamespace: {@code http://test.statkart.no/test1}  TestService
     * <p>
     * Testservice har to metoder.
     *
     * <p><b>
     * PS: Merk at service navn og klassenavn divergerer!
     */
    public static File writeSimpleDemoServiceWSBean(File targetPath) {
        File file = new File(targetPath, '/no/statkart/sktools/test/SimpleDemoServiceWSBean.java')
        file.parentFile.mkdirs()
        Files.write(file.toPath(), ["""\
                     package no.statkart.sktools.test;

                     /**
                      * Bla bla bla beskrivelse av service.
                      */
                     @jakarta.jws.WebService(
                         name = "TestService",
                         serviceName = "TestServiceWS",
                         targetNamespace = "http://test.statkart.no/test1")
                     public class SimpleDemoServiceWSBean {

                         /** Returnerer PONG **/
                         @jakarta.jws.WebMethod
                         public String ping() {
                             return "PONG";
                         }

                         /** Returnerer ikke noe */
                         @jakarta.jws.WebMethod
                         public void noPing() {

                         }

                         /** Returnerer noen bytes **/
                         @jakarta.jws.WebMethod
                         public byte[] binary() {
                             return new byte[]{(byte) 1, (byte) 2};
                         }

                         /** Eksponeres ikke */
                         @jakarta.jws.WebMethod( exclude = true)
                         public void secret() {

                         }

                     }
                """], StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE)

        return file;
    }

    /**
     * Skriver kildekode for en testservice til fil.
     * Merk at webservicen har dokumentasjon både på interface og implementasjonen.
     * <p>
     * <p>
     *  Service : {http://sktools.statkart.no/test/service/interfaceservice} InterfaceService
     * <p>
     *  Java interface : no.statkart.sktools.interfaceservice.InterfaceServiceInterface - med javadoc på klasse
     * <p>
     *  Domeneklasse : no.statkart.sktools.interfaceservice.domain.SimpleClass - med javadoc på klasse
     *
     * Testservice har to metoder definert i interface, samt domenemodell (webservice lag).
     *
     * <p><b>
     */
    public static void writeInterfaceServiceWSBean(File targetPath) {

        new File(targetPath, '/no/statkart/sktools/interfaceservice/domain/SimpleClass.java').with { File file ->
            file.parentFile.mkdirs()
            Files.write(file.toPath(), ["""\
                    package no.statkart.sktools.interfaceservice.domain;

                    import jakarta.xml.bind.annotation.XmlAccessType;
                    import jakarta.xml.bind.annotation.XmlAccessorType;
                    import jakarta.xml.bind.annotation.XmlElement;
                    import jakarta.xml.bind.annotation.XmlType;

                    /* not documented */
                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "SimpleClass")
                    public class SimpleClass {

                        private String value;

                        public SimpleClass(String value) {
                            setValue(value);
                        }

                        public String getValue() {
                            return value;
                        }

                        public void setValue(String value) {
                            this.value = value;
                        }

                    }
                """], StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
            return file
        }


        new File(targetPath, '/no/statkart/sktools/interfaceservice/InterfaceServiceInterface.java').with { File file ->
            file.parentFile.mkdirs()
            Files.write(file.toPath(), ["""\
                     package no.statkart.sktools.interfaceservice;

                     import no.statkart.sktools.interfaceservice.domain.SimpleClass;


                     /**
                      * Beskrivelse av service i interface.
                      */
                     public interface InterfaceServiceInterface {

                         @jakarta.jws.WebMethod
                         SimpleClass ping(String value);

                         /** Returnerer ikke noe */
                         @jakarta.jws.WebMethod
                         void interfaceDocumentedMethod();

                         @jakarta.jws.WebMethod
                         Character stringToChar(String value);

                     }
                """], StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
            return file
        }


        new File(targetPath, '/no/statkart/sktools/interfaceservice/InterfaceServiceWSBean.java').with { File file ->
            file.parentFile.mkdirs()
            Files.write(file.toPath(), ["""\
                    package no.statkart.sktools.interfaceservice;

                    import no.statkart.sktools.interfaceservice.domain.SimpleClass;

                    /**
                    * Beskrivelse av service i implementasjon.
                    */
                    @jakarta.jws.WebService(
                            name = "InterfaceService",
                            serviceName = "InterfaceServiceWS",
                            targetNamespace = "http://sktools.statkart.no/test/service/interfaceservice")
                    public class InterfaceServiceWSBean implements InterfaceServiceInterface {


                        /**
                        * TargetNamespace definert i implementasjonsklassen. Ikke i SimpleClass...
                        **/
                        @jakarta.jws.WebResult(targetNamespace = "http://sktools.statkart.no/test/service/interfaceservice/domain")
                        public SimpleClass ping(String value) {
                            return new SimpleClass(value);
                        }


                        public void interfaceDocumentedMethod() {
                            ;
                        }

                        public Character stringToChar(String value) {
                            return null;
                        }

                    }
                """], StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
            return file
        }
    }


    /**
     * Skriver XSLT eksempel-skjema til disk.
     *
     * @since 1.3
     */
    public static File writeSimpleXSLT(File targetPath, String filename = 'transform.xslt') {
        File file = new File(targetPath, filename)
        file.parentFile.mkdirs()
        Files.write(file.toPath(), ["""\
<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" indent="yes"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  media-type="text/html"
  omit-xml-declaration="no" />

<xsl:template match="/services/service">

<html>
<head>
  <title><xsl:value-of select="@name"/></title>
</head>

<body>

   <h1>name=<xsl:value-of select="@name"/></h1>
   <h1>description=<xsl:value-of select="description"/></h1>
   <h1>namespace=<xsl:value-of select="@namespace"/></h1>

    <div>
        <ul>
            <xsl:for-each select="methods/method">
                <xsl:sort select="@name"/><!-- ordered TOC by name -->
                <li><a href=""><xsl:value-of select="@name"/></a></li>
            </xsl:for-each>
        </ul>
    </div>
    <div>
        <xsl:for-each select="methods/method">
          <div>
            <h4><xsl:value-of select="@name"/></h4>
            <p><xsl:value-of select="description"/></p>

            <xsl:if test="count(parameters/parameter) gt 0">
                <h5>Input</h5>
                <ul>
                  <xsl:for-each select="parameters/parameter">
                    <li>
                       <span><xsl:value-of select="@name"/></span>
                       <p><xsl:value-of select="description"/></p>
                       <div>
                         <span><xsl:value-of select="type/@name"/></span>
                         <span><xsl:value-of select="type/@namespace"/></span>
                         <span><xsl:value-of select="type/@javadocPath"/></span>
                       </div>
                    </li>
                  </xsl:for-each>
                </ul>
            </xsl:if>

            <xsl:if test="(count(returns/parameter) + count(exceptions/exception)) gt 0">
                <h5>Response</h5>
                <ul>
                  <xsl:for-each select="returns/parameter">
                    <li>
                       <span><xsl:value-of select="@name"/></span>
                       <p><xsl:value-of select="description"/></p>
                       <div>
                         <span><xsl:value-of select="type/@name"/></span>
                         <span><xsl:value-of select="type/@namespace"/></span>
                         <span><xsl:value-of select="type/@javadocPath"/></span>
                       </div>
                    </li>
                  </xsl:for-each>
                  <xsl:for-each select="exceptions/exception">
                    <li>
                       <span><xsl:value-of select="@name"/></span>
                       <p><xsl:value-of select="description"/></p>
                       <div>
                         <span><xsl:value-of select="type/@name"/></span>
                         <span><xsl:value-of select="type/@namespace"/></span>
                         <span><xsl:value-of select="type/@javadocPath"/></span>
                       </div>
                    </li>
                  </xsl:for-each>
                </ul>
            </xsl:if>

          </div>
        </xsl:for-each>
    </div>

</body></html>

</xsl:template>

</xsl:stylesheet>

                """], StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE)

        return file
    }

}
