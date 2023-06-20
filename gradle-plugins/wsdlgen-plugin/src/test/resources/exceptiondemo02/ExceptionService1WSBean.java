package exceptiondemo02;

import exceptiondemo02.exception.ServiceException;

@jakarta.jws.WebService(
        name = "ExceptionService1",
        serviceName = "ExceptionService1WS",
        targetNamespace = "http://test.statkart.no/exceptiondemo02/service/service1")
public class ExceptionService1WSBean {

    /**
     * Returnerer PONG
     */
    @jakarta.jws.WebMethod
    public String ping() throws ServiceException {
        return "PONG";
    }

}
