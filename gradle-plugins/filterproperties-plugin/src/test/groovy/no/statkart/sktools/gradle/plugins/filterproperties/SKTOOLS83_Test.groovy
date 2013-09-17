package no.statkart.sktools.gradle.plugins.filterproperties

import org.testng.annotations.Test

/**
 * Test av http://jira.statkart.no:8080/browse/SKTOOLS-83
 *
 * @author Leif Lislegård
 * @since 1.3
 */
class SKTOOLS83_Test {


    @Test
    void testSpecialChars() {
        throw new RuntimeException("æÆ øØ åÅ");
    }

    @Test
    void testSpecialChars2() {
        throw new Error("æÆ øØ åÅ");
    }

}
