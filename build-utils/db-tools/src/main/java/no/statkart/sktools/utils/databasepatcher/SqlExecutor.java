package no.statkart.sktools.utils.databasepatcher;

import no.statkart.sktools.utils.databasepatcher.exception.ConfigurationException;
import no.statkart.sktools.utils.databasepatcher.exception.OperationalException;
import no.statkart.sktools.utils.parsers.sql.SQLStatementParser;
import no.statkart.sktools.utils.parsers.sql.model.Comment;
import no.statkart.sktools.utils.parsers.sql.model.Expression;
import no.statkart.sktools.utils.parsers.sql.model.Statement;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
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

    //SKTOOLS-21
    static Charset sqlFileEncoding = Charset.defaultCharset();
    static {
        String encoding = System.getProperty("sql.file.encoding");
        if (encoding != null) {
            sqlFileEncoding = Charset.forName(encoding);
        }
    }


    /**
     * Denne klassen er brukt i fra Ant.
     * <p/>
     * Hvis parameteren FailOnError er med, vil det bli kastet en exception ved ORACLE SQL error
     * fra feil med andre feilkoder enn: 02443, 02275, 00955, 01418, 00942
     * <p/>
     * Kjører ett sql script på database angitt som VM-Parametere, parametere brukt:
     * -DFailOnError=true
     *
     * @param sqlScriptNavn, navn på filen som skal lastes.
     */
    public void runScript(String sqlScriptNavn) throws Exception {
        try (Connection con = JDBCHelper.createConnection()) {
            runScript(con, lesFilFraWorkingDir(sqlScriptNavn));
            con.commit();
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

        StringBuilder tmpScript = new StringBuilder();
        try (InputStream inputStream = classLoader.getResourceAsStream(filnavn.trim())) {

            if (inputStream == null) {
                throw new OperationalException("Finner ikke filen " + filnavn + " i classpath");
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                String line;

                line = br.readLine();
                while (line != null) {
                    tmpScript.append(line).append("\n");
                    line = br.readLine();
                }
            } catch (IOException ioe) {
                logger.error("Under lesing av sqlscript " + ioe.getMessage(), ioe);
            }

        } catch (IOException e) {
            logger.error(e);
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
        StringBuilder tmpScript = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(filnavn.trim())) {
            try (InputStreamReader inputStream = new InputStreamReader(fis, sqlFileEncoding)) {
                try (BufferedReader br = new BufferedReader(inputStream)) {
                    String line;
                    try {
                        line = br.readLine();
                        while (line != null) {
                            tmpScript.append(line).append("\n");
                            line = br.readLine();
                        }
                    } catch (IOException ioe) {
                        logger.error("Under lesing av sqlscript " + ioe.getMessage(), ioe);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new OperationalException("Finner ikke filen " + filnavn);
        } catch (IOException ioe) {
            logger.error("Under lesing av scriptfil " + ioe.getMessage(), ioe);
        }
        return tmpScript.toString();
    }


    /**
     * Kjører ett sql script på anngit database kobling.
     * SqlScript statements som er ommgitt av {} skal kjøres som et prepared statement.
     * Den som kaller denne metoden må passe på og stenge den gitte connectionen selv,
     * alle statements blir stengt.
     * Den som kaller må også passe på å committe.
     *
     * @param connection database koblingen skriptet skal kjøre på.
     * @param sqlScript  scriptet som skal kjøres.
     * @return resultSet hvis det er kalt en stored procedure ommgitt av {} som gir resultatsett, gir flere linjer resultatsett blir disse lagt sammen i den rekkefølgen de er laget.
     */
    public java.sql.ResultSet[] runScript(Connection connection, String sqlScript) throws Exception {
        if (connection == null) {
            throw new ConfigurationException("Kan ikke kjøre databasescript med connection = null");
        }
        if (sqlScript == null) {
            throw new ConfigurationException("Det må angis ett sqlscript, sqlSript = null.");
        }

        List<? extends Expression> expressions = SQLStatementParser.parseExpressions(sqlScript);

        return runScript(connection, expressions);
    }

    public java.sql.ResultSet[] runScript(Connection connection, List<? extends Expression> scriptLines) throws Exception {
        List<java.sql.ResultSet> rsList = new ArrayList<java.sql.ResultSet>();
        boolean feilet = false;
        int antallFeil = 0;
        int antallWarnings = 0;
        int antallStatements = 0;

        //Kjøre en og en linje i skriptet.
        try (java.sql.Statement statement = connection.createStatement()) {

            for (Iterator<? extends Expression> iterator = scriptLines.iterator(); iterator.hasNext(); ) {
                Expression scriptLine = iterator.next();

                if (scriptLine instanceof Comment) {
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
                        } catch (SQLException e) {
                            if (isWarning(e)) {
                                logger.warn("Warning: Error executing line#" + scriptLine.getLineNumber() + ". Oracle error: " + e.getMessage());
                                antallWarnings++;

                                if (failOnWarning) {
                                    throw new Exception("Feil under kjøring av script.", e);
                                }
                            } else {
                                logger.error("Error: Error executing line#" + scriptLine.getLineNumber() + ". Oracle error: " + e.getMessage());
                                feilet = true;
                                antallFeil++;
                                if (failOnError) {
                                    throw new Exception("Feil under kjøring av script.", e);
                                }
                            }
                            logger.debug("Errors while executing : " + sqlStatement.getSql());
                        }
                    }
                }
            }
        }
        if (feilet) {
            logger.error(String.format("Script had errors! Statements: %d. Warnings: %d, errors: %d.", antallStatements, antallWarnings, antallFeil));
        } else {
            logger.info(String.format("Script completed. Statements: %d. Warnings: %d.", antallStatements, antallWarnings));
        }
        return rsList.toArray(new java.sql.ResultSet[rsList.size()]);
    }

    static final String newlineRgex = "\\u000D\\u000A|[\\u000A\\u000B\\u000C\\u000D\\u0085\\u2028\\u2029]"; //JDK8: "\\R"
    static boolean isWarning(SQLException e) {
        String msg = e.getMessage().split(newlineRgex, 0)[0];
        return msg.contains("ORA-02443")  // cannot drop nonexistent constraint
                || msg.contains("ORA-02275")  // referential constraint already exists in the table
                || msg.contains("ORA-00955")  // name is already being used by existing object
                || msg.contains("ORA-01418")  // index does not exist
                || msg.contains("ORA-00942")  // table or view does not exist
                ;
    }

    private static void callCallable(String scriptLine, Connection connection, List<java.sql.ResultSet> rsList) throws SQLException {
        //prøver å ta hensyn til at det kan returneres flere Resultset fra ett storedProcedure kall.
        logger.info("Procedure: " + scriptLine);
        try (CallableStatement callablStatement = connection.prepareCall(scriptLine)) {
            boolean isResultset = callablStatement.execute();
            if (isResultset) {
                java.sql.ResultSet rs = callablStatement.getResultSet();
                rsList.add(rs);
            }
            while (callablStatement.getMoreResults()) {
                java.sql.ResultSet rs = callablStatement.getResultSet();
                rsList.add(rs);
            }
        }
    }


    /**
     * Denne klassen er brukt i fra Ant.
     * <p/>
     * Hvis parameteren FailOnError er med, vil det bli kastet en exception ved ORACLE SQL error
     * fra feil med andre feilkoder enn: 02443, 02275, 00955, 01418, 00942
     * <p/>
     * Kjører sql script på database angitt som VM-Parametere, parametere brukt:
     * -DFailOnError=true
     * -DFailOnWarning=false
     *
     * @param args navn på filen som skal lastes.
     */
    public static void main(String[] args) throws Exception {
        if (args == null || args.length < 1) {
            throw new OperationalException("Scriptfilen(e) som skal kjøres må være anngitt som parameter.");
        }

        final SqlExecutor executor = new SqlExecutor();
        executor.failOnError = "true".equals(System.getProperty("FailOnError", "true"));
        executor.failOnWarning = "true".equals(System.getProperty("FailOnWarning", "false"));


        for (String arg : args) {
            executor.runScript(arg);
        }
    }


}
