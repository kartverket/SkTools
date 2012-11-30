package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.tasks.Input
import org.gradle.api.internal.ConventionTask

/**
 * Task for executing av statements over JDBC.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
abstract class AbstractSQLTask extends ConventionTask {

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

    //SKIF-211
    String encoding


    abstract File getSqlFile();
}
