package no.statkart.sktools.utils.databasepatcher;

import no.statkart.sktools.utils.databasepatcher.exception.ConfigurationException;
import no.statkart.sktools.utils.databasepatcher.exception.OperationalException;
import no.statkart.sktools.utils.parsers.sql.SQLStatementParser;
import no.statkart.sktools.utils.parsers.sql.model.Comment;
import no.statkart.sktools.utils.parsers.sql.model.Expression;
import no.statkart.sktools.utils.parsers.sql.model.Statement;
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

    //SKTOOLS-84: error håndtering
    boolean failOnError, failOnWarning;

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
   };





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
   public void runScript(String sqlScriptNavn) throws Exception {
      Connection con = null;
       try {
         con = JDBCHelper.createConnection();
         runScript(con, lesFilFraWorkingDir(sqlScriptNavn));
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
   public ResultSet[] runScript(Connection connection, String sqlScript) throws Exception {
      if( connection == null ) {
         throw new ConfigurationException("Kan ikke kjøre databasescript med connection = null");
      }
      if( sqlScript == null ) {
         throw new ConfigurationException("Det må anngis ett sqlscript, sqlSript = null.");
      }

       List<? extends Expression> expressions = SQLStatementParser.parseExpressions(sqlScript);

       return runScript(connection, expressions);
   }

   public ResultSet[] runScript(Connection connection, List<? extends Expression> scriptLines) throws Exception {
      List rsList = new ArrayList();
      boolean feilet = false;
      int antallFeil = 0;
      int antallWarnings = 0;
      int antallStatements = 0;

      //Kjøre en og en linje i skriptet.
       java.sql.Statement statement = null;
      try {
         statement = connection.createStatement();

         for( Iterator<? extends Expression> iterator = scriptLines.iterator(); iterator.hasNext(); ) {
             Expression scriptLine = iterator.next();

            if( scriptLine instanceof Comment) {
               logger.debug("Comment: " + ((Comment) scriptLine).getText());
               continue; // gjør ikke noe mer for kommentarer
            }

            if (scriptLine instanceof Statement) {
                Statement sqlStatement = (Statement) scriptLine;
                antallStatements++;

                if (sqlStatement.getSql().startsWith("{")) {
                    callCallable(sqlStatement.getSql(), connection, rsList);
                } else {
                    try {
                        statement.executeUpdate(sqlStatement.getSql());
                        logger.debug("Executed : " + sqlStatement.getSql());
                    } catch( SQLException e ) {
                        if (isWarning(e)) {
                            logger.warn("Warning: Error executing line#" + scriptLine.getLineNumber() + ". Oracle error: " + e.getMessage());
                            antallWarnings++;

                            if( failOnWarning ) {
                                throw new Exception("Feil under kjøring av script.", e);
                            }
                        } else {
                            logger.error("Error: Error executing line#" + scriptLine.getLineNumber() + ". Oracle error: " + e.getMessage());
                            feilet = true;
                            antallFeil++;
                            if( failOnError ) {
                                throw new Exception("Feil under kjøring av script.", e);
                            }
                        }
                        logger.debug("Errors while executing : " + sqlStatement.getSql());
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

    static boolean isWarning(SQLException e) {
        String msg = e.getMessage();
        return msg.contains("02443") || msg.contains("02275") || msg.contains("00955") || msg.contains("01418") || msg.contains("00942");
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


    /**
     * Denne klassen er brukt i fra Ant.
     * <p/>
     * Hvis parameteren FailOnError er med, vil det bli kastet en exception ved ORACLE SQL error
     * fra feil med andre feilkoder en: 02443, 02275, 00955, 01418, 00942
     * <p/>
     * Kjører sql script på database anngitt som VM-Parametere, parameterebrukt:
     * -DFailOnError=true
     * -DFailOnWarning=false
     *
     * @param args navn på filen som skal lastes.
     */
    public static void main(String[] args) throws Exception {
      if( args == null || args.length < 1 ) {
         throw new OperationalException("Scriptfilen(e) som skal kjøres må være anngitt som parameter.");
      }

        final SqlExecutor executor = new SqlExecutor();
        executor.failOnError = "true".equals(System.getProperty("FailOnError", "true"));
        executor.failOnWarning = "true".equals(System.getProperty("FailOnWarning", "false"));


        for (int i = 0; i < args.length; i++) {
            executor.runScript(args[i]);
        }
    }



}
