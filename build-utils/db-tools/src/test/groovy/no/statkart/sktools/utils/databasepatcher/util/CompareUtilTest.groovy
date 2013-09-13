package no.statkart.sktools.utils.databasepatcher.util

import org.testng.annotations.Test

/**
 * Test av {@link CompareUtil}
 *
 * @since 1.3
 * @author Leif Lislegård
 */
class CompareUtilTest {

    @Test
    void testCompareDBVersions() {

        Assert.that("1").isEqualTo("1")
        Assert.that("1").isLessThan("1.0")
        Assert.that("1.0").isGreaterThan("1")
        Assert.that("1.0").isLessThan("1.0.0")
        Assert.that("1.0.0").isGreaterThan("1.0")

        Assert.that("1.8").isLessThan("1.9")
        Assert.that("1.9").isEqualTo("1.9")

        Assert.that("1.9.1").isEqualTo("1.9.1")
        Assert.that("1.9.1").isLessThan("1.9.10")
        Assert.that("1.9.10").isGreaterThan("1.9.1")

        Assert.that("1").isLessThan("null")
        Assert.that("null").isEqualTo("null")
        Assert.that("null1").isLessThan("null2")
        Assert.that("null2").isGreaterThan("null1")

    }

    static class Assert {
        final CompareUtil test = new CompareUtil();
        final String value
        
        private Assert(String value) {
            this.value = value
        }

        static Assert that(String value) {
            return new Assert(value)
        }

        Assert isLessThan(String other) {
            if (!(test.compareDBVersions(value, other) < 0)) {
                throw new AssertionError("Expected that '${value}' is less than '${other}'")
            }
            this
        }
        Assert isEqualTo(String other) {
            if (!(test.compareDBVersions(value, other) == 0)) {
                throw new AssertionError("Expected that '${value}' is equal to '${other}'")
            }
            this
        }
        Assert isGreaterThan(String other) {
            if (!(test.compareDBVersions(value, other) > 0)) {
                throw new AssertionError("Expected that '${value}' is greater than '${other}'")
            }
            this
        }
    }



}
