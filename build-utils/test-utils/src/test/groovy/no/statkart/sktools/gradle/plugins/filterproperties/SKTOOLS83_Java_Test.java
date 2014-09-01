package no.statkart.sktools.gradle.plugins.filterproperties;

import org.testng.annotations.Test;

/**
 * Test av http://jira.statkart.no:8080/browse/SKTOOLS-83
 *
 * @author Leif LislegÂrd
 * @since 1.3
 */
public class SKTOOLS83_Java_Test {

    @Test(expectedExceptions = RuntimeException.class)
    public void testSpecialChars() {
        final String message = "Ê∆ ¯ÿ Â≈";
        System.out.println(String.format("message:%s", message));
        throw new RuntimeException(message);
    }

    @Test(expectedExceptions = Error.class)
    public void testSpecialChars2() {
        final String message = "Ê∆ ¯ÿ Â≈";
        System.out.println(String.format("message:%s", message));
        throw new Error(message);
    }


}
