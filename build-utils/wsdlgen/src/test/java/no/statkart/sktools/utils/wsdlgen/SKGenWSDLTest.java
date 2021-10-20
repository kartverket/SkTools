package no.statkart.sktools.utils.wsdlgen;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class SKGenWSDLTest {
    @Test
    public void destinationErPaakrevd() {
        Assertions.assertThatThrownBy(() -> SKGenWSDL.main("foo"))
            .hasMessage("Missing required option: d");
    }

    @Test
    public void classpathErPaakrevd() {
        Assertions.assertThatThrownBy(() -> SKGenWSDL.main("-d bar"))
            .hasMessage("Classpath not set. Use parameter -cp <value> or environment variable WEB_SERVICE_CLASSPATH.");
    }
}