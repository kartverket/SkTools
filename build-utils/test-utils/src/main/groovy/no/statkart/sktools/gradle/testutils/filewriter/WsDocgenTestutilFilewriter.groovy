package no.statkart.sktools.gradle.testutils.filewriter

import no.statkart.sktools.gradle.testutils.ProjectHelper

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


                        //setter targetnamespace her da det ikke er definert for SimpleClass...
                        @javax.jws.WebResult(targetNamespace = "http://sktools.statkart.no/test/service/interfaceservice/domain")
                        public SimpleClass ping(String value) {
                            return new SimpleClass(value);
                        }


                        public void interfaceDocumentedMethod() {
                            ;
                        }


                    }
             """
            }
            return file
        }

        return generatedFiles;
    }



}
