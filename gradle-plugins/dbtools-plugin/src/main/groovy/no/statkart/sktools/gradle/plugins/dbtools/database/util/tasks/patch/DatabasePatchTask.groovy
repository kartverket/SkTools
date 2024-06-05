package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractSQLTask
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
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
    @Internal
    final Property<Boolean> failOnWarning = project.getObjects().property(Boolean).convention(failOnError)

    @Internal
    final Property<String> schema = project.getObjects().property(String)

    @Internal
    final Property<String> component = project.getObjects().property(String).convention('null')

    @Internal
    final Property<FileCollection> classpath = project.getObjects().property(FileCollection)


    protected JavaExecSpec configureDefaultSpec(JavaExecSpec spec) {
        spec.getMainClass().set("no.statkart.sktools.utils.databasepatcher.DatabasePatcher")

        spec.args('-component', component.get())

        spec.setClasspath(classpath.get())

        spec.systemProperties.put('sql.file.encoding', encoding.get())
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

        if (schema.isPresent()) {
            spec.systemProperties.put('hibernate.connection.schema', schema.get())
        }

        spec.systemProperties.put('failOnError', failOnError.get())
        spec.systemProperties.put('failOnWarning', failOnWarning.get())

        spec.setMaxHeapSize('128m')

        return spec
    }

    @Override
    void validate() {

        validateAbstractSQLTask()
    }
}
