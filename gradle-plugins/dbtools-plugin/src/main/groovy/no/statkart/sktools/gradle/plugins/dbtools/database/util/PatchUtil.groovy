package no.statkart.sktools.gradle.plugins.dbtools.database.util

import groovy.sql.Sql
import no.statkart.sktools.utils.databasepatcher.DatabasePatcher
import no.statkart.sktools.utils.databasepatcher.util.CompareUtil
import no.statkart.sktools.utils.databasepatcher.exception.NotFoundException

import java.sql.Connection
import java.sql.SQLException

/**
 * Verktøy for runtime sjekker mot basen. Disse kan blir utført via deklarasjon av tasker.
 *
 * PS: Deklararert bruk av disse metodene skjer kun innenfor scope for toolset { } -konfigurasjon.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
public class PatchUtil {

    private final PatchConfiguration configuration;

    PatchUtil(PatchConfiguration configuration) {
        this.configuration = configuration;
    }


    public void assertMinVersion(String version) {
        String currentVersion = getCurrentVersion()
        if (currentVersion == null) {
            throw new AssertionError(String.format("No current version defined for module '%s'", configuration.name))
        }
        if (CompareUtil.compareDBVersions(currentVersion, version) < 0) {
            throw new AssertionError(String.format("Expected minimum current version to be '%s' but was '%s'", version, currentVersion))
        }
    }

    /**
     * @return currentVersion for module or {@code null} if not defined
     */
    public String getCurrentVersion() {
        DatabasePatcher databasePatcher = setUpDatabasePatcher()
        databasePatcher.component = configuration.name
        databasePatcher.schema = configuration.schema

        try {
            return databasePatcher.getVersion().patchVersion.dbVersion
        } catch (NotFoundException ignored) {
            return null;
        }
    }

    public AbstractDatabaseTasks getTasks() {
        return configuration.getTasks();
    }

    private DatabasePatcher setUpDatabasePatcher() {
        return new DatabasePatcher(new DatabasePatcher.ConnectionProvider() {
            @Override
            Connection get() throws SQLException {
                def driver = configuration.databaseConvention.driver
                def url = configuration.databaseConvention.url
                def username = configuration.databaseConvention.credentials.username
                def password = configuration.databaseConvention.credentials.password

                final Sql sql = Sql.newInstance(url, username, password, driver);
                final Connection connection = sql.getConnection();
                connection.setAutoCommit(false);
                return connection;
            }
        })
    }

}
