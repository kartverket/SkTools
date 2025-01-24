package exceptiondemo02;

import exceptiondemo02.exception.ServiceException;

@jakarta.jws.WebService(
        name = "ExceptionService2",
        serviceName = "ExceptionService2WS",
        targetNamespace = "http://test.statkart.no/exceptiondemo02/displaced/service/service2")
public class ExceptionService2WSBean {

    /**
     * Returnerer PONG
     */
    @jakarta.jws.WebMethod
    public String ping() throws ServiceException {
        return "PONG";
    }

}
