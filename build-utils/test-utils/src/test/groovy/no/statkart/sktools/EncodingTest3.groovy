package no.statkart.sktools

import org.assertj.core.api.Assertions
import org.testng.annotations.Test

class EncodingTest3 {

    /**
     * Groovy kilde set kompileres som utf-8 (SKTOOLS-163).
     * Verifiserer dette ved å sammenligne kompilert streng med utf8 escaped tekst.
     */
    @Test
    public void encodingForGroovySource() {
        final String message = "æÆ øØ åÅ"
        println "message:${message}"
        Assertions.assertThat(message).isEqualTo('\u00e6\u00c6 \u00f8\u00d8 \u00e5\u00c5');
    }

}
