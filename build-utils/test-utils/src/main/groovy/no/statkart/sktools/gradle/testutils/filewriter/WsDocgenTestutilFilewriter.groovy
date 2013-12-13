package no.statkart.sktools.gradle.testutils.filewriter

import no.statkart.sktools.gradle.testutils.ProjectHelper

/**
 * Statiske understøttende hjelpemetoder for generering av kildekode for bruk i testing.
 *
 * @author Leif Lislegård
 */
class WsDocgenTestutilFilewriter extends AbstractTestutilFilewriter {

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
    public static Collection<File> writeSimpleDemoServiceWSBean(ProjectHelper projectHelper, String targetPath) {
        ArrayList<File> generatedFiles = new ArrayList<File>()

        generatedFiles.add projectHelper.project.file(targetPath + '/no/statkart/sktools/test/SimpleDemoServiceWSBean.java').with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.print """
                     package no.statkart.sktools.test;

                     /**
                      * Bla bla bla beskrivelse av service.
                      */
                     @javax.jws.WebService(
                         name = "TestService",
                         serviceName = "TestServiceWS",
                         targetNamespace = "http://test.statkart.no/test1")
                     public class SimpleDemoServiceWSBean {

                         /** Returnerer PONG **/
                         @javax.jws.WebMethod
                         public String ping() {
                             return "PONG";
                         }

                         /** Returnerer ikke noe */
                         @javax.jws.WebMethod
                         public void noPing() {

                         }

                         /** Eksponeres ikke */
                         @javax.jws.WebMethod( exclude = true)
                         public void secret() {

                         }

                     }
                """
            }
            return file
        }

        return generatedFiles
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
    public static Collection<File> writeInterfaceServiceWSBean(ProjectHelper projectHelper, String targetPath) {
        ArrayList<File> generatedFiles = new ArrayList<File>()

        generatedFiles.add projectHelper.project.file(targetPath + '/no/statkart/sktools/interfaceservice/domain/SimpleClass.java').with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.print """
                    package no.statkart.sktools.interfaceservice.domain;

                    import javax.xml.bind.annotation.XmlAccessType;
                    import javax.xml.bind.annotation.XmlAccessorType;
                    import javax.xml.bind.annotation.XmlElement;
                    import javax.xml.bind.annotation.XmlType;

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
                 """
            }
            return file
        }


        generatedFiles.add projectHelper.project.file(targetPath + '/no/statkart/sktools/interfaceservice/InterfaceServiceInterface.java').with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.print """
                     package no.statkart.sktools.interfaceservice;

                     import no.statkart.sktools.interfaceservice.domain.SimpleClass;


                     /**
                      * Beskrivelse av service i interface.
                      */
                     public interface InterfaceServiceInterface {

                         @javax.jws.WebMethod
                         SimpleClass ping(String value);

                         /** Returnerer ikke noe */
                         @javax.jws.WebMethod
                         void interfaceDocumentedMethod();

                         @javax.jws.WebMethod
                         Character stringToChar(String value);

                     }
                 """
            }
            return file
        }


        generatedFiles.add projectHelper.project.file(targetPath + '/no/statkart/sktools/interfaceservice/InterfaceServiceWSBean.java').with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.print """
                    package no.statkart.sktools.interfaceservice;

                    import no.statkart.sktools.interfaceservice.domain.SimpleClass;

                    /**
                    * Beskrivelse av service i implementasjon.
                    */
                    @javax.jws.WebService(
                            name = "InterfaceService",
                            serviceName = "InterfaceServiceWS",
                            targetNamespace = "http://sktools.statkart.no/test/service/interfaceservice")
                    public class InterfaceServiceWSBean implements InterfaceServiceInterface {


                        /**
                        * TargetNamespace definert i implementasjonsklassen. Ikke i SimpleClass...
                        **/
                        @javax.jws.WebResult(targetNamespace = "http://sktools.statkart.no/test/service/interfaceservice/domain")
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
             """
            }
            return file
        }

        return generatedFiles;
    }



    /**
     * Skriver XSLT eksempel-skjema til disk.
     *
     * @since 1.3
     */
    public static File writeSimpleXSLT(ProjectHelper projectHelper, String targetPath, String filename = 'transform.xslt') {
        ArrayList<File> generatedFiles = new ArrayList<File>()

        generatedFiles.add projectHelper.project.file(targetPath + '/' + filename).with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.print """
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

   name=<span><xsl:value-of select="@name"/></span>
   description=<span><xsl:value-of select="@description"/></span>
   namespace=<span><xsl:value-of select="@namespace"/></span>

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
            <p><xsl:value-of select="@description"/></p>

            <h5>Input</h5>
            <ul>
              <xsl:for-each select="parameters/parameter">
                <li>
                   <span><xsl:value-of select="@name"/></span>
                   <span><xsl:value-of select="@description"/></span>
                   <div>
                     <span><xsl:value-of select="type/@name"/></span>
                     <span><xsl:value-of select="type/@namespace"/></span>
                     <span><xsl:value-of select="type/@javadocPath"/></span>
                   </div>
                </li>
              </xsl:for-each>
            </ul>

            <h5>Response</h5>
            <ul>
              <xsl:for-each select="returns/parameter">
                <li>
                   <span><xsl:value-of select="@name"/></span>
                   <span><xsl:value-of select="@description"/></span>
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
                   <span><xsl:value-of select="@description"/></span>
                   <div>
                     <span><xsl:value-of select="type/@name"/></span>
                     <span><xsl:value-of select="type/@namespace"/></span>
                     <span><xsl:value-of select="type/@javadocPath"/></span>
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
            return file
        }

        return generatedFiles.iterator().next();
    }

}
