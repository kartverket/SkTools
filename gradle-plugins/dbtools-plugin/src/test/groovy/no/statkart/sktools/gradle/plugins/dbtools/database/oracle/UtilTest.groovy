package no.statkart.sktools.gradle.plugins.dbtools.database.oracle

import org.testng.annotations.Test

import static org.assertj.core.api.Assertions.assertThat

/**
 * @see Util
 */
class UtilTest {


    /**
     * Tester filtrering av verdier uten kolon
     */
    @Test
    void testFilterIncludeOrExcludeValueNoColon() {

        assertThat(Util.filterIncludeOrExcludeValue('one two')).isEqualTo('one two');
        assertThat(Util.filterIncludeOrExcludeValue('one 2')).isEqualTo('one 2');
        assertThat(Util.filterIncludeOrExcludeValue('one,2')).isEqualTo('one,2');
        assertThat(Util.filterIncludeOrExcludeValue('one,"2"')).isEqualTo('one,"2"');
        assertThat(Util.filterIncludeOrExcludeValue('one,()')).isEqualTo('one,()');

    }


    /**
     * Tester filtrering av verdier med kolon
     */
    @Test
    void testFilterIncludeOrExcludeValueWithColon() {

        assertThat(Util.filterIncludeOrExcludeValue('one: two')).isEqualTo('one: two');
        assertThat(Util.filterIncludeOrExcludeValue('"one": "two"')).isEqualTo('"one": \\"two\\"');
        assertThat(Util.filterIncludeOrExcludeValue("'one': 'two'")).isEqualTo("'one': \\'two\\'");

        assertThat(Util.filterIncludeOrExcludeValue(': (two)')).isEqualTo(': \\(two\\)');

        assertThat(Util.filterIncludeOrExcludeValue("TABLE:\"IN('REPCHECK', 'REPCHECK')\" "))
            .describedAs("regression of SKTOOLS-113")
            .isEqualTo("TABLE:\\\"IN\\(\\\'REPCHECK\\\', \\\'REPCHECK\\\'\\)\\\" ");
    }
}
