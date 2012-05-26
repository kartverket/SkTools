package no.statkart.sktools.interfaceservice;

import no.statkart.sktools.interfaceservice.domain.SimpleClass;


/**
 * Beskrivelse av service i interface.
 */
public interface InterfaceServiceInterface {

    @javax.jws.WebMethod
    SimpleClass ping(String value);

    /**
     * Returnerer ikke noe
     */
    @javax.jws.WebMethod
    void interfaceDocumentedMethod();


}
             