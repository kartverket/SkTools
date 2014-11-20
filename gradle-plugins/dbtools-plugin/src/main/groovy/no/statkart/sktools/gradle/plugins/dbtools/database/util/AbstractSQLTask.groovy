package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.internal.ConventionTask
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Input

/**
 * Task for executing av statements over JDBC.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
abstract class AbstractSQLTask extends ConventionTask {

    //SKTOOLS-27, SKTOOLS-84
    boolean failOnError = !project.gradle.startParameter.isContinueOnFailure()

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

    //SKTOOLS-21
    String encoding


    abstract File getSqlFile();

    abstract void validate(); //SKTOOLS-81

    public abstract Logger getLogger();

    protected void validateAbstractSQLTask() {

        if (getDriver() == null) {
            throw new Exception("Value for attribute 'driver' not set!")
        }
        if (getUrl() == null) {
            throw new Exception("Value for attribute 'url' not set!")
        }
        if (getUsername() == null) {
            throw new Exception("Value for attribute 'username' not set!")
        }
        if (getPassword() == null) {
            throw new Exception("Value for attribute 'password' not set!")
        }

    }

}
