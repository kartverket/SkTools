package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.internal.ConventionTask
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

import java.util.function.BiConsumer

/**
 * Task for executing av statements over JDBC.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
abstract class AbstractSQLTask extends ConventionTask {

    @Input
    boolean failOnError = !project.gradle.startParameter.isContinueOnFailure()

    @Internal
    final Credentials credentials = new Credentials("task:${name}"
        , getProject().getProviders().provider { hasProperty('username') ? property('username') as String : null }
        , getProject().getProviders().provider { hasProperty('password') ? property('password') as String : null }
    )


    final Property<String> urlProvider = project.getObjects().property(String)
    @Input
    String getUrl() {
        return urlProvider.getOrNull();
    }
    void setUrl(String url) {
        urlProvider.set(url)
    }

    final Property<String> driverProvider = project.getObjects().property(String)
    @Input
    String getDriver() {
        return driverProvider.getOrNull()
    }
    void setDriver(String driver) {
        driverProvider.set(driver)
    }

    @Input
    @Internal
    String getUsername() {
        return credentials.getUsername()
    }
    void setUsername(String username) {
        credentials.username = username
    }


    @Input
    @Internal
    String getPassword() {
        return credentials.getPassword()
    }
    void setPassword(String password) {
        credentials.password = password
    }

    //SKTOOLS-21
    String encoding

    @Input
    String getEncoding() {
        if (encoding != null) {
            return encoding;
        } else {
            Map<String, String> sysProperties = new HashMap<String, String>();
            sysProperties.putAll((Map) System.getProperties());
            sysProperties.putAll(project.gradle.startParameter.getSystemPropertiesArgs());

            return sysProperties.get('sql.file.encoding') ?: sysProperties.get('file.encoding')
        }
    }


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

    protected void eachProperty(BiConsumer<String, Object> consumer) {
        for (Map.Entry<String, Object> entry : getExtensions().getExtraProperties().getProperties().entrySet()) {
            consumer.accept(entry.getKey(), entry.getValue());
        }

        //noinspection GroovyAssignabilityCheck
        Map<String, Object> toolsetProperties = getExtensions().findByName(AbstractDatabaseConvention.TOOLSET_PROPERTIES)
        if (toolsetProperties) {
            for (Map.Entry<String, Object> entry : toolsetProperties.entrySet()) {
                consumer.accept(entry.getKey(), entry.getValue());
            }
        }
    }

}
