package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.process.JavaExecSpec

/**
 * Task for å stoppe en applikasjon.
 *
 * @since 1.3
 * @author Leif Lislegård
 * @author Tor Egil R. Strand
 */
class WeblogicStopTask extends AbstractWeblogicDeployTask {
    protected static final Logger logger = Logging.getLogger(WeblogicStopTask.class);

    @Input
    @Optional
    Boolean graceful = null

    @Input
    @Optional
    String appversion = null


    @Override
    protected void buildCommandLine(JavaExecSpec execSpec) {
        logger.quiet("Stopper ${getDeploymentName()} i Weblogic paa ${getUrl()}")

        execSpec.args('-stop')

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
        return logger;
    }
}
