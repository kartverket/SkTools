package no.statkart.sktools.gradle.plugins.dbtools.database.util

import no.statkart.sktools.utils.databasepatcher.DatabasePatcher
import no.statkart.sktools.utils.databasepatcher.util.CompareUtil
import no.statkart.sktools.utils.databasepatcher.DatabasePatcherWrapper
import no.statkart.sktools.utils.databasepatcher.exception.NotFoundException

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

        try {
            return databasePatcher.getVersion().patchVersion.dbVersion
        } catch (NotFoundException nfe) {
            return null;
        }
    }

    public AbstractDatabaseTasks getTasks() {
        return configuration.getTasks();
    }

    private DatabasePatcher setUpDatabasePatcher(Closure config) {
        def wrapper = new DatabasePatcherWrapper();

        wrapper.driver = configuration.databaseConvention.driver
        wrapper.url = configuration.databaseConvention.url
        wrapper.username = configuration.databaseConvention.credentials.username
        wrapper.password = configuration.databaseConvention.credentials.password

        return wrapper;
    }

}
