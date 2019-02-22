package no.statkart.sktools.utils.databasepatcher;

import no.statkart.sktools.utils.databasepatcher.exception.ConfigurationException;
import no.statkart.sktools.utils.databasepatcher.exception.OperationalException;
import no.statkart.sktools.utils.parsers.sql.SQLStatementParser;
import no.statkart.sktools.utils.parsers.sql.model.Comment;
import no.statkart.sktools.utils.parsers.sql.model.Expression;
import no.statkart.sktools.utils.parsers.sql.model.PromptStatement;
import no.statkart.sktools.utils.parsers.sql.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Er laget for å kunne kjøre database script på en bestemt databasekobling.
 *
 * @author Jan Holmen
 * @author Henrik Fredholm
 */
public class SqlExecutor {
    private static final Logger logger = LoggerFactory.getLogger(SqlExecutor.class);

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

            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;

                line = br.readLine();
                while (line != null) {
                    tmpScript.append(line).append("\n");
                    line = br.readLine();
                }
            }

        } catch (IOException ioe) {
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
        List<java.sql.ResultSet> rsList = new ArrayList<>();
        boolean feilet = false;
        int antallFeil = 0;
        int antallWarnings = 0;
        int antallStatements = 0;

        //Kjøre en og en linje i skriptet.
        try (java.sql.Statement statement = connection.createStatement()) {

            for (Expression scriptLine : scriptLines) {
                if (scriptLine instanceof Comment) {
                    if (scriptLine instanceof PromptStatement) {
                        logger.info(((Comment) scriptLine).getText()); //inneholder allerede "PROMPT: "
                    } else {
                        logger.debug("comment: {}", ((Comment) scriptLine).getText());
                    }
                    continue; // gjør ikke noe mer for kommentarer
                }

                if (scriptLine instanceof Statement) {
                    Statement sqlStatement = (Statement) scriptLine;
                    antallStatements++;

                    if (sqlStatement.getSql().startsWith("{")) {
                        callCallable(sqlStatement.getSql(), connection, rsList);
                    } else {
                        try {
                            logger.debug("Executing :\n{}", sqlStatement.getSql());
                            statement.executeUpdate(sqlStatement.getSql());
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
                            logger.debug("Errors while executing : {}",  sqlStatement.getSql());
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

    static boolean isWarning(SQLException e) {
        String msg = e.getMessage();
        //ORA-02443: Cannot drop constraint - nonexistent constraint
        //ORA-02275: such a referential constraint already exists in the table
        //ORA-00955: name is already being used by existing object
        //ORA-01418: specified index does not exist
        //ORA-00942: table or view does not exist
        return msg.contains("02443") || msg.contains("02275") || msg.contains("00955") || msg.contains("01418") || msg.contains("00942");
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
