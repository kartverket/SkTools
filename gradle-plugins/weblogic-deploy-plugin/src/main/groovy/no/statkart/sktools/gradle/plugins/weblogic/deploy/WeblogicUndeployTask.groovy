package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input

/**
 * Task for undeploy
 *
 * @since 1.2
 * @author Leif Lislegård
 */
class WeblogicUndeployTask extends AbstractWeblogicDeployTask {

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

}
