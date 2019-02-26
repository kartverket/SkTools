package no.statkart.sktools.testutils

/**
 * Verktøy som kan avgjøre om en kjører tester ifra IntelliJ eller ikke.
 *
 * PS: Dette verktøyet virket pt kun innenfor SKTOOLS prosjektet og kan ikke benyttes ved ekstern testavhengighet.
 */
public class IntelliJTestUtil {

    public static boolean getIsIntelliJRuntime() {
        return hasIntelliJScope()
    }

    public static boolean getIsIntelliJTestRuntime() {
        return hasIntelliJTestScope()
    }

    public static boolean hasIntelliJScope() {
        try {
            return IntelliJTestUtil.class.getClassLoader().loadClass("IntelliJDummy") != null
        } catch (ClassNotFoundException ignored) {
            return false
        }
    }

    public static boolean hasIntelliJTestScope() {
        try {
            return IntelliJTestUtil.class.getClassLoader().loadClass("IntelliJTestDummy") != null
        } catch (ClassNotFoundException ignored) {
            return false
        }
    }

}
