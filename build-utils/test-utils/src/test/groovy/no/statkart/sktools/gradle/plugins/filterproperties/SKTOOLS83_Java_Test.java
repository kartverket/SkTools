package no.statkart.sktools.gradle.plugins.filterproperties;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

/**
 * Test av http://jira.statkart.no:8080/browse/SKTOOLS-83
 *
 *
 * @author Leif Lislegård
 * @since 1.3
 */
public class SKTOOLS83_Java_Test {

    /**
     * Java kilde kompileres som utf-8 (SKTOOLS-163).
     * PS: Denne testen feiler dersom java klassen ligger i groovy source set og groovy kompileres med annen encoding.
     *
     * Verifiserer riktig encoding ved å sammenligne kompilert streng med utf8 escaped tekst.
     */
    @Test
    public void testSpecialChars() {
        final String message = "æÆ øØ åÅ";
        System.out.println(String.format("message:%s", message));
        Assertions.assertThat(message).isEqualTo("\u00e6\u00c6 \u00f8\u00d8 \u00e5\u00c5");
    }

    /**
     * Verifiserer av http://jira.statkart.no:8080/browse/SKTOOLS-83 ved å generere norske tegn i output til testrapport.
     */
    @Test(expectedExceptions = Error.class)
    public void testSpecialChars2() {
        final String message = "æÆ øØ åÅ";
        System.out.println(String.format("message:%s", message));
        throw new Error(message);
    }


}
