package exceptiondemo01.exception;

@jakarta.xml.ws.WebFault(name = "ServiceException", targetNamespace = "http://test.statkart.no/exceptiondemo01/exception")
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
