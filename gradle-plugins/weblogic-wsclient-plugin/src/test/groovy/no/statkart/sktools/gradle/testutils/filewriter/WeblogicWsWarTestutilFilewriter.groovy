package no.statkart.sktools.gradle.testutils.filewriter

import no.statkart.sktools.gradle.testutils.ProjectHelper

/**
 * Statiske understøttende hjelpemetoder for generering av kildekode for bruk i testing.
 *
 * @author Leif Lislegård
 */
class WeblogicWsWarTestutilFilewriter {


    /**
     * Service <b>{http://test.statkart.no/service/pingtns}PingService</b> som skrives til <br />
     *     filen <code>service/PingServiceWSBean.java</code> <br />
     *     med pakkenavn <code>no.statkart.test.service.ping</code> <br />
     */
    public static Collection<File> writePingServiceWSBean(ProjectHelper projectHelper, String targetPath) {
        ArrayList<File> generatedFiles = new ArrayList<File>()

        generatedFiles.add projectHelper.project.file("${targetPath}/service/PingServiceWSBean.java").with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.print """
                    package no.statkart.test.service.ping;

                    /**
                     * Bla bla bla beskrivelse av service.
                     */
                    @javax.jws.WebService(
                        name = "PingService",
                        serviceName = "PingServiceWS",
                        targetNamespace = "http://test.statkart.no/service/pingtns")
                    public class PingServiceWSBean {

                        /** Returnerer PONG **/
                        @javax.jws.WebMethod
                        public String ping() {
                            return "PONG";
                        }

                    }
                """
            }
            return file
        }
        return generatedFiles
    }

    /**
     * Service <b>{http://test.statkart.no/service/demotns}TestService</b> som skrives til <br />
     *     filen <code>service/DemoServiceWSBean.java</code> <br />
     *     med pakkenavn <code>no.statkart.test.service.demo</code> <br />
     */
    public static Collection<File> writeDemoServiceWSBean2(ProjectHelper projectHelper, String targetPath) {
        ArrayList<File> generatedFiles = new ArrayList<File>()

        generatedFiles.add projectHelper.project.file("${targetPath}/service/DemoServiceWSBean.java").with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.print """
                    package no.statkart.test.service.demo;

                    /**
                     * Bla bla bla beskrivelse av service.
                     */
                    @javax.jws.WebService(
                        name = "TestService",
                        serviceName = "TestServiceWS",
                        targetNamespace = "http://test.statkart.no/service/demotns")
                    public class DemoServiceWSBean {

                        /** Returnerer ikke noe */
                        @javax.jws.WebMethod
                        public void noPing() {

                        }

                        /** Returnerer PONG **/
                        @javax.jws.WebMethod
                        public String ping() {
                            return "PONG";
                        }

                    }
                """
            }
            return file
        }
        return generatedFiles
    }


    /**
     * Skriver kildekode til to services med felles exception definisjon.
     *
     * <br>
     * Den ene service har namespace = <b> http://test.statkart.no/exceptiondemo01/service/service1/ExceptionService1 </b>
     * <br>
     * Den andre service har namespace = <b> http://test.statkart.no/exceptiondemo01/displaced/service/service2/ExceptionService2 </b>
     * <br>
     *
     * <p>
     * @see #writeExceptionDemoWithTwoServicesDomain(ProjectHelper, String)
     */
    static Collection<File> writeExceptionDemoWithTwoServicesService(ProjectHelper projectHelper, String targetPath) {
        ArrayList<File> generatedFiles = new ArrayList<File>()

        generatedFiles.add projectHelper.project.file("${targetPath}/exceptiondemo01/ExceptionService1WSBean.java").with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.print """
                    package exceptiondemo01.service1;

                    import exceptiondemo01.exception.*;

                    @javax.jws.WebService(
                        name = "ExceptionService1",
                        serviceName = "ExceptionService1WS",
                        targetNamespace = "http://test.statkart.no/exceptiondemo01/service/service1")
                    public class ExceptionService1WSBean {


                        /** Returnerer PONG **/
                        @javax.jws.WebMethod
                        public String ping() throws ServiceException {
                            return "PONG";
                        }

                    }
                """
            }
            return file
        }

        generatedFiles.add projectHelper.project.file("${targetPath}/exceptiondemo01/ExceptionService2WSBean.java").with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.print """
                    package exceptiondemo01.service2;

                    import exceptiondemo01.exception.*;

                    @javax.jws.WebService(
                        name = "ExceptionService2",
                        serviceName = "ExceptionService2WS",
                        targetNamespace = "http://test.statkart.no/exceptiondemo01/displaced/service/service2")
                    public class ExceptionService2WSBean {

                        /** Returnerer PONG **/
                        @javax.jws.WebMethod
                        public String ping() throws ServiceException {
                            return "PONG";
                        }

                    }
                """
            }
            return file
        }

        return generatedFiles;
    }

    /**
     * Skriver kildekode for exception klasser.
     *
     * <br>
     * Exception har namespace = <b> http://test.statkart.no/exceptiondemo01/exception/ServiceException </b>
     *
     * @see #writeExceptionDemoWithTwoServicesService(ProjectHelper, String)
     */
    static Collection<File> writeExceptionDemoWithTwoServicesDomain(ProjectHelper projectHelper, String targetPath) {
        ArrayList<File> generatedFiles = new ArrayList<File>()

        generatedFiles.add projectHelper.project.file("${targetPath}/exceptiondemo01/exception/ServiceException.java").with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.print """
                    package exceptiondemo01.exception;

                    @javax.xml.ws.WebFault(name = "ServiceException", targetNamespace = "http://test.statkart.no/exceptiondemo01/exception")
                    public class ServiceException extends Exception {

                        /**
                         * Java type that goes as soapenv:Fault detail element.
                         */
                        private ServiceFaultInfo faultInfo;


                        public ServiceException() {
                        }

                        /**
                         * Std constructor in JAX-WS 2.0
                         */
                        public ServiceException(String message, ServiceFaultInfo faultInfo) {
                            super(message);
                            this.faultInfo = faultInfo;
                        }

                        /**
                         * Std constructor in JAX-WS 2.0
                         */
                        public ServiceException(String message, ServiceFaultInfo faultInfo, Throwable cause) {
                            super(message, cause);
                            this.faultInfo = faultInfo;
                        }

                        /**
                         * Std getter for detail element in JAX-WS 2.0
                         */
                        public ServiceFaultInfo getFaultInfo() {
                            return faultInfo;
                        }

                        public void setFaultInfo(ServiceFaultInfo faultInfo) {
                            this.faultInfo = faultInfo;
                        }
                    }
            """
            }
            return file
        }

        generatedFiles.add projectHelper.project.file("${targetPath}/exceptiondemo01/exception/ServiceFaultInfo.java").with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.print """
                    package exceptiondemo01.exception;

                    import javax.xml.bind.annotation.*;


                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "ServiceFaultInfo", propOrder = {
                        "category"
                    })
                    public class ServiceFaultInfo {

                        @XmlElement(required = true)
                        protected String category;

                        public String getCategory() {
                            return category;
                        }

                        public void setCategory(String value) {
                            this.category = value;
                        }
                    }
            """
            }
            return file
        }

        return generatedFiles;
    }

    /**
     * Tom java klasse <code>no.statkart.test.div.Dummy</code> som skrives til <code>div/Dummy.java</code>
     */
    public static Collection<File> writeDummyClass(ProjectHelper projectHelper, String targetPath) {
        ArrayList<File> generatedFiles = new ArrayList<File>()

        generatedFiles.add projectHelper.project.file("${targetPath}/div/Dummy.java").with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.print """
                    package no.statkart.test.div;

                    public class Dummy {

                        public Dummy() {
                        }

                    }
                """
            }
            return file
        }
        return generatedFiles
    }

}
