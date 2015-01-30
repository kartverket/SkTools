package no.statkart.test.test2;


@javax.jws.WebService(
        name = "Test2Service",
        serviceName = "Test2ServiceWS_v1",
        targetNamespace = "http://test.statkart.no/test2")
public class DemoMedWSIServiceWSBean implements DemoMedWSIServiceWSI{

    @javax.jws.WebMethod
    public void noPing() {

    }

    @javax.jws.WebMethod
    public String ping() {
        return "PONG";
    }

}