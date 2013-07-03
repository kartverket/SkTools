package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Task for å starte en applikasjon.
 *
 * @since 1.3
 * @author Leif Lislegård
 * @author Tor Egil R. Strand
 */
class WeblogicStartTask extends AbstractWeblogicDeployTask {

    @Input
    Boolean graceful = null

    @Input
    String appversion = null


    @TaskAction
    void deploy() {
        logger.quiet("Stopper ${getDeploymentName()} i Weblogic paa ${getUrl()}")

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
