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
             