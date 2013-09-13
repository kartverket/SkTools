package no.statkart.sktools.utils.databasepatcher;

import java.util.HashMap;

/**
 * Beskriver lovlige typer av patchblokker.
 *
 * @author Leif Lislegård
 * @since 1.3
 */
public class PatchtypeKode {
    private final static HashMap<String, PatchtypeKode> _staticLookup = new HashMap<String, PatchtypeKode>();

    /** @since 1.3 */
    public final static PatchtypeKode SCHEMA = new PatchtypeKode("SCHEMA", "Patching av skjema");

    /** @deprecated since 1.3 - use {@link #SCHEMA} instead */
    public final static PatchtypeKode DATA = new PatchtypeKode("DATA", "Patching av data");

    public final static PatchtypeKode INDEX = new PatchtypeKode("INDEX", "Patching av indekser");

    /** @since 1.3 */
    public final static PatchtypeKode ALWAYS = new PatchtypeKode("ALWAYS", "Patcheblokk som alltid skal kjøres");


    final String name;
    final String description;


    public static PatchtypeKode fromString(String name) {
        if (name != null) {
            if (_staticLookup.containsKey(name)) {
                return _staticLookup.get(name);
            } else {
                return new PatchtypeKode(name, "User defined code type");
            }
        }
        return null;
    }

    private PatchtypeKode(String name, String description) {
        this.name = name;
        this.description = description;

        _staticLookup.put(name, this);
    }

    public boolean isTypeOf(PatchtypeKode patchtype) {
        return patchtype != null && isTypeOf(patchtype.name);
    }

    /**
     * @since 1.3
     */
    public boolean isIndexPatch() {
        return PatchtypeKode.INDEX.isTypeOf(this);
    }

    public boolean isTypeOf(String patchtypeName) {
        if (patchtypeName == null) return false;

        if (this == ALWAYS || this == SCHEMA) {
            return true;
        } else {
            return patchtypeName.startsWith(name);
        }
    }

    public String toString() {
        return name;
    }

}
