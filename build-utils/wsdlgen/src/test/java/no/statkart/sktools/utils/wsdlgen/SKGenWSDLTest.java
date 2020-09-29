package no.statkart.sktools.utils.wsdlgen;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class SKGenWSDLTest {
    @Test
    public void paakrevdeParametre() {
        Assertions.assertThatThrownBy(() -> SKGenWSDL.main(new String[0]))
            .hasMessage("Missing required options: cp, d");
    }
}