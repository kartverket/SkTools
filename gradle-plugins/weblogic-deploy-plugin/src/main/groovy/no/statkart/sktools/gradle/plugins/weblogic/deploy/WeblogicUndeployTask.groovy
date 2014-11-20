package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * Task for undeploy
 *
 * @since 1.2
 * @author Leif Lislegård
 */
class WeblogicUndeployTask extends AbstractWeblogicDeployTask {
    protected static final Logger logger = Logging.getLogger(WeblogicUndeployTask.class);

    @Input
    @Optional
    Boolean graceful = null

    @Input
    @Optional
    String appversion = null


    @TaskAction
    void deploy() {
        logger.quiet("Undeployment av ${getDeploymentName()} til Weblogic paa ${getUrl()}")

        def args = [
                action: 'undeploy',
                name: getDeploymentName(),
                targets: getTargets(),

                adminurl: getUrl(),
                user: getUsername(),
                password: getPassword(),

                failonerror: getFailOnError(),
                verbose: getVerbose(),
        ]

        if (getGraceful() != null) {
            args.graceful = getGraceful()
        }

        if (getAppversion() != null) {
            args.appversion = getAppversion()
        }

        ant.wldeploy(args)
    }

    public Logger getLogger() {
        return logger;
    }
}
