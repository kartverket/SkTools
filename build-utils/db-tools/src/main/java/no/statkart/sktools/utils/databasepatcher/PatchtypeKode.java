package no.statkart.sktools.utils.databasepatcher;

import java.util.Collection;
import java.util.HashMap;

/**
 * Beskriver lovlige typer av patchblokker.
 *
 * @author Leif Lislegård
 * @since 1.3
 */
public class PatchtypeKode {
    private final static HashMap<String, PatchtypeKode> _staticLookup = new HashMap<>();

    /** @since 1.3 */
    public final static PatchtypeKode ALL = new PatchtypeKode("", "Alle patchtyper"); //samme som schema + always

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

    /**
     * @since 1.3
     * @return {@code true} dersom denne koden er en subtype av parameterisert kode
     */
    public boolean isTypeOf(PatchtypeKode patchtype) {
        return patchtype != null && isTypeOf(patchtype.name);
    }

    /**
     * @since 1.3
     * @see #isTypeOf(PatchtypeKode)
     * @return {@code true} dersom koden er et subsett av samlingen av koder.
     */
    public boolean isContaintedBy(Collection<PatchtypeKode> patchtypes) {
        if (this == ALL) {
            return true;
        } else {
            for (PatchtypeKode patchtypeKode : patchtypes) {
                if (isTypeOf(patchtypeKode)) {
                    return true;
                }
            }
        }
        return false;
    }


    public boolean isTypeOf(String patchtypeName) {
        if (patchtypeName == null) return false;
        return name.startsWith(patchtypeName);
    }

    public String toString() {
        return name;
    }

}
