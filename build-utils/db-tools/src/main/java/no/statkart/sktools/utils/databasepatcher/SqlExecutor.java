package no.statkart.sktools.utils.databasepatcher;

import no.statkart.sktools.utils.databasepatcher.exception.ConfigurationException;
import no.statkart.sktools.utils.databasepatcher.exception.OperationalException;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Er laget for å kunne kjøre database script på en bestemt databasekobling.
 *
 * @author Jan Holmen
 * @author Henrik Fredholm
 */
public class SqlExecutor {
   private static Logger logger = Logger.getLogger(SqlExecutor.class);


   /**
    * Hjelpeklasse som inneholder en sql statement som godt kan strekke seg over flere linjer.
    */
   public static class ScriptLine {
      /**
       * Start linje
       */
      int lineno;
      /**
       * Er dette en kommentar eller en statement
       */
      boolean isComment;
      /**
       * Sql linjen som skal utføres.
       */
      String line;

      public ScriptLine(int lineno, boolean comment, String line) {
         this.lineno = lineno;
         isComment = comment;
         this.line = line;
      }

      public String toString() {
         return "Line " + lineno + "\n" + line;
      }
   }

   ;

   /**
    * Parser sqlscript og deler det opp i ScriptLiner. Hver script linje er enten eller en kommentar eller en enkelt
    * sql statement.
    * <p/>
    * Kommentarlinjer er linjer som starter med "--". Sqlstatements er linjer som ikke starter med "--". En sqlstatement
    * kan bestå av en eller flere linjer og må avsluttes med ";" og kan ikke inneholde kommentarlinjer. Det kan godt stå
    * flere sqlstatements på en linje.
    *
    * @param sqlScript
    * @return Liste av scriptlinjer hvor hver linje enten er en kommentar eller en enklet sqlstatement.
    */
   static List<ScriptLine> parseSQL(String sqlScript) {
      List<ScriptLine> scriptLines = new LinkedList<ScriptLine>();
      String[] lines = sqlScript.trim().split("\n");
      StringBuffer lineUnderConstruction = null;
      int startLineNo = 0;
      for( int i = 0; i < lines.length; i++ ) {
         // Fjern spacer før og etter
         String line = lines[i].trim();
         if( line.startsWith("--") ) {
            // Det er en kommentar linje, sjekk om kommentaren kommer midt i en sql setning som strekker seg over flere linjer
            if( lineUnderConstruction != null ) {
               throw new OperationalException("Feil i parsing av sql, linje " + (i + 1) + ": kommentar inne i en sql statements som strekker seg over flere linjer støttes ikke. Sjekk om det mangler en \";\". SQL:\n" + lineUnderConstruction);
            }
            scriptLines.add(new ScriptLine(i + 1, true, line));
         } else if( line.length() != 0 ) {
            // Det er ikke en kommentar.
            // Split linjen etter hver ";" og opprett en sql scriptLine per ";"
            int currPos = 0;
            int nextPos = 0;
            while( (nextPos = findEndOfStatement(line, currPos)) != -1 ) {
               String subLine;
               // ';' skal ikke være med, men '}' skal
               if( line.charAt(nextPos) == ';' ) {
                  subLine = line.substring(currPos, nextPos);
               } else {
                  subLine = line.substring(currPos, nextPos + 1);
               }

               if( subLine.contains("--") ) {
                  throw new OperationalException("Feil i parsing av sql, linje " + (i + 1) + ": Kommentarer må stå først på linjen. SQL:\n" + subLine);
               }
               if( lineUnderConstruction != null ) {
                  lineUnderConstruction.append('\n');
                  lineUnderConstruction.append(subLine);
               } else {
                  lineUnderConstruction = new StringBuffer(subLine);
                  startLineNo = i;
               }
               currPos = nextPos + 1;
               scriptLines.add(new ScriptLine(startLineNo, false, lineUnderConstruction.toString()));
               lineUnderConstruction = null;
               startLineNo = 0;
            }
            if( currPos < line.length() ) {
               String subLine = line.substring(currPos);
               if( subLine.contains("--") ) {
                  throw new OperationalException("Feil i parsing av sql, linje " + (i + 1) + ": Kommentarer må stå først på linjen. SQL:\n" + subLine);
               }
               if( lineUnderConstruction != null ) {
                  lineUnderConstruction.append('\n');
                  lineUnderConstruction.append(subLine);
               } else {
                  lineUnderConstruction = new StringBuffer(subLine);
                  startLineNo = i + 1;
               }
            }
         }
      }
      if( lineUnderConstruction != null ) {
         throw new OperationalException("Feil i parsing av sql, linje " + (lines.length) + ": Sql er ikke avsluttet med \";\". SQL:" + lineUnderConstruction);
      }
      return scriptLines;
   }

   private static int findEndOfStatement(String line, int currPos) {
      if( currPos >= line.length() ) {
         return -1;
      } else if( line.charAt(currPos) == '{' ) {
         return line.indexOf("}", currPos);
      } else {
         return line.indexOf(";", currPos);
      }
   }



   /**
    * Denne klassen er brukt i fra Ant.
    * <p/>
    * Hvis parameteren FailOnError er med, vil det bli kastet en exception ved ORACLE SQL error
    * fra feil med andre feilkoder en: 02443, 02275, 00955, 01418, 00942
    * <p/>
    * Kjører ett sql script på database anngitt som VM-Parametere, parameterebrukt:
    * -DFailOnError=true
    *
    * @param sqlScriptNavn, navn på filen som skal lastes.
    */
   public static void runScript(String sqlScriptNavn) throws Exception {
      Connection con = null;
      try {
         con = JDBCHelper.createConnection();
         runScript(con, lesFilFraClasspath(sqlScriptNavn));
      } finally {
         if( con != null ) {
            con.close();
         }
      }
   }

   /**
    * Leser en fil fra classpath og returnerer den som en String
    *
    * @param filnavn Navnet på filene som skal leses fra classpath
    * @return filens innhold i en java.lang.String
    */
   public static String lesFilFraClasspath(String filnavn) {
      ClassLoader classLoader = SqlExecutor.class.getClassLoader();
      InputStream inputStream = classLoader.getResourceAsStream(filnavn.trim());
      if( inputStream == null ) {
         throw new OperationalException("Finner ikke filen " + filnavn + " i classpath");
      }
      BufferedReader br = null;
      StringBuffer tmpScript = new StringBuffer();
      try {
         if( inputStream != null ) {
            br = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
            String line = null;
            try {
               line = br.readLine();
               while( line != null ) {
                  tmpScript.append(line).append("\n");
                  line = br.readLine();
               }
            } finally {
               if( br != null ) {
                  br.close();
               }
               if( inputStream != null ) {
                  inputStream.close();
               }
            }
         }
      } catch( IOException ioe ) {
         logger.error("Under lesing av sqlscript " + ioe.getMessage(), ioe);
      }
      return tmpScript.toString();
   }

   /**
    * Leser en fil fra classpath og returnerer den som en String
    *
    * @param filnavn Navnet på filene som skal leses fra classpath
    * @return filens innhold i en java.lang.String
    */
   public static String lesFilFraWorkingDir(String filnavn) {
      InputStreamReader inputStream = null;
      try {
         inputStream = new FileReader(filnavn.trim());
      } catch( FileNotFoundException e ) {
         throw new OperationalException("Finner ikke filen " + filnavn);
      }
      BufferedReader br = null;
      StringBuffer tmpScript = new StringBuffer();
      try {
         if( inputStream != null ) {
            br = new BufferedReader(inputStream);
            String line = null;
            try {
               line = br.readLine();
               while( line != null ) {
                  tmpScript.append(line).append("\n");
                  line = br.readLine();
               }
            } finally {
               if( br != null ) {
                  br.close();
               }
               if( inputStream != null ) {
                  inputStream.close();
               }
            }
         }
      } catch( IOException ioe ) {
         logger.error("Under lesing av sqlscript " + ioe.getMessage(), ioe);
      }
      return tmpScript.toString();
   }


   /**
    * Kjører ett sql script på anngit database kobling.
    * SqlScript statements som er ommgitt av {} skal kjøres som et prepared statement.
    * Den som kaller denne metoden må passe på og stenge den gitte connectionen selv,
    * alle statements blir stengt.
    *
    * @param connection database koblingen skriptet skal kjøre på.
    * @param sqlScript  scriptet som skal kjøres.
    * @return resultSet hvis det er kalt en stored procedure ommgitt av {} som gir resultatsett, gir flere linjer resultatsett blir disse lagt sammen i den rekkefølgen de er laget.
    */
   public static ResultSet[] runScript(Connection connection, String sqlScript) throws Exception {
      if( connection == null ) {
         throw new ConfigurationException("Kan ikke kjøre databasescript med connection = null");
      }
      if( sqlScript == null ) {
         throw new ConfigurationException("Det må anngis ett sqlscript, sqlSript = null.");
      }
      boolean failOnWarning = "true".equals(System.getProperty("FailOnWarning"));
      List<ScriptLine> scriptLines = parseSQL(sqlScript);

      return runScript(connection, scriptLines, failOnWarning);
   }

   public static ResultSet[] runScript(Connection connection, List<ScriptLine> scriptLines, boolean failOnWarning) throws Exception {
      List rsList = new ArrayList();
      boolean feilet = false;
      int antallFeil = 0;
      int antallWarnings = 0;
      int antallStatements = 0;

      //Kjøre en og en linje i skriptet.
      Statement statement = null;
      try {
         statement = connection.createStatement();

         for( Iterator<ScriptLine> iterator = scriptLines.iterator(); iterator.hasNext(); ) {
            ScriptLine scriptLine = iterator.next();

            if( scriptLine.isComment ) {
               logger.debug("Comment: " + scriptLine.line);
               continue;
            }
            antallStatements++;
            if( scriptLine.line.startsWith("{") ) {
               callCallable(scriptLine.line, connection, rsList);
            } else {
               try {
                  statement.executeUpdate(scriptLine.line);
                  logger.debug("Executed : " + scriptLine);
               } catch( SQLException e ) {
                  String msg = e.getMessage();
                  if( msg.contains("02443") || msg.contains("02275") || msg.contains("00955") || msg.contains("01418") || msg.contains("00942") )
                  {
                     logger.warn("Warning: " + scriptLine + ". Oracle feil: " + msg);
                     antallWarnings++;

                     if( failOnWarning ) {
                        throw new Exception("Feil under kjøring av script.", e);
                     }
                  } else {
                     logger.error("Error: " + scriptLine + "\nOracle feil: " + msg);
                     feilet = true;
                     antallFeil++;
                     if( "true".equals(System.getProperty("FailOnError")) ) {
                        throw new Exception("Feil under kjøring av script.", e);
                     }
                  }

               }
            }
         }
      } finally {
         //forsikre seg at de er stengt ved feil. Ok og kalle close på closed connection.
         if( statement != null ) {
            JDBCHelper.close(statement);
         }
      }
      if( feilet ) {
         logger.error(String.format("Script had errors! Statements: %d. Warnings: %d, errors: %d.", antallStatements, antallWarnings, antallFeil));
      } else {
         logger.info(String.format("Script completed. Statements: %d. Warnings: %d.", antallStatements, antallWarnings));
      }
      return (ResultSet[]) rsList.toArray(new ResultSet[rsList.size()]);
   }

   private static CallableStatement callCallable(String scriptLine, Connection connection, List rsList) throws SQLException {
      //prøver å ta hensyn til at det kan returneres flere Resultset fra ett storedProcedure kall.
      logger.info("Procedure: " + scriptLine);
      CallableStatement callablStatement = connection.prepareCall(scriptLine);
      boolean isResultset = callablStatement.execute();
      if( isResultset ) {
         ResultSet rs = callablStatement.getResultSet();
         rsList.add(rs);
      }
      while( callablStatement.getMoreResults() ) {
         ResultSet rs = callablStatement.getResultSet();
         rsList.add(rs);
      }
      JDBCHelper.close(callablStatement);
      return callablStatement;
   }

   public static void main(String[] args) throws Exception {
      if( args == null || args.length < 1 ) {
         throw new OperationalException("Scriptfilen som skal kjøres må være anngitt som parameter.");
      }
      for( int i = 0; i < args.length; i++ ) {
         runScript(args[i]);
      }
   }



}
