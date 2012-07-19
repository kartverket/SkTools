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
     * Dersom {@code useDefaultCredentials == true}, så blir konversjonelle verdier benyttet. Dvs credentials ifra koblet dbTool-set
     */
    final Credentials credentials = new Credentials("task:${name}", project.properties)
    Credentials defaultCredentials = null
    boolean useDefaultCredentials = false

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
        if (useDefaultCredentials && credentials.isEmpty()) {  //dersom en ikke skal benytte alternative credentials for task
            return defaultCredentials.getUsername()
        }
        return credentials.getUsername()
    }
    void setUsername(String username) {
        credentials.username = username
    }


    @Input
    String getPassword() {
        if (useDefaultCredentials && credentials.isEmpty()) {  //dersom en ikke skal benytte alternative credentials for task
            return defaultCredentials.getPassword()
        }
        return credentials.getPassword()
    }
    void setPassword(String password) {
        credentials.password = password
    }

    //SKIF-206
    boolean failOnError = true


    @TaskAction
    def exec() {
        validate()

        if (sqlFile) {
            logger.info("parsing statements from file: ${getSqlFile()}");
            executor.statements = SQLStatementParser.parseStatements(getSqlFile());
        } else {
            executor.statements = SQLStatementParser.parseStatements(getSqlString());
        }

        ExecSpecs specs = new ExecSpecs();
        specs.username = getUsername();
        specs.password = getPassword();
        specs.driver = getDriver();
        specs.url = getUrl();
        specs.failOnError = getFailOnError()

        executor.executeStatements(specs)
    }


    private void validate() {
        if (getSqlFile() == null && getSqlString() == null) {
            throw new Exception("sqlFile eller sqlString må anngis!")
        }

        if (getSqlFile() != null) {
            if (!getSqlFile().exists()) {
                throw new Exception("File does not exist! sqlFile=${getSqlFile()}")
            }

            if (getSqlString() != null) {
                throw new Exception("Enten sqlFile eller sqlString kan anngis!")
            }
        }

        if (getSqlString() != null) {
            if (getSqlString().trim().isEmpty()) {
                throw new Exception("sqlString kan ikke være tom! sqlString='${getSqlString()}'")
            }

            if (getSqlFile() != null) {
                throw new Exception("Enten sqlFile eller sqlString kan anngis!")
            }
        }

    }



}
