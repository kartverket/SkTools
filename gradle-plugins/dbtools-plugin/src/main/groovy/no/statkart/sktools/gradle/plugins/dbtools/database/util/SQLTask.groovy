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
public class SQLTask extends AbstractSQLTask {

    private final SQLExecutor executor = new SQLExecutor()

    @Optional
    @Input
    File sqlFile

    @Optional
    @Input
    String sqlString


    @TaskAction
    def exec() {
        validate()

        if (getSqlFile()) {
            logger.info("parsing statements from file: ${getSqlFile()}");
            executor.statements = SQLStatementParser.parseStatements(getSqlFile(), getEncoding());
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


    void validate() {
        validateAbstractSQLTask()

        if (getSqlFile() == null && getSqlString() == null) {
            throw new Exception("sqlFile eller sqlString må anngis!")
        }

        if (getSqlFile() != null) {
            if (!getSqlFile().exists()) {
                throw new Exception("File does not exist! sqlFile=${project.relativePath(getSqlFile())}")
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
