package no.statkart.test.service.v2;

/**
 * Bla bla bla beskrivelse av service.
 */
@javax.jws.WebService(
        name = "TestService",
        serviceName = "TestServiceWS",
        targetNamespace = "http://test.statkart.no/service/v2"
)
public class DemoServiceWSBean {

    /**
     * Returnerer ikke noe
     */
    @javax.jws.WebMethod
    public void noPing() {

    }

    /**
     * Returnerer PONG *
     */
    @javax.jws.WebMethod
    public String ping() {
        return "PONG";
    }

}
            