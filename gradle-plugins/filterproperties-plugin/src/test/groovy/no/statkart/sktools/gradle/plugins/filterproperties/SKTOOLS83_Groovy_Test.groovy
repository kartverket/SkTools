package no.statkart.sktools.gradle.plugins.filterproperties

import org.testng.annotations.Test

/**
 * Test av http://jira.statkart.no:8080/browse/SKTOOLS-83
 *
 * @author Leif LislegÂrd
 * @since 1.3
 */
class SKTOOLS83_Groovy_Test {


    @Test(expectedExceptions = RuntimeException.class)
    void testSpecialChars() {
        final message = "Ê∆ ¯ÿ Â≈"
        println "message: ${message}"
        throw new RuntimeException(message);
    }

    @Test(expectedExceptions = Error.class)
    void testSpecialChars2() {
        final message = "Ê∆ ¯ÿ Â≈"
        println "message: ${message}"
        throw new Error(message);
    }

}
