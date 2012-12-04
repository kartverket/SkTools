package no.statkart.sktools.utils.databasepatcher;

import no.statkart.sktools.utils.parsers.sql.SQLStatementParser;
import no.statkart.sktools.utils.parsers.sql.model.Comment;
import no.statkart.sktools.utils.parsers.sql.model.Expression;
import no.statkart.sktools.utils.parsers.sql.model.Statement;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.Date;
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

   //SKTOOLS-34: modulbasert patching
   String component = PatchInfo.DEFAULT_MODULE;


    /**
    * Angir versjonsinformasjon om en patchblock samt patchblokk type
    */
   private static class PatchVersion implements Comparable {

      public static final String DEFAULT_DB_VERSION = null;
      public static final int DEFAULT_PATCH_NO = -1;

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
       * Patchversjons sammenlikning: {@code DB.VERSION="<any>"} < {@code DB.VERSION=null} < {@code DB.VERSION="<streng>" PATCH.NO="<number>"}
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
      public static final String DEFAULT_MODULE = "null";

      public PatchVersion patchVersion;
      public boolean indexesInSyncWithPatch;
      public String component;

      public PatchInfo(String component, PatchVersion patchVersion, boolean indexesInSyncWithPatch) {
          this.component = component != null ? component : PatchInfo.DEFAULT_MODULE;
          this.patchVersion = patchVersion;
          this.indexesInSyncWithPatch = indexesInSyncWithPatch;
      }

      public String toString() {
          return String.format("DB.MODULE=%s %s IndexesInSyncWithPatch=%s", component, patchVersion, indexesInSyncWithPatch);
      }
   }

   ;

    private static void printUsage() {
        System.out.println("Usage: DatabasePatcher getVersion [-component <component>]");
        System.out.println("Usage: DatabasePatcher patch sqlPatchfil [-component <component>]");
        System.out.println("Usage: DatabasePatcher setIndexesInSyncWithPatch (true|false) [-component <component>]");
    }

    public static void main(String... args) {

        if (args.length > 0) {
            DatabasePatcher databasePatcher = new DatabasePatcher();
            String commandName = args[0];

           //finner optionalt nivå
           for (int i = 0; i < args.length; i++) {
               String arg = args[i];
               if ("-component".equals(arg) && args.length > i+1) {
                   databasePatcher.component = args[i+1];
               }
           }

           //parser kommando
           if( commandName.equals("getVersion") ) {
               databasePatcher.getVersion();

           } else if( commandName.equals("setIndexesInSyncWithPatch") ) {
               if (args.length > 1 && ("true".equalsIgnoreCase(args[1]) || "false".equalsIgnoreCase(args[1]))) {
                   boolean value = "true".equalsIgnoreCase(args[1]);
                   databasePatcher.setIndexesInSyncWithPatch(value);
               }

           } else if( commandName.equals("patch") ) {
               boolean singleStepPatches = "true".equalsIgnoreCase(System.getProperty("singlestep"));
               if( singleStepPatches ) {
                   logger.info("Kjøre patcher i singlestep mode slik at kun en ny patch blir utført per kall");
               }
               databasePatcher.patch(args[1], singleStepPatches);

           } else {
               //feil ved parsing av kommando
               printUsage();
               System.exit(1);
           }
           System.exit(0);
       }
       printUsage();
       System.exit(1);
   }


    /**
    * Patcher eksisterende database i henhold til patchfil og eksisterende patcher som allerede er installert i databasen
    *
    * @param patchFilePath
    * @param singleStepPatches true hvis kun en ny patch skal utføres. Hvis false utføres alle patcher
    * @return antall patchblokker påført (inkludert indekser dersom indexesInSyncWithPatch != true)
    */
   int patch(String patchFilePath, boolean singleStepPatches) {
      Connection con = null;

      try {
         List<? extends Expression> statements = SQLStatementParser.parseExpressions(SqlExecutor.lesFilFraWorkingDir(patchFilePath));

         LinkedHashMap<PatchVersion, List<? extends Expression>> patches = parsePatches(statements);

         con = JDBCHelper.createConnection();
         PatchInfo currentPatchInfo = getOrCreatePatchInfo(con);
         logger.info("Nåværende patchinformasjon: " + currentPatchInfo);

         // Første entry inneholder min version.
         PatchVersion minVersion = patches.entrySet().iterator().next().getKey();
         patches.remove(minVersion);
         if( currentPatchInfo.patchVersion.compareTo(minVersion) == -1 ) {
            throw new RuntimeException("Kan ikke patch database. Krever minimum versjon: " + minVersion);
         }

         int executedPatchesCount = 0;
         for( PatchVersion p : patches.keySet() ) {
            List<? extends Expression> patchBlock = patches.get(p);

            // Sjekke om databasen allerede er patchet med denne patch og om indexer er i sync.
            if( p.compareTo(currentPatchInfo.patchVersion) < 1 ) {
               // Patch har allerede blitt utført, men skal utføres på nytt hvis det er en index patch og indexer ikke er i sync
               if( !p.isDataPatch && !currentPatchInfo.indexesInSyncWithPatch ) {
                  executePatchBlock(con, p, patchBlock, false);
                  executedPatchesCount++;
               }
            } else {
               // Ny patch. Utfør alltid.
               executePatchBlock(con, p, patchBlock, true);
               executedPatchesCount++;
               if( singleStepPatches ) break;
            }
         }

         setIndexesInSyncWithPatch(true);
         return executedPatchesCount;
      } catch( SQLException e ) {
         JDBCHelper.close(con);
         throw new RuntimeException(e);
      } catch( Exception e ) {
         throw new RuntimeException(e);
      }
   }

   private void executePatchBlock(Connection con, PatchVersion p, List<? extends Expression> patchBlock, boolean isNewPatch) {
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
   private static LinkedHashMap<PatchVersion, List<? extends Expression>> parsePatches(List<? extends Expression> scriptLines) {
      LinkedHashMap<PatchVersion, List<? extends Expression>> result = new LinkedHashMap<PatchVersion, List<? extends Expression>>();

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
         List<Expression> patchScriptLines = new ArrayList<Expression>();
         while( i < scriptLines.size() && (isOrdinaryComment(scriptLines.get(i)) || isStatement(scriptLines.get(i))) ) {
            patchScriptLines.add(scriptLines.get(i));
            i++;
         }
         result.put(patchVersion, patchScriptLines);
      }
      if( i != scriptLines.size() ) {
         if( isMinDbVersion(scriptLines.get(i)) ) {
            throw new RuntimeException("Feil: '-- PATCH DB.MIN.VERSION=\"<streng>\" allerede spesifisert: " + scriptLines.get(i));
         } else if( isStatement(scriptLines.get(i)) ) {
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
    * @param expression
    */
   private static boolean isStatement(Expression expression) {
      return expression instanceof Statement;
   }

   /**
    * Parser en kommentarlinje med format: '-- PATCH DB.VERSION="<streng>" PATCH.NO="<number>" [<kommentar>]
    *
    * @param expression
    */
   private static PatchVersion parsePatchVersion(Expression expression) {
       if (expression instanceof Comment) {
           Comment comment = (Comment) expression;

           Matcher m = pParsePatchDBVersion.matcher(comment.getText());
           if (m.find()) {
               boolean isDataPatch = m.group(1).equals("DATA");
               String version = m.group(2);
               int patchNo = Integer.parseInt(m.group(3));
               String kommentar = null;
               if (m.groupCount() == 5) {
                   kommentar = m.group(4).trim();
                   if (kommentar.equals("")) kommentar = null;
               }
               return new PatchVersion(version, patchNo, kommentar, isDataPatch);
           }
       }
       throw new RuntimeException("Feil: forventet -- PATCH (INDEX|DATA) DB.VERSION=\"<streng>\" PATCH.NO=\"<number>\": " + expression);
   }

   /**
    * Returnerer true hvis linje er en kommentar som starter med: '-- PATCH DB.VERSION...'
    *
    * @param expression
    */
   private static boolean isPatchVersion(Expression expression) {
       if (expression instanceof Comment) {
           Comment comment = (Comment) expression;
           Matcher m = pPatchDBVersion.matcher(comment.getText());
           return m.find();
       }
       return false;

   }


   /**
    * Parser en kommentarlinje med format: '-- PATCH DB.MIN.VERSION="<streng>"
    *
    * @param expression
    */
   private static PatchVersion parseMinPatchVersion(Expression expression) {
       if (expression instanceof Comment) {
           Comment comment = (Comment) expression;
           Matcher m = pParsePatchMinVersion.matcher(comment.getText());
           if (m.find()) {
               String version = m.group(1);
               return new PatchVersion(version, -1, null);
           }
       }
       throw new RuntimeException("Feil: forventet -- PATCH DB.MIN.VERSION=\"<streng>\": " + expression);
   }

   /**
    * Returnerer true hvis linjen er en kommentar som starter med: '-- PATCH DB.MIN.VERSION..."
    *
    * @param expression
    */
   private static boolean isMinDbVersion(Expression expression) {
       if (expression instanceof Comment) {
           Comment comment = (Comment) expression;
           Matcher m = pPatchDBMinVersion.matcher(comment.getText());
           return m.find();
       }
       return false;
   }

   /**
    * Returnerer true hvis linjen er en kommentart som ikke starter med: '-- PATCH ...'
    *
    * @param expression
    */
   private static boolean isOrdinaryComment(Expression expression) {
       if (expression instanceof Comment) {
           Comment comment = (Comment) expression;
           Matcher m = pStartsWithPatch.matcher(comment.getText());
           return !m.find();
       }
       return false;

   }

   /**
    * Henter ut nåværende PatchVersion for en database. Hvis databasen ikke har noe PatchVersion
    * tabell opprettes en.
    *
    * @param con
    * @return patch info for databasen.
    */
   private PatchInfo getOrCreatePatchInfo(Connection con) {
      PreparedStatement stmt = null;
      ResultSet rs = null;
      try {

          //finner ut om tabell finnes i databasen ved å spørre på metadata
          {
              rs = con.getMetaData().getTables(con.getCatalog(), null, "PATCHINFO", new String[]{"TABLE"});
              boolean patchTableExists = rs.next();
              JDBCHelper.close(rs, stmt);
              if (!patchTableExists) {
                  createPatchInfoTable(con);
              }
          }

          //finner ut om en evt trenger å utvide tabell
          {
              rs = con.getMetaData().getColumns(con.getCatalog(), null, "PATCHINFO", null);
              boolean hasComponentColumn = false;
              while (rs.next()) {
                  if ("COMPONENT".equals(rs.getString("COLUMN_NAME"))) {
                      hasComponentColumn = true;
                  }
              }
              JDBCHelper.close(rs, stmt);
              if (!hasComponentColumn) {
                  addComponentColumn(con);


                  //har maks en rad. Endrer evt rad.
                  stmt = con.prepareStatement("update PATCHINFO set component=?");
                  stmt.setString(1, component);
                  stmt.executeUpdate();

                  JDBCHelper.close(rs, stmt);
              }
          }


         stmt = con.prepareStatement("SELECT count(*) FROM PATCHINFO WHERE component=?");
         stmt.setString(1, component);
         rs = stmt.executeQuery();

         rs.next();
         int rowCount = rs.getInt(1);
         JDBCHelper.close(rs, stmt);

         if( rowCount == 0 ) {

             //todo: spørre etter patchnummer

             stmt = con.prepareStatement("insert into PATCHINFO (component, dbVersion, patchNo, indexesInSyncWithPatch, kommentar) values (?, ?, ?, ?, ?)");
             stmt.setString(1, component);
             stmt.setString(2, PatchVersion.DEFAULT_DB_VERSION);
             stmt.setInt(3, PatchVersion.DEFAULT_PATCH_NO);
             stmt.setInt(4, 1); //indexes up to date by default
             stmt.setString(5, String.format("Automatisk opprettet tabell for patchistorikk den %s", new Date()));

             stmt.executeUpdate();
             JDBCHelper.close(rs, stmt);

         } else if( rowCount > 1 ) {
            throw new RuntimeException("Fant mer enn en rad i tabell PATCHINFO");
         }

         // PatchVersion tabell finnes, hent ut versjon
         return getPatchInfo(con);

      } catch( SQLException e ) {
         throw new RuntimeException(e);
      } finally {
         JDBCHelper.close(rs, stmt);
      }
   }


    /**
     * Henter ut nåværende PatchVersion for en database.
     *
     * @param con
     * @return patch info for databasen.
     */
    private PatchInfo getPatchInfo(Connection con) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {

            stmt = con.prepareStatement("SELECT dbVersion, patchNo, indexesInSyncWithPatch, kommentar FROM PATCHINFO WHERE component=?");
            stmt.setString(1, component);
            rs = stmt.executeQuery();

            rs.next();
            String dbVersion = rs.getString("dbVersion");
            int patchNo = rs.getInt("patchNo");
            boolean indexesInSyncWithPatch = rs.getBoolean("indexesInSyncWithPatch");
            String kommentar = rs.getString("kommentar");

            JDBCHelper.close(rs, stmt);

            return new PatchInfo(component, new PatchVersion(dbVersion, patchNo, kommentar), indexesInSyncWithPatch);

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
      java.sql.Statement stmt = null;
      ResultSet rs = null;
      try {
         stmt = con.createStatement();
         stmt.execute("CREATE TABLE PATCHINFO (dbVersion varchar(255), patchNo INTEGER NOT NULL, indexesInSyncWithPatch BOOLEAN NOT NULL, kommentar VARCHAR(255))");
      } catch( SQLException e ) {
         throw new RuntimeException(e);
      } finally {
         JDBCHelper.close(stmt);
      }
   }

    /**
     * Utvider tabell 'PatchVersion' med rad for "component"/komponent.
     *
     * @param con
     */
    private static void addComponentColumn(Connection con) {
        java.sql.Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.createStatement();
            stmt.execute("ALTER TABLE PATCHINFO ADD component varchar(64) NOT NULL");
        } catch( SQLException e ) {
            throw new RuntimeException(e);
        } finally {
            JDBCHelper.close(stmt);
        }
    }


    PatchInfo getVersion() {
      Connection con = null;
      try {
         con = JDBCHelper.createConnection();
         PatchInfo patchInfo = getOrCreatePatchInfo(con);
         System.out.println(String.format("Database versjon: component=%s db.version=%s patch.no=%d indexesInSyncWithPatch=%b", component, patchInfo.patchVersion.dbVersion, patchInfo.patchVersion.patchNo, patchInfo.indexesInSyncWithPatch));
         return patchInfo;
      } catch( SQLException e ) {
         JDBCHelper.close(con);
         throw new RuntimeException(e);
      } catch( Exception e ) {
         throw new RuntimeException(e);
      }
   }

   void setIndexesInSyncWithPatch(boolean value) {
      Connection con = null;
      try {
         con = JDBCHelper.createConnection();
         PatchInfo patchInfo = getPatchInfo(con);   //feiler dersom ikke versjon finnes
         setIndexesInSyncWithPatch(con, value);
      } catch( SQLException e ) {
         JDBCHelper.close(con);
         throw new RuntimeException(e);
      } catch( Exception e ) {
         throw new RuntimeException(e);
      }
   }

   private void setIndexesInSyncWithPatch(Connection con, boolean value) {
      PreparedStatement stmt = null;
      ResultSet rs = null;
      try {
         stmt = con.prepareStatement("update PATCHINFO set indexesInSyncWithPatch=? WHERE component=?");
         stmt.setInt(1, value ? 1 : 0);
         stmt.setString(2, component);

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

   /**
    * Oppdatere patchinfo rad i databasen
    */
   private void updatePatchInfo(Connection con, PatchVersion p) {
      PreparedStatement stmt = null;
      ResultSet rs = null;
      try {
         stmt = con.prepareStatement("update PATCHINFO set dbVersion=?, patchNo=?, indexesInSyncWithPatch=1, kommentar=? WHERE component=?");
         stmt.setString(1, p.dbVersion);
         stmt.setInt(2, p.patchNo);
         stmt.setString(3, p.kommentar);
         stmt.setString(4, component);
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

