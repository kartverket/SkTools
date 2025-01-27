package exceptiondemo01;

import exceptiondemo01.exception.*;

@jakarta.jws.WebService(
        name = "ExceptionService2",
        serviceName = "ExceptionService2WS",
        targetNamespace = "http://test.statkart.no/exceptiondemo01/displaced/service/service2")
public class ExceptionService2WSBean {

    /**
     * Returnerer PONG
     */
    @jakarta.jws.WebMethod
    public String ping() throws ServiceException {
        return "PONG";
    }

}
