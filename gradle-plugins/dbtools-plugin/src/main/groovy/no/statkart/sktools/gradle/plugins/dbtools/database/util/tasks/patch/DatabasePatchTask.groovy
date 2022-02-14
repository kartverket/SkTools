package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractSQLTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.process.JavaExecSpec

/**
 * Task for patching av schema over JDBC.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
abstract class DatabasePatchTask extends AbstractSQLTask {

    /**
     * Bestemmer om tasken skal feile ved enkelte feiltyper eller ikke.
     * Se dokumentasjon av {@link no.statkart.sktools.utils.databasepatcher.DatabasePatcher}
     */
    boolean failOnWarning = failOnError

    //SKTOOLS-77
    @Input
    @Optional
    String schema

    @Input
    String component

    @Input
    FileCollection classpath


    protected JavaExecSpec configureDefaultSpec(JavaExecSpec spec) {
        spec.setMain("no.statkart.sktools.utils.databasepatcher.DatabasePatcher")

        spec.args('-component', getComponent())

        spec.setClasspath(getClasspath())

        spec.systemProperties.put('sql.file.encoding', getEncoding())
        spec.setDefaultCharacterEncoding(System.getProperty('file.encoding')) //samme file.encoding unngår forvrengning av loggoutput til konsoll

        //see documentation https://www.slf4j.org/api/org/slf4j/impl/SimpleLogger.html
        spec.systemProperties.put('org.slf4j.simpleLogger.defaultLogLevel', getLogger().isDebugEnabled() ? 'trace' : logger.isInfoEnabled() ? 'debug' : 'info')
        spec.systemProperties.put('org.slf4j.simpleLogger.showShortLogName', logger.isInfoEnabled())
        spec.systemProperties.put('org.slf4j.simpleLogger.showLogName', false)
        spec.systemProperties.put('org.slf4j.simpleLogger.showThreadName', false)
        spec.systemProperties.put('org.slf4j.simpleLogger.showDateTime', false)

        spec.systemProperties.put('hibernate.connection.driver_class', getDriver())
        spec.systemProperties.put('hibernate.connection.url', getUrl())
        spec.systemProperties.put('hibernate.connection.username', getUsername())
        spec.systemProperties.put('hibernate.connection.password', getPassword())

        if (getSchema() != null) {
            spec.systemProperties.put('hibernate.connection.schema', getSchema())
        }

        spec.systemProperties.put('failOnError', getFailOnError())
        spec.systemProperties.put('failOnWarning', getFailOnWarning())

        spec.setMaxHeapSize('128m')

        return spec
    }

    @Override
    void validate() {

        if (getComponent() == null) {
            throw new Exception("Value for attribute 'component' not set!")
        }

        validateAbstractSQLTask()
    }
}
