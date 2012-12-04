package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.tasks.Input
import org.gradle.process.JavaExecSpec
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Optional

/**
 * Task for patching av schema over JDBC.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
abstract class DatabasePatchTask extends AbstractSQLTask {

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


        spec.setMaxHeapSize('128m')

        return spec
    }


}
