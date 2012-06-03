package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import no.statkart.sktools.utils.parsers.sql.SQLStatementParser
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Optional

/**
 * Task for executing av statements over JDBC.
 *
 * @author Leif Lislegård
 * @since 1.0
 */
public class SQLTask extends ConventionTask {

    private final SQLExecutor executor = new SQLExecutor()

    /**
     * Disse credentials blir benyttet dersom {@code useTaskCredentials == true}
     *
     * Dersom {@code useTaskCredentials == false}, så blir konversjonelle verdier benyttet. Det vil da si credentials ifra koblet dbTool-set
     */
    final Credentials credentials = new Credentials(getProject(), "task:${name}")
    public void clearCredentials() {
        credentials.clear()
        this.usernameSet = !credentials.isEmpty()
        this.passwordSet = !credentials.isEmpty()
        assert true
    }
    boolean useTaskCredentials = true

    @Input
    String url

    @Input
    String driver

    @Optional
    @Input
    File sqlFile

    @Optional
    @Input
    String sqlString

    @Input
    String getUsername() {
        if (useTaskCredentials) {
            return credentials.getUsername()
        } else {
            return credentials.isEmpty() ? null : credentials.getUsername()
        }
    }
    void setUsername(String username) {
        credentials.username = username
        this.usernameSet = !credentials.isEmpty()
        this.passwordSet = !credentials.isEmpty()
    }


    @Input
    String getPassword() {
        if (useTaskCredentials) {
            return credentials.getPassword()
        } else {
            return credentials.isEmpty() ? null : credentials.getPassword()
        }
    }
    void setPassword(String password) {
        credentials.password = password
        this.usernameSet = !credentials.isEmpty()
        this.passwordSet = !credentials.isEmpty()
    }



    @TaskAction
    def exec() {
        validate()

        if (sqlFile) {
            logger.info("parsing statements from file: ${sqlFile}");
            executor.statements = SQLStatementParser.parseStatements(sqlFile);
        } else {
            executor.statements = SQLStatementParser.parseStatements(sqlString);
        }

        ExecSpecs specs = new ExecSpecs();
        specs.username = getUsername();
        specs.password = getPassword();
        specs.driver = getDriver();
        specs.url = getUrl();

        executor.executeStatements(specs)
    }


    private void validate() {
        if (sqlFile == null && sqlString == null) {
            throw new Exception("sqlFile eller sqlString må anngis!")
        }

        if (sqlFile != null) {
            if (!sqlFile.exists()) {
                throw new Exception("File does not exist! sqlFile=${sqlFile}")
            }

            if (sqlString != null) {
                throw new Exception("Enten sqlFile eller sqlString kan anngis!")
            }
        }

        if (sqlString != null) {
            if (sqlString.trim().isEmpty()) {
                throw new Exception("sqlString kan ikke være tom! sqlString='${sqlString}'")
            }

            if (sqlFile != null) {
                throw new Exception("Enten sqlFile eller sqlString kan anngis!")
            }
        }

    }



}
