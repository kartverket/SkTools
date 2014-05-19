package no.statkart.sktools.gradle.plugins.dbtools.database.oracle

import org.testng.Assert
import org.testng.annotations.Test

/**
 * @see Util
 */
class UtilTest {

    static Closure assertFilterIncludeOrExcludeValue = { String value, String expectedValue, String message = null ->
        def result = Util.filterIncludeOrExcludeValue(value)
        if (message == null) {
            Assert.assertEquals(result, expectedValue);
        } else {
            Assert.assertEquals(result, expectedValue, message);
        }
    };

    /**
     * Tester filtrering av verdier uten kolon
     */
    @Test
    void testFilterIncludeOrExcludeValueNoColon() {

        assertFilterIncludeOrExcludeValue('one two', 'one two', 'tekst uten kolon uten spesialtegn');
        assertFilterIncludeOrExcludeValue('one 2', 'one 2', 'tekst uten kolon med tall');
        assertFilterIncludeOrExcludeValue('one,2', 'one,2', 'tekst uten kolon med komma');
        assertFilterIncludeOrExcludeValue('one,"2"', 'one,"2"', 'tekst uten kolon med spesialtegn');
        assertFilterIncludeOrExcludeValue('one,()', 'one,()', 'tekst uten kolon med spesialtegn');

    }


    /**
     * Tester filtrering av verdier med kolon
     */
    @Test
    void testFilterIncludeOrExcludeValueWithColon() {

        assertFilterIncludeOrExcludeValue('one: two', 'one: two', 'tekst med kolon uten spesialtegn');
        assertFilterIncludeOrExcludeValue('"one": "two"', '"one": \\"two\\"', 'tekst med kolon med spesialtegn');
        assertFilterIncludeOrExcludeValue("'one': 'two'", "'one': \\'two\\'", 'tekst med kolon med spesialtegn');

        assertFilterIncludeOrExcludeValue(': (two)', ': \\(two\\)', 'tekst med kolon med spesialtegn');

        assertFilterIncludeOrExcludeValue("TABLE:\"IN('REPCHECK', 'REPCHECK')\" ", "TABLE:\\\"IN\\(\\\'REPCHECK\\\', \\\'REPCHECK\\\'\\)\\\" ", 'case for SKTOOLS-113');
    }
}
