package no.statkart.sktools.utils.databasepatcher.util;

/**
 * Util class for comparison
 * @since 1.2
 */
public class CompareUtil {

    /**
     * Sjekker DB versjonsnumre mot hverandre slik at:
     * <ul>
     * <li>1.8 < 1.9
     * <li>1.9 = 1.9
     * <li>1.9 < 1.9.1
     * <li>1.9.1 < 1.10
     * <li>1.9.1 < 1.9.2
     * <li>1 > NULL
     */
    public static int compareDBVersions(String dbVersion1, String dbVersion2) {
       String[] s1 = dbVersion1.split("\\.");
       String[] s2 = dbVersion2.split("\\.");
       for( int i = 0; i < s1.length; i++ ) {
          if( i == s2.length ) {
             return 1; // dbVersion1 er størst siden den har flest "."
          }
          int res = 0;
          try {
             int int1 = Integer.parseInt(s1[i]);
             int int2 = Integer.parseInt(s2[i]);
             res = (int1 == int2) ? 0 : (int1 < int2) ? -1 : 1;
          } catch( NumberFormatException e ) {
              res = s1[i].compareTo(s2[i]);  //sammenligner leksografisk dersom numerisk sammenligning feiler
          }
          if (res!=0) {
             return res;
          }
       }
       if( s1.length == s2.length ) {
          return 0;
       } else {
          return -1;  // dbVersion2 er størst siden den har flest "."
       }
    }
}
