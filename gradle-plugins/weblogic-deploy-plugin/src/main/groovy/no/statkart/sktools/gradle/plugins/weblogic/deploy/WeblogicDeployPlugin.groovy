package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.Plugin
import org.gradle.api.internal.project.ProjectInternal

/**
 * Plugin for deployment-tasker til Weblogic Server.
 *
 * @author Tor Egil R. Strand
 * @author Leif Lislegård
 * @since 1.2
 */
class WeblogicDeployPlugin implements Plugin<ProjectInternal> {
    public final static String WEBLOGIC_DEPLOY_CONVENTION_NAME = 'weblogicDeployConvention'

    @Override
    void apply(ProjectInternal project) {

        WeblogicDeployConvention convention = new WeblogicDeployConvention(project)
        project.convention.plugins.put(WEBLOGIC_DEPLOY_CONVENTION_NAME, convention)

    }
}

