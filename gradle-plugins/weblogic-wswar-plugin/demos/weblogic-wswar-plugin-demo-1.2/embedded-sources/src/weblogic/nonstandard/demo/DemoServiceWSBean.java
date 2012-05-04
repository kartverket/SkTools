package demo;


/**
 * Bla bla bla beskrivelse av service.
 */
@javax.jws.WebService(
        name = "TestService",
        serviceName = "TestServiceWS",
        targetNamespace = "http://test.statkart.no/demo")
public class DemoServiceWSBean {

    /**
     * Returnerer PONG *
     */
    @javax.jws.WebMethod
    public String ping() {
        return new PingHelper().buildPong();
    }

}