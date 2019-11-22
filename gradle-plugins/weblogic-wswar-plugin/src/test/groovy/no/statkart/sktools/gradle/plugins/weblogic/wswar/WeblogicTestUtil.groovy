package no.statkart.sktools.gradle.plugins.weblogic.wswar

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class WeblogicTestUtil {

    /**
     * Oppretter kildekode til fil for en simpel webservice implementasjon.
     */
    public static void writeDemoServiceWSBean(File targetPath) {
        File file = new File(targetPath, "no/statkart/test/test1/DemoServiceWSBean.java")
        Path destination = file.toPath()

        Files.createDirectories(destination.getParent());
        Files.write(destination, Arrays.asList("""
                    package no.statkart.test.test1;

                    /**
                     * Bla bla bla beskrivelse av service.
                     */
                    @javax.jws.WebService(
                        name = "TestService",
                        serviceName = "TestServiceWS_v1",
                        targetNamespace = "http://test.statkart.no/test1")
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
                """), StandardCharsets.UTF_8,
            StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }


    /**
     * Oppretter kildekode for "exceptiondemo01.exception.ServiceException"
     *
     * @param targetPath for java kildekode
     */
    public static void writeExceptionService01Exceptions(File targetPath) {
        Path exceptionPath = new File(targetPath, "exceptiondemo01/exception").toPath()
        Files.createDirectories(exceptionPath);
        Files.copy(WeblogicTestUtil.class.getResourceAsStream("/demo01/java/exceptiondemo01/exception/ServiceException.java"), exceptionPath.resolve("ServiceException.java"))
        Files.copy(WeblogicTestUtil.class.getResourceAsStream("/demo01/java/exceptiondemo01/exception/ServiceFaultInfo.java"), exceptionPath.resolve("ServiceFaultInfo.java"))
    }

    /**
     * Oppretter kildekode for "exceptiondemo01.ExceptionService1WSBean"
     *
     * @param targetPath for java kildekode
     */
    public static void writeExceptionService01(File targetPath) {
        Path servicePath = new File(targetPath, "exceptiondemo01").toPath()
        Files.createDirectories(servicePath);
        Files.copy(WeblogicTestUtil.class.getResourceAsStream("/demo01/java/exceptiondemo01/ExceptionService1WSBean.java"), servicePath.resolve("ExceptionService1WSBean.java"))
        Files.copy(WeblogicTestUtil.class.getResourceAsStream("/demo01/java/exceptiondemo01/WebConfig.java"), servicePath.resolve("WebConfig.java"))
    }

}
