package no.statkart.sktools.utils.databasepatcher;

import no.statkart.sktools.utils.databasepatcher.exception.ConfigurationException;
import no.statkart.sktools.utils.databasepatcher.exception.NotFoundException;
import no.statkart.sktools.utils.databasepatcher.exception.OperationalException;
import no.statkart.sktools.utils.databasepatcher.util.CompareUtil;
import no.statkart.sktools.utils.parsers.sql.SQLStatementParser;
import no.statkart.sktools.utils.parsers.sql.model.Comment;
import no.statkart.sktools.utils.parsers.sql.model.Expression;
import no.statkart.sktools.utils.parsers.sql.model.Statement;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;
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
   static Pattern pPatchDBVersion = Pattern.compile("^--\\s*PATCH\\s+(\\w+)\\s+DB\\.VERSION");
   static Pattern pParsePatchDBVersion = Pattern.compile("^--\\s*PATCH\\s+(\\w+)\\s+DB\\.VERSION\\s*=\\s*\"([\\w\\.-]+)\"\\s+PATCH\\.NO\\s*=\\s*\"(\\d+)\"(\\s*(.*))?");
   static Pattern pPatchDBMinVersion = Pattern.compile("^--\\s*PATCH\\s+DB\\.MIN\\.VERSION");
   static Pattern pParsePatchMinVersion = Pattern.compile("^--\\s*PATCH\\s+DB\\.MIN\\.VERSION\\s*=\\s*\"([<>\\w\\.-]+)\"");
   static Pattern pStartsWithPatch = Pattern.compile("^--\\s*PATCH[\\s\\n]");

   //SKTOOLS-34: modulbasert patching
   public String component = PatchInfo.DEFAULT_MODULE;


    /**
     * Mulighet for programatisk konfigurering av properties.
     *
     * Dette kan feks gjøres ved å selv opprette en Connection instans, eller å sette {@code JDBCHelper.connectionProperties}
     * @since 1.2
     */
    protected Connection createConnection() throws SQLException {
        return JDBCHelper.createConnection();
    }


    /**
    * Angir versjonsinformasjon om en patchblock samt patchblokk type
    */
   static class PatchVersion implements Comparable {

      public static final String DEFAULT_DB_VERSION = null;
      public static final int DEFAULT_PATCH_NO = -1;

      // Angir om det er en data eller index patchblock
      PatchtypeKode patchtype;
      // Versjonsinfo
      String dbVersion;
      int patchNo;
      // Optional kommentar
      String kommentar;

      public PatchVersion(String kommentar) {
         this(DEFAULT_DB_VERSION, DEFAULT_PATCH_NO, kommentar, null);
      }

      public PatchVersion(String dbVersion, int patchNo, String kommentar) {
         this(dbVersion, patchNo, kommentar, null);
      }

      public PatchVersion(String dbVersion, int patchNo, String kommentar, PatchtypeKode type) {
         this.dbVersion = dbVersion;
         this.patchNo = patchNo;
         this.kommentar = kommentar;
         this.patchtype = type;
      }

      public int compareTo(Object o) {
         if( o == null ) throw new NullPointerException();
         return compareTo((PatchVersion) o);
      }

      /**
       * Patchversjons sammenlikning: {@code DB.VERSION="<any>"} < {@code DB.VERSION=null} < {@code DB.VERSION="<string>" PATCH.NO="<number>"}
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

         int i = CompareUtil.compareDBVersions(this.dbVersion, o.dbVersion);
         if( i == 0 ) {
            if( this.patchNo < o.patchNo ) {
               i = -1;
            } else if( this.patchNo > o.patchNo ) {
               i = 1;
            }
         }
         return i;
      }

        public String toString() {
         return (patchtype != null ? patchtype : "   ") + " DB.VERSION=\"" + dbVersion + "\" PATCH.NO=\"" + patchNo + "\"";
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
        System.err.println("Usage: DatabasePatcher getVersion [-component <component>]");
        System.err.println("Usage: DatabasePatcher patch sqlPatchfil [-component <component>]");
        System.err.println("Usage: DatabasePatcher setIndexesInSyncWithPatch (true|false) [-component <component>]");
        System.err.println("Usage: DatabasePatcher defineVersion DB.VERSION [PATCH.NO] -component <component>");
        System.err.println("Usage: DatabasePatcher assertVersion DB.VERSION [PATCH.NO] -component <component>");
    }

    public static void main(String... args) {

        int idx = 0;
        boolean hasComponentArg = false;

        if (args.length > 0) {
            DatabasePatcher databasePatcher = new DatabasePatcher();
            String commandName = args[idx++];

           //finner optionalt nivå
           for (int i = idx; i < args.length; i++) {
               String arg = args[i];
               if ("-component".equals(arg) && args.length > i+1) {
                   databasePatcher.component = args[i+1];
                   hasComponentArg = true;
               }
           }

           //parser kommando
           if( commandName.equals("getVersion") ) {
               try {
                   databasePatcher.getVersion();
               } catch (NotFoundException nfe) {
                   logger.error(String.format("Versjon ikke definert for '%s'", databasePatcher.component), nfe);
               }
               System.exit(0);

           } else if( commandName.equals("setIndexesInSyncWithPatch") ) {
               if (args.length > idx && ("true".equalsIgnoreCase(args[idx]) || "false".equalsIgnoreCase(args[idx]))) {
                   boolean value = "true".equalsIgnoreCase(args[idx++]);
                   databasePatcher.setIndexesInSyncWithPatch(value);
                   System.exit(0);
               }

           } else if( commandName.equals("patch") ) {
               boolean singleStepPatches = "true".equalsIgnoreCase(System.getProperty("singlestep"));
               if( singleStepPatches ) {
                   logger.info("Kjøre patcher i singlestep mode slik at kun en ny patch blir utført per kall");
               }
               databasePatcher.patch(args[idx++], singleStepPatches);
               System.exit(0);

           } else if( commandName.equals("assertVersion") ) {
               if (hasComponentArg && args.length > idx && !args[idx].startsWith("-")) {
                   String dbVersion = args[idx++];

                   //finner valgfri patch#
                   Integer patchNumber = null;
                   if (args.length > idx && !args[idx].startsWith("-")) {
                       try {
                           patchNumber = Integer.parseInt(args[idx++]);
                       } catch (NumberFormatException nfe) {
                           System.err.println(String.format("Error parsing patch#! (%s)", args[idx-1]));
                           printUsage();
                           System.exit(2);
                       }
                   }

                   boolean noError = databasePatcher.assertVersion(dbVersion, patchNumber);
                   System.exit(noError ? 0 : 2);
               }
           } else if( commandName.equals("defineVersion") ) {
               if (hasComponentArg && args.length > idx && !args[idx].startsWith("-")) {
                   String dbVersion = args[idx++];

                   //finner valgfri patch#
                   int patchNumber = PatchVersion.DEFAULT_PATCH_NO;
                   if (args.length > idx && !args[idx].startsWith("-")) {
                       try {
                           patchNumber = Integer.parseInt(args[idx++]);
                       } catch (NumberFormatException nfe) {
                           System.err.println(String.format("Error parsing patch#! (%s)", args[idx-1]));
                           printUsage();
                           System.exit(2);
                       }
                   }

                   boolean ok = databasePatcher.defineVersion(dbVersion, patchNumber);
                   if (!ok) {
                       System.exit(2);
                   }
                   System.exit(0);

               }
           }
        }
       //feil ved parsing av kommando
       printUsage();
       System.exit(1);
   }


    /**
    * Patcher eksisterende database i henhold til patchfil og eksisterende patcher som allerede er installert i databasen
    *
    * @param patchFilePath filsti for patchfil som skal eksekveres
    * @param singleStepPatches {@code true} hvis kun en ny patch skal utføres. Hvis {@code false} utføres alle patcher
    * @return antall patchblokker påført (inkludert indekser dersom indexesInSyncWithPatch != true)
    */
   public int patch(String patchFilePath, boolean singleStepPatches) {
      Connection con = null;

      try {
         List<? extends Expression> statements = SQLStatementParser.parseExpressions(SqlExecutor.lesFilFraWorkingDir(patchFilePath));

         LinkedHashMap<PatchVersion, List<? extends Expression>> patches = parsePatches(statements);

         con = createConnection();
         PatchInfo currentPatchInfo = getOrCreatePatchInfo(con, getDefaultVersion());
         logger.info("Nåværende patchinformasjon: " + currentPatchInfo);

         // Første entry inneholder min version.
         PatchVersion minVersion = patches.entrySet().iterator().next().getKey();
         patches.remove(minVersion);
         if( currentPatchInfo.patchVersion.compareTo(minVersion) < 0 ) {
            throw new RuntimeException("Kan ikke patch database. Krever minimum versjon: " + minVersion);
         }

         int executedPatchesCount = 0;
         for (Map.Entry<PatchVersion, List<? extends Expression>> entry : patches.entrySet()) {
             PatchVersion p = entry.getKey();

             // Bestemmer om databasen allerede er patchet med denne patch og om indexer er i sync.
             boolean newPatch = currentPatchInfo.patchVersion.compareTo(p) < 0;

             // Patch har allerede blitt utført, men skal utføres på nytt hvis det er en index patch og indexer ikke er i sync
             if (p.patchtype.isIndexPatch() && !currentPatchInfo.indexesInSyncWithPatch) {
                 executePatchBlock(con, p, entry.getValue(), newPatch);
                 executedPatchesCount++; //telles med når currentPatchInfo.indexesInSyncWithPatch == false
             } else if (p.patchtype == PatchtypeKode.ALWAYS) {
                 executePatchBlock(con, p, entry.getValue(), false);
                 executedPatchesCount = executedPatchesCount; //oppdateres ikke antall eksekverte patchblokker da denne er såpass spesiell
             } else if (newPatch) {
                 // Ny patch. Utføres alltid.
                 executePatchBlock(con, p, entry.getValue(), true);
                 executedPatchesCount++;
                 if( singleStepPatches ) {
                    break;
                 }
             }
         }

         setIndexesInSyncWithPatch(true); //indexer blir automatisk lagt til naar !indexesInSyncWithPatch
         return executedPatchesCount;
      } catch( SQLException e ) {
          throw new OperationalException(logger, "Feil ved sql", e);
      } catch (IOException e) {
          throw new OperationalException(logger, "Feil ved parsing av sql-fil", e);
      } finally {
          JDBCHelper.close(con);
      }
   }

   private void executePatchBlock(Connection con, PatchVersion p, List<? extends Expression> patchBlock, boolean isNewPatch) {
      try {
         if( isNewPatch ) {
            logger.info("Utfører patchblokk: " + p + ((p.kommentar == null) ? "" : " " + p.kommentar));
            SqlExecutor.runScript(con, patchBlock, false);
            updatePatchInfo(con, p);
         } else {
            if( p.patchtype.isIndexPatch() || p.patchtype == PatchtypeKode.ALWAYS ) {
                if (p.patchtype.isIndexPatch()) {
                    logger.info("Utfører index patchblokk på nytt. Noen index statements kan feile : " + p);
                }
                SqlExecutor.runScript(con, patchBlock, false);
            } else {
                throw new RuntimeException("Forsøk på å utføre skjema patch blokk flere ganger mot samme database");
            }
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

      PatchVersion minDBVersion = new PatchVersion("Unspecified min.version");
      PatchVersion lastPatchVersion = null;

      int i = 0;
      // Skip initielle kommentar linjer
      while( i < scriptLines.size() && isOrdinaryComment(scriptLines.get(i)) ) {
         i++;
      }
      if( i < scriptLines.size() && isMinDbVersion(scriptLines.get(i)) ) {
         minDBVersion = parseMinDBVersion(scriptLines.get(i));
         i++;
      }
      result.put(minDBVersion, null);
      lastPatchVersion = minDBVersion;

      // Skip kommentar linjer frem til første patchblock
      while( i < scriptLines.size() && isOrdinaryComment(scriptLines.get(i)) ) {
         i++;
      }

      // Parse patchblokker
      while( i < scriptLines.size() && isPatchVersion(scriptLines.get(i)) ) {
         PatchVersion patchVersion = parsePatchVersion(scriptLines.get(i));

          if (lastPatchVersion == minDBVersion && PatchtypeKode.ALWAYS.isTypeOf(patchVersion.patchtype)) {
              //SKTOOLS-77: ALWAYS patcher kan ha patchnummer mindre enn db.min.version
          } else if (lastPatchVersion.compareTo(patchVersion) >= 0) {
              throw configurationException(scriptLines.get(i), String.format("Patchblokker må ha stigende versjonsnummer i fil (forrige var: %s ).", lastPatchVersion));
          } else {
              lastPatchVersion = patchVersion;
          }

         i++;
         List<Expression> patchScriptLines = new ArrayList<Expression>();
         while( i < scriptLines.size() && (isOrdinaryComment(scriptLines.get(i)) || isStatement(scriptLines.get(i))) ) {
            patchScriptLines.add(scriptLines.get(i));
            i++;
         }
         result.put(patchVersion, patchScriptLines);
      }
      if( i != scriptLines.size() ) {
          String statementAsText = isStatement(scriptLines.get(i)) ? ((Statement)scriptLines.get(i)).getSql() : ((Comment)scriptLines.get(i)).getText();
          if( isMinDbVersion(scriptLines.get(i)) ) {
              throw configurationException(scriptLines.get(i), "'-- PATCH DB.MIN.VERSION=\"<string>\" allerede spesifisert.");
         } else if( isStatement(scriptLines.get(i)) ) {
              throw configurationException(scriptLines.get(i), String.format("SQL tilhører ingen patchblokk: %s", statementAsText));
         } else {
              throw configurationException(scriptLines.get(i), String.format("Feil i '-- PATCH direktiv': %s", statementAsText));
         }
      }

      return result;
   }

    private static ConfigurationException configurationException(Expression expression, String message) {
        return new ConfigurationException(String.format("Feil i linje# %d: %s", expression.getLineNumber(), message));
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
    * Parser en kommentarlinje med format: {@code -- PATCH <type> DB.VERSION="<string>" PATCH.NO="<number>" [<kommentar>]}
    *
    * @param expression
    */
   private static PatchVersion parsePatchVersion(Expression expression) {
       if (expression instanceof Comment) {
           Comment comment = (Comment) expression;

           Matcher m = pParsePatchDBVersion.matcher(comment.getText());
           if (m.find()) {
               PatchtypeKode patchtype = PatchtypeKode.fromString(m.group(1));
               String version = m.group(2);
               int patchNo = Integer.parseInt(m.group(3));
               String kommentar = null;
               if (m.groupCount() == 5) {
                   kommentar = m.group(4).trim();
                   if (kommentar.equals("")) kommentar = null;
               }
               return new PatchVersion(version, patchNo, kommentar, patchtype);
           }
       }
       throw configurationException(expression, "Forventet -- PATCH <type> DB.VERSION=\"<string>\" PATCH.NO=\"<number>\"");
   }

   /**
    * Returnerer true hvis linje er en kommentar som starter med: {@code -- PATCH <type> DB.VERSION}
    *
    * @param expression uttrykk som skal parses
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
    * Parser en kommentarlinje med format: '-- PATCH DB.MIN.VERSION="<string>"
    *
    * @param expression uttrykk som skal parses
    * @return parset patchversjon for db.version. Patch.version er satt til {@link Integer#MIN_VALUE}
    */
   private static PatchVersion parseMinDBVersion(Expression expression) {
       if (expression instanceof Comment) {
           Comment comment = (Comment) expression;
           Matcher m = pParsePatchMinVersion.matcher(comment.getText());
           if (m.find()) {
               String version = m.group(1);
               return new PatchVersion(version, Integer.MIN_VALUE, "DB.MIN.VERSION"); //SKTOOLS-77: patchversion slik at man tillater negative patchnummer for ALWYAS patcher
           }
       }
       throw configurationException(expression, "Forventet -- PATCH DB.MIN.VERSION=\"<string>\"");
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
    * Returnerer {@code true} hvis linjen er en kommentar som ikke starter med: {@code -- PATCH}...
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
    * @throws NotFoundException if no patchinfo for component exists
    */
   private PatchInfo getOrCreatePatchInfo(Connection con, PatchInfo candidatePatchInfo) throws NotFoundException {
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
             if (candidatePatchInfo != null) {
                 stmt = con.prepareStatement("insert into PATCHINFO (component, dbVersion, patchNo, indexesInSyncWithPatch, kommentar) values (?, ?, ?, ?, ?)");
                 stmt.setString(1, candidatePatchInfo.component);
                 stmt.setString(2, candidatePatchInfo.patchVersion.dbVersion);
                 stmt.setInt(3, candidatePatchInfo.patchVersion.patchNo);
                 stmt.setInt(4, candidatePatchInfo.indexesInSyncWithPatch ? 0 : 1);
                 stmt.setString(5, candidatePatchInfo.patchVersion.kommentar);

                 logger.info(String.format("Defining patchInfo: %s", candidatePatchInfo));

                 stmt.executeUpdate();
                 JDBCHelper.close(rs, stmt);
             } else {
                 throw new NotFoundException("Fant ikke versjon for komponent: " + component);
             }

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
          logger.info("Creating table PATCHINFO");
         stmt.execute("CREATE TABLE PATCHINFO (dbVersion varchar(255), patchNo INTEGER NOT NULL, indexesInSyncWithPatch SMALLINT NOT NULL, kommentar VARCHAR(255))");
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
            logger.info("Adding column component to PATCHINFO");
            stmt.execute(String.format("ALTER TABLE PATCHINFO ADD component varchar(64) DEFAULT '%s' NOT NULL", PatchInfo.DEFAULT_MODULE));
        } catch( SQLException e ) {
            throw new RuntimeException(e);
        } finally {
            JDBCHelper.close(stmt);
        }
    }


    public PatchInfo getVersion() {
        return getOrCreateVersion(null);
    }

    /**
     *
     * @throws NotFoundException if no patchinfo for component exists
     */
    public PatchInfo getOrCreateVersion(PatchInfo patchInfo) throws NotFoundException {
        Connection con = null;
        try {
            con = createConnection();

            PatchInfo currentPatchInfo = getOrCreatePatchInfo(con, patchInfo);
            logger.info(String.format("Database versjon: component=%s db.version=%s patch.no=%d indexesInSyncWithPatch=%b", currentPatchInfo.component, currentPatchInfo.patchVersion.dbVersion, currentPatchInfo.patchVersion.patchNo, currentPatchInfo.indexesInSyncWithPatch));

            return currentPatchInfo;
        } catch( SQLException e ) {
            throw new OperationalException(logger, "Feil ved connection", e);
        } finally {
            JDBCHelper.close(con);
        }
    }

    PatchInfo getDefaultVersion() {
        PatchVersion patchVersion = new PatchVersion(String.format("Automatisk opprettet patchistorikk den %s", new Date()));
        PatchInfo patchInfo = new PatchInfo(component, patchVersion, true);
        patchInfo.indexesInSyncWithPatch = true; //indexes up to date by default
        return patchInfo;
    }


    /**
     * Asserts that the given version parameters exists
     * @param patchNumber asserted only if not <code>null</code>
     * @return <code>true</code> if no error found
     */
    public boolean assertVersion(String dbVersion, Integer patchNumber) {
        try {
            PatchInfo patchInfo = getVersion();
            if ( !dbVersion.equals(patchInfo.patchVersion.dbVersion)) {
                return false;
            }
            if (patchNumber != null ) {
                if (patchNumber.intValue() != patchInfo.patchVersion.patchNo) {
                    return false;
                }
            }
            return true;
        } catch (NotFoundException nfe) {
            return false;
        }

    }

    /**
     * Inserts or updates the version information.
     * If no version for component exists, then a new info is added.
     * Else if version exists, then the info is updated according to parameteres.
     *
     * @return {@code false} if the version already exists
     */
    public boolean defineVersion(String dbVersion, int patchNumber) {
        Connection con = null;
        try {
            con = createConnection();

            if (hasVersion(con, null)) { //version info for component exists
                PatchInfo currentPatchInfo = getOrCreatePatchInfo(con, null);

                if (currentPatchInfo.patchVersion.dbVersion.equals(dbVersion)) {
                    if (currentPatchInfo.patchVersion.patchNo != patchNumber) { //allows update across the same dbversion
                        //update version
                        PatchVersion patchVersion = currentPatchInfo.patchVersion;

                        patchVersion.dbVersion = dbVersion;
                        patchVersion.patchNo = patchNumber;

                        updatePatchInfo(con, patchVersion);

                        currentPatchInfo = getOrCreatePatchInfo(con, null);
                        logger.info((String.format("Database versjon: component=%s db.version=%s patch.no=%d indexesInSyncWithPatch=%b", currentPatchInfo.component, currentPatchInfo.patchVersion.dbVersion, currentPatchInfo.patchVersion.patchNo, currentPatchInfo.indexesInSyncWithPatch)));

                        return true;

                    } else {
                        //har allerede versjon
                        logger.info(String.format("Patchversjon allerede definert!"));
                        return true;
                    }

                } else {
                    logger.error("Kan ikke definere patchversjon da versjonsinfomasjon for komponent allerede finnes.");
                    logger.info((String.format("Database versjon: component=%s db.version=%s patch.no=%d indexesInSyncWithPatch=%b", currentPatchInfo.component, currentPatchInfo.patchVersion.dbVersion, currentPatchInfo.patchVersion.patchNo, currentPatchInfo.indexesInSyncWithPatch)));

                    return false;
                }

            } else {
                //legger inn versjon
                PatchInfo patchInfo = getDefaultVersion();
                patchInfo.indexesInSyncWithPatch = false;

                PatchVersion patchVersion = patchInfo.patchVersion;
                patchVersion.dbVersion = dbVersion;
                patchVersion.patchNo = patchNumber;

                PatchInfo currentPatchInfo = getOrCreatePatchInfo(con, patchInfo);
                logger.info((String.format("Database versjon: component=%s db.version=%s patch.no=%d indexesInSyncWithPatch=%b", currentPatchInfo.component, currentPatchInfo.patchVersion.dbVersion, currentPatchInfo.patchVersion.patchNo, currentPatchInfo.indexesInSyncWithPatch)));

                return true;
            }


        } catch( SQLException e ) {
            throw new OperationalException(logger, "Feil ved sql-connection", e);
        } finally {
            JDBCHelper.close(con);
        }
    }

    /**
     *
     * @param dbVersion ignores dbVersion if {@code null}
     */
    boolean hasVersion(Connection con, String dbVersion) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sqlString = "SELECT count(*) FROM PATCHINFO WHERE component=?";
            if (dbVersion != null) {
                sqlString += " AND dbVersion=?";
            }
            stmt = con.prepareStatement(sqlString);
            stmt.setString(1, component);
            if (dbVersion != null) {
                stmt.setString(2, dbVersion);
            }
            rs = stmt.executeQuery();

            rs.next();
            return rs.getLong(1) > 0;

        } catch( SQLException e ) {
            return false;
        } finally {
            JDBCHelper.close(rs, stmt);
        }
    }

    public void setIndexesInSyncWithPatch(boolean value) {
      Connection con = null;
      try {
         con = createConnection();
         PatchInfo patchInfo = getPatchInfo(con);   //feiler dersom ikke versjon finnes
         setIndexesInSyncWithPatch(con, value);
      } catch( SQLException e ) {
          throw new OperationalException(logger, "Feil ved sql-connection", e);
      } finally {
          JDBCHelper.close(con);
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

