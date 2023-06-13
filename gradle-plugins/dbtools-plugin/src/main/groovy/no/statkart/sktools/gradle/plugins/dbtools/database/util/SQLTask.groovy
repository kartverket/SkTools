package no.statkart.sktools.gradle.plugins.dbtools.database.util

import groovy.transform.PackageScope
import no.statkart.sktools.utils.parsers.sql.SQLStatementParser
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

import java.nio.charset.Charset
import java.nio.file.Files

/**
 * Task for executing av statements over JDBC.
 *
 * @author Leif Lislegård
 * @since 1.0
 */
public class SQLTask extends AbstractSQLTask {
    protected static final Logger logger = Logging.getLogger(SQLTask.class);

    protected final SQLExecutor executor = new SQLExecutor()

    @Internal
    File sqlFile

    String sqlString


    @TaskAction
    def exec() {
        validate()

        parseStatements()

        ExecSpecs specs = new ExecSpecs();
        specs.username = getUsername();
        specs.password = getPassword();
        specs.driver = getDriver();
        specs.url = getUrl();
        specs.failOnError = failOnError.get()

        executor.executeStatements(specs)
    }

    @PackageScope
    void parseStatements() {
        String sql = fillInnProperties(getSqlString())

        executor.statements = SQLStatementParser.parseStatements(sql);
    }

    /**
     * Substitutes tokens with property value. Token syntax: {@code @property@}.
     */
    @PackageScope
    String fillInnProperties(String sql) {
        eachProperty({ key, value ->
            sql = sql.replace("@${key}@", value.toString())
        });

        return sql
    }


    void validate() {
        validateAbstractSQLTask()
    }


    @Internal //no up-to-date check
    String getSqlString() {
        if (sqlString != null && getSqlFile() != null) {
            throw new Exception("Enten sqlFile eller sqlString kan angis!")
        }

        if (sqlString != null) return sqlString

        File file = getSqlFile()
        Objects.requireNonNull(file, "Enten sqlFile eller sqlString må angis!")

        logger.info('parsing statements from file: {}', file);
        Charset charset = Charset.forName(encoding.get())
        return String.join('\n', Files.readAllLines(file.toPath(), charset))
    }

    @Internal
    public Logger getLogger() {
        return logger;
    }

}
