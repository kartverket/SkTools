package no.statkart.sktools.gradle.plugins.dbtools.database.oracle;

/**
 * @author Leif Lislegård
 * @since Ny Grunnbok Sprint 39
 */
public class Util {

    /**
     * SKTOOLS-113: escaping of values for include and exclude
     * <p>Escaping is needed when passing argument on command line (not using a parameter file)<p/>
     */
    public static String filterIncludeOrExcludeValue(String value) {
        if (value == null) {
            return null;
        } else {
            return filterImpl(value.indexOf(':'), value);
        }
    }

    static String filterImpl(int idx, String value) {
        if (idx < 0) {
            return value;
        } else if (idx == 0) {
            return escapeImpl(value);
        } else {
            return value.substring(0, idx + 1) + escapeImpl(value.substring(idx + 1));
        }
    }

    static String escapeImpl(String value) {
        value = value.replaceAll("'", "\\\\'");
        value = value.replaceAll("\"", "\\\\\"");
        value = value.replaceAll("\\(", "\\\\(");
        value = value.replaceAll("\\)", "\\\\)");
        return value;
    }

}
