package no.statkart.test.test1;

/**
 * Bla bla bla beskrivelse av service.
 */
@javax.jws.WebService(
        name = "TestService",
        serviceName = "TestServiceWS_v1",
        targetNamespace = "http://test.statkart.no/service1")
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
            