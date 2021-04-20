package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.process.JavaExecSpec

/**
 * Task for undeploy
 *
 * @since 1.2
 * @author Leif Lislegård
 */
class WeblogicUndeployTask extends AbstractWeblogicDeployTask {
    protected static final Logger logger = Logging.getLogger(WeblogicUndeployTask.class)

    @Input
    @Optional
    Boolean graceful = null

    @Input
    @Optional
    String appversion = null


    @Override
    protected void buildCommandLine(JavaExecSpec execSpec) {
        logger.quiet("Undeployment av ${getDeploymentName()} til Weblogic paa ${getUrl()}")

        execSpec.args('-undeploy')

        if (getGraceful() != null) {
            execSpec.args('-graceful', getGraceful())
        }

        if (getAppversion() != null) {
            execSpec.args('-appversion', getAppversion())
        }
    }

    @Internal
    @Override
    Logger getLogger() {
        return logger
    }
}
