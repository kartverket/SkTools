package no.statkart.sktools.gradle.plugins.filterproperties;

import org.testng.annotations.Test;

/**
 * Test av http://jira.statkart.no:8080/browse/SKTOOLS-83
 *
 * @author Leif Lislegård
 * @since 1.3
 */
public class SKTOOLS83_Test2 {

    @Test
    public void testSpecialChars() {
        throw new RuntimeException("æÆ øØ åÅ");
    }

    @Test
    public void testSpecialChars2() {
        throw new Error("æÆ øØ åÅ");
    }


}
