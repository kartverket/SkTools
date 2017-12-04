package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import org.gradle.api.tasks.Input
import org.gradle.process.JavaExecSpec
import org.gradle.api.file.FileCollection
import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractSQLTask
import org.gradle.api.tasks.Optional

/**
 * Task for patching av schema over JDBC.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
abstract class DatabasePatchTask extends AbstractSQLTask {

    //SKTOOLS-84
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

        spec.setDefaultCharacterEncoding(getEncoding())

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
