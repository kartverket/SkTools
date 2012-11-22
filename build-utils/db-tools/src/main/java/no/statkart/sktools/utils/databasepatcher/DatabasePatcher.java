package no.statkart.sktools.utils.databasepatcher;

import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Eksekverer en sql patch fil som er inndelt i patchblokker via patch kommentar direktiver. Patchblokker
 * som er eldre enn database nåværende patchversjon blir ikke utført. Hvis databasen har satt et flagg om
 * at indexer ikke er i synk blir alle index patch blokker utført. Hvis indexer er i sync blir indexer i
 * eldre patch blokker skippet.
 */
public class DatabasePatcher {
   private static Logger logger = Logger.getLogger(DatabasePatcher.class);
   static Pattern pPatchDBVersion = Pattern.compile("^--\\s*PATCH\\s+((INDEX)|(DATA))\\s+DB\\.VERSION");
   static Pattern pParsePatchDBVersion = Pattern.compile("^--\\s*PATCH\\s+((?:INDEX)|(?:DATA))\\s+DB\\.VERSION\\s*=\\s*\"([\\w\\.-]+)\"\\s+PATCH\\.NO\\s*=\\s*\"(\\d+)\"(\\s*(.*))?");
   static Pattern pPatchDBMinVersion = Pattern.compile("^--\\s*PATCH\\s+DB\\.MIN\\.VERSION");
   static Pattern pParsePatchMinVersion = Pattern.compile("^--\\s*PATCH\\s+DB\\.MIN\\.VERSION\\s*=\\s*\"([<>\\w\\.-]+)\"");
   static Pattern pStartsWithPatch = Pattern.compile("^--\\s*PATCH[\\s\\n]");

   /**
    * Angir versjonsinformasjon om en patchblock samt patchblokk type
    */
   private static class PatchVersion implements Comparable {
      // Angir om det er en data eller index patchblock
      boolean isDataPatch;
      // Versjonsinfo
      String dbVersion;
      int patchNo;
      // Optional kommentar
      String kommentar;

      public PatchVersion(String dbVersion, int patchNo, String kommentar) {
         this(dbVersion, patchNo, kommentar, false);
      }

      public PatchVersion(String dbVersion, int patchNo, String kommentar, boolean isDataPatch) {
         this.dbVersion = dbVersion;
         this.patchNo = patchNo;
         this.kommentar = kommentar;
         this.isDataPatch = isDataPatch;
      }

      public int compareTo(Object o) {
         if( o == null ) throw new NullPointerException();
         return compareTo((PatchVersion) o);
      }

      /**
       * Patchversjons sammenlikning: DB.VERSION="<any>" < DB.VERSION=null <  DB.VERSION="<streng> PATCH.NO="<number>"
       */
      private int compareTo(PatchVersion o) {
         // Check null
         if( this.dbVersion == null ) {
            if( o.dbVersion == null ) {
               return 0;
            } else {
               return o.dbVersion.equals("<any>") ? 1 : -1;
            }
         }
         if( o.dbVersion == null ) {
            return this.dbVersion.equals("<any>") ? -1 : 1;
         }

         // Check <any>
         if( this.dbVersion.equals("<any>") ) {
            return o.dbVersion.equals("<any>") ? 0 : -1;
         } else if( o.dbVersion.equals("<any>") ) {
            return 1;
         }

         int i = compareDBVersions(this.dbVersion, o.dbVersion);
         if( i == 0 ) {
            if( this.patchNo < o.patchNo ) {
               i = -1;
            } else if( this.patchNo > o.patchNo ) {
               i = 1;
            }
         }
         return i;
      }

      /**
       * Sjekker DB versjonsnumre mot hverandre slik at:
       * <ul>
       * <li>1.8 < 1.9
       * <li>1.9 = 1.9
       * <li>1.9 < 1.9.1
       * <li>1.9.1 < 1.10
       * <li>1.9.1 < 1.9.2
       */
      private static int compareDBVersions(String dbVersion1, String dbVersion2) {
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
               throw new RuntimeException("Kan ikke sammenlikne " + dbVersion1 + " mot " + dbVersion2);
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

      public String toString() {
         return "DB.VERSION=\"" + dbVersion + "\" PATCH.NO=\"" + patchNo + "\"";
      }
   }

   ;

   /**
    * Angir nåverende patchinfo i databasen
    */
   private static class PatchInfo {
      public PatchVersion patchVersion;
      public boolean indexesInSyncWithPatch;

      public PatchInfo(PatchVersion patchVersion, boolean indexesInSyncWithPatch) {
         this.patchVersion = patchVersion;
         this.indexesInSyncWithPatch = indexesInSyncWithPatch;
      }
   }

   ;

   public static void main(String... args) {
      String commandName = args.length > 0 ? args[0] : "";
      if( commandName.equals("getVersion") ) {
         getVersion();
      } else if( commandName.equals("setIndexesInSyncWithPatch") ) {
         setIndexesInSyncWithPatch(args[1].equals("true"));
      } else if( commandName.equals("patch") ) {
         boolean singleStepPatches = "true".equalsIgnoreCase(System.getProperty("singlestep"));
         if( singleStepPatches ) {
            logger.info("Kjøre patcher i singlestep mode slik at kun en ny patch blir utført per kall");
         }
         patch(args[1], singleStepPatches);
      } else {
         System.out.println("Usage: DatabasePatcher getVersion");
         System.out.println("Usage: DatabasePatcher patch sqlPatchfil");
         System.out.println("Usage: DatabasePatcher setIndexesInSyncWithPatch true|false");
      }
   }


   /**
    * Patcher eksisterende database i henhold til patchfil og eksisterende patcher som allerede er installert i databasen
    *
    * @param sqlScriptNavn
    * @param singleStepPatches true hvis kun en ny patch skal utføres. Hvis false utføres alle patcher
    */
   private static void patch(String sqlScriptNavn, boolean singleStepPatches) {
      Connection con = null;

      try {
         List<SqlExecutor.ScriptLine> scriptLines = SqlExecutor.parseSQL(SqlExecutor.lesFilFraClasspath(sqlScriptNavn));
         LinkedHashMap<PatchVersion, List<SqlExecutor.ScriptLine>> patches = parsePatches(scriptLines);

         con = JDBCHelper.createConnection();
         PatchInfo currentPatchInfo = getOrCreatePatchInfo(con);
         logger.info("Nåværende database versjon: " + currentPatchInfo.patchVersion + ". IndexesInSyncWithPatch=" + currentPatchInfo.indexesInSyncWithPatch);

         // Første entry inneholder min version.
         PatchVersion minVersion = patches.entrySet().iterator().next().getKey();
         patches.remove(minVersion);
         if( currentPatchInfo.patchVersion.compareTo(minVersion) == -1 ) {
            throw new RuntimeException("Kan ikke patch database. Krevet minimum versjon: " + minVersion);
         }

         for( PatchVersion p : patches.keySet() ) {
            List<SqlExecutor.ScriptLine> patchBlock = patches.get(p);

            // Sjekke om databasen allerede er patchet med denne patch og om indexer er i sync.
            if( p.compareTo(currentPatchInfo.patchVersion) < 1 ) {
               // Patch har allerede blitt utført, men skal utføres på nytt hvis det er en index patch og indexer ikke er i sync
               if( !p.isDataPatch && !currentPatchInfo.indexesInSyncWithPatch ) {
                  executePatchBlock(con, p, patchBlock, false);
               }
            } else {
               // Ny patch. Utfør alltid.
               executePatchBlock(con, p, patchBlock, true);
               if( singleStepPatches ) break;
            }
         }
         setIndexesInSyncWithPatch(true);
      } catch( SQLException e ) {
         JDBCHelper.close(con);
         throw new RuntimeException(e);
      } catch( Exception e ) {
         throw new RuntimeException(e);
      }
   }

   private static void executePatchBlock(Connection con, PatchVersion p, List<SqlExecutor.ScriptLine> patchBlock, boolean isNewPatch) {
      try {
         if( isNewPatch ) {
            logger.info("Utfører patchblokk: " + p + ((p.kommentar == null) ? "" : " " + p.kommentar));
            SqlExecutor.runScript(con, patchBlock, false);
            updatePatchInfo(con, p);
         } else {
            if( p.isDataPatch ) {
               throw new RuntimeException("Forsøk på å utføre data patch blokk flere ganger på samme database");
            }
            logger.info("Utfører index patchblokk på nytt. Noen index statements kan feile : " + p);
            SqlExecutor.runScript(con, patchBlock, false);
         }
      } catch( Exception e ) {
         throw new RuntimeException(e.getMessage(), e);
      }
   }

   /**
    * Parser en liste av script lines og deler dem opp i patchblokker.
    *
    * @param scriptLines
    */
   private static LinkedHashMap<PatchVersion, List<SqlExecutor.ScriptLine>> parsePatches(List<SqlExecutor.ScriptLine> scriptLines) {
      LinkedHashMap<PatchVersion, List<SqlExecutor.ScriptLine>> result = new LinkedHashMap<PatchVersion, List<SqlExecutor.ScriptLine>>();

      PatchVersion minPatchVersion = new PatchVersion(null, -1, null);
      PatchVersion lastPatchVersion = null;

      int i = 0;
      // Skip initielle kommentar linjer
      while( i < scriptLines.size() && isOrdinaryComment(scriptLines.get(i)) ) {
         i++;
      }
      if( i < scriptLines.size() && isMinDbVersion(scriptLines.get(i)) ) {
         minPatchVersion = parseMinPatchVersion(scriptLines.get(i));
         i++;
      }
      result.put(minPatchVersion, null);
      lastPatchVersion = minPatchVersion;

      // Skip kommentar linjer frem til første patchblock
      while( i < scriptLines.size() && isOrdinaryComment(scriptLines.get(i)) ) {
         i++;
      }

      // Parse patchblokker
      while( i < scriptLines.size() && isPatchVersion(scriptLines.get(i)) ) {
         PatchVersion patchVersion = parsePatchVersion(scriptLines.get(i));

         if( lastPatchVersion.compareTo(patchVersion) != -1 ) {
            throw new RuntimeException("Feil: Patchblokker må ha stigende versjonsnr i fil (forrige var: " + lastPatchVersion + " ): " + scriptLines.get(i));
         }
         lastPatchVersion = patchVersion;
         i++;
         List<SqlExecutor.ScriptLine> patchScriptLines = new ArrayList<SqlExecutor.ScriptLine>();
         while( i < scriptLines.size() && (isOrdinaryComment(scriptLines.get(i)) || isSqlCommand(scriptLines.get(i))) ) {
            patchScriptLines.add(scriptLines.get(i));
            i++;
         }
         result.put(patchVersion, patchScriptLines);
      }
      if( i != scriptLines.size() ) {
         if( isMinDbVersion(scriptLines.get(i)) ) {
            throw new RuntimeException("Feil: '-- PATCH DB.MIN.VERSION=\"<streng>\" allerede spesifisert: " + scriptLines.get(i));
         } else if( isSqlCommand(scriptLines.get(i)) ) {
            throw new RuntimeException("Feil: SQL tilhører ingen patchblokk: " + scriptLines.get(i));
         } else {
            throw new RuntimeException("Feil i '-- PATCH direktiv': " + scriptLines.get(i));
         }
      }

      return result;
   }

   /**
    * Returnerer true hvis linjen er en sql kommando
    *
    * @param scriptLine
    */
   private static boolean isSqlCommand(SqlExecutor.ScriptLine scriptLine) {
      return !scriptLine.isComment;
   }

   /**
    * Parser en kommentarlinje med format: '-- PATCH DB.VERSION="<streng>" PATCH.NO="<number>" [<kommentar>]
    *
    * @param scriptLine
    */
   private static PatchVersion parsePatchVersion(SqlExecutor.ScriptLine scriptLine) {
      Matcher m = pParsePatchDBVersion.matcher(scriptLine.line);
      if( m.find() ) {
         boolean isDataPatch = m.group(1).equals("DATA");
         String version = m.group(2);
         int patchNo = Integer.parseInt(m.group(3));
         String kommentar = null;
         if( m.groupCount() == 5 ) {
            kommentar = m.group(4).trim();
            if( kommentar.equals("") ) kommentar = null;
         }
         return new PatchVersion(version, patchNo, kommentar, isDataPatch);
      } else {
         throw new RuntimeException("Feil: forventet -- PATCH (INDEX|DATA) DB.VERSION=\"<streng>\" PATCH.NO=\"<number>\": " + scriptLine);
      }
   }

   /**
    * Returnerer true hvis linje er en kommentar som starter med: '-- PATCH DB.VERSION...'
    *
    * @param scriptLine
    */
   private static boolean isPatchVersion(SqlExecutor.ScriptLine scriptLine) {
      Matcher m = pPatchDBVersion.matcher(scriptLine.line);
      return m.find();
   }


   /**
    * Parser en kommentarlinje med format: '-- PATCH DB.MIN.VERSION="<streng>"
    *
    * @param scriptLine
    */
   private static PatchVersion parseMinPatchVersion(SqlExecutor.ScriptLine scriptLine) {
      Matcher m = pParsePatchMinVersion.matcher(scriptLine.line);
      if( m.find() ) {
         String version = m.group(1);
         return new PatchVersion(version, -1, null);
      } else {
         throw new RuntimeException("Feil: forventet -- PATCH DB.MIN.VERSION=\"<streng>\": " + scriptLine);
      }
   }

   /**
    * Returnerer true hvis linjen er en kommentar som starter med: '-- PATCH DB.MIN.VERSION..."
    *
    * @param scriptLine
    */
   private static boolean isMinDbVersion(SqlExecutor.ScriptLine scriptLine) {
      Matcher m = pPatchDBMinVersion.matcher(scriptLine.line);
      return m.find();
   }

   /**
    * Returnerer true hvis linjen er en kommentart som ikke starter med: '-- PATCH ...'
    *
    * @param scriptLine
    */
   private static boolean isOrdinaryComment(SqlExecutor.ScriptLine scriptLine) {
      Matcher m = pStartsWithPatch.matcher(scriptLine.line);
      return scriptLine.isComment && !m.find();
   }

   /**
    * Henter ut nåværende PatchVersion for en database. Hvis databasen ikke har noe PatchVersion
    * tabell opprettes en.
    *
    * @param con
    * @return patch info for databasen.
    */
   private static PatchInfo getOrCreatePatchInfo(Connection con) {
      Statement stmt = null;
      ResultSet rs = null;
      try {
         stmt = con.createStatement();

         stmt.execute("select count(*) from user_tables where table_name='PATCHINFO'");
         rs = stmt.getResultSet();
         rs.next();
         boolean patchTableExists = rs.getInt(1) == 1;
         rs.close();
         if( !patchTableExists ) {
            createPatchInfoTable(con);
         }

         stmt.execute("select count(*) from PATCHINFO");
         rs = stmt.getResultSet();
         rs.next();
         int rowCount = rs.getInt(1);
         if( rowCount == 0 ) {
            throw new RuntimeException("Fant ingen rader i tabell PATCHINFO");
         } else if( rowCount > 1 ) {
            throw new RuntimeException("Fant mer enn en rad i tabell PATCHINFO");
         }
         rs.close();

         // PatchVersion tabell finnes, hent ut versjon
         stmt.execute("SELECT dbVersion, patchNo, indexesInSyncWithPatch, kommentar from PATCHINFO");
         rs = stmt.getResultSet();
         rs.next();
         String dbVersion = rs.getString(1);
         int patchNo = rs.getInt(2);
         boolean indexesInSyncWithPatch = rs.getBoolean(3);
         String kommentar = rs.getString(4);
         return new PatchInfo(new PatchVersion(dbVersion, patchNo, kommentar), indexesInSyncWithPatch);
      } catch( SQLException e ) {
         throw new RuntimeException(e);
      } finally {
         JDBCHelper.close(rs, stmt);
      }
   }

   /**
    * Oppretter tabell 'PatchVersion' og legger inn initiell rad.
    *
    * @param con
    */
   private static void createPatchInfoTable(Connection con) {
      Statement stmt = null;
      ResultSet rs = null;
      try {
         stmt = con.createStatement();
         stmt.execute("create table PATCHINFO (dbVersion varchar2(255), patchNo number(19,0) not null, indexesInSyncWithPatch number(1,0) not null, kommentar varchar2(255))");
         stmt.executeUpdate("insert into PATCHINFO (dbVersion, patchNo, indexesInSyncWithPatch, kommentar) values (null, -1, 1, null)");
      } catch( SQLException e ) {
         throw new RuntimeException(e);
      } finally {
         JDBCHelper.close(stmt);
      }
   }


   private static void getVersion() {
      Connection con = null;
      try {
         con = JDBCHelper.createConnection();
         PatchInfo patchInfo = getOrCreatePatchInfo(con);
         System.out.println(String.format("Database versjon: db.version=%s patch.no=%d indexesInSyncWithPatch=%b", patchInfo.patchVersion.dbVersion, patchInfo.patchVersion.patchNo, patchInfo.indexesInSyncWithPatch));
      } catch( SQLException e ) {
         JDBCHelper.close(con);
         throw new RuntimeException(e);
      } catch( Exception e ) {
         throw new RuntimeException(e);
      }
   }

   private static void setIndexesInSyncWithPatch(boolean value) {
      Connection con = null;
      try {
         con = JDBCHelper.createConnection();
         PatchInfo patchInfo = getOrCreatePatchInfo(con);
         setIndexesInSyncWithPatch(con, value);
      } catch( SQLException e ) {
         JDBCHelper.close(con);
         throw new RuntimeException(e);
      } catch( Exception e ) {
         throw new RuntimeException(e);
      }
   }

   private static void setIndexesInSyncWithPatch(Connection con, boolean value) {
      Statement stmt = null;
      ResultSet rs = null;
      try {
         stmt = con.createStatement();

         int res = stmt.executeUpdate("update PATCHINFO set indexesInSyncWithPatch=" + ((value) ? "1" : "0"));
         if( res != 1 ) {
            throw new RuntimeException("Oppdaterte feil antall rader i tabell PATCHINFO. Forventet 1 oppdatering, fikk " + res);
         }
         con.commit();
      } catch( SQLException e ) {
         throw new RuntimeException(e);
      } finally {
         JDBCHelper.close(rs, stmt);
      }
   }

   /**
    * Oppdatere patchinfo rad i databasen
    */
   private static void updatePatchInfo(Connection con, PatchVersion p) {
      PreparedStatement stmt = null;
      ResultSet rs = null;
      try {
         stmt = con.prepareStatement("update PATCHINFO set DBVERSION=?, PATCHNO=?, INDEXESINSYNCWITHPATCH=1, KOMMENTAR=?");
         stmt.setString(1, p.dbVersion);
         stmt.setInt(2, p.patchNo);
         stmt.setString(3, p.kommentar);
         int res = stmt.executeUpdate();
         if( res != 1 ) {
            throw new RuntimeException("Oppdaterte feil antall rader i tabell PATCHINFO. Forventet 1 oppdatering, fikk " + res);
         }
         con.commit();
      } catch( SQLException e ) {
         throw new RuntimeException(e);
      } finally {
         JDBCHelper.close(rs, stmt);
      }
   }

}

