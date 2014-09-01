package no.statkart.sktools.gradle.plugins.filterresources;

import java.io.File;

/**
 * @since 1.2
 * @author Leif Lislegård
 */
public interface FilterResourcesSourceSetOutput {

    /**
     * Configures the path for filtered resources output.
     *
     * @since 1.2 - SKIF-173
     * @return this
     */
    FilterResourcesSourceSetOutput filterResourcesOutput(Object path);

    /**
     * @since 1.2 - SKIF-173
     * @return output dir for filtered resources
     */
    File getFilterResourcesOutputDir();

}
