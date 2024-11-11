package exceptiondemo01;

import exceptiondemo01.exception.*;

@javax.jws.WebService(
        name = "ExceptionService1",
        serviceName = "ExceptionService1WS",
        targetNamespace = "http://test.statkart.no/exceptiondemo01/service/service1")
public class ExceptionService1WSBean {

    /**
     * Returnerer PONG
     */
    @javax.jws.WebMethod
    public String ping() throws ServiceException {
        return "PONG";
    }

}
