package no.statkart.sktools.gradle.plugins.weblogic.deploy

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
    boolean graceful = false



    @TaskAction
    void deploy() {
        logger.quiet("Undeployment av ${getDeploymentName()} til Weblogic paa ${getUrl()}")

        ant.wldeploy(
                action: 'undeploy',
                name: getDeploymentName(),
                targets: getTargets(),

                adminurl: getUrl(),
                user: getUsername(),
                password: getPassword(),

                graceful: getGraceful(),

                failonerror: getFailOnError(),
                verbose: getVerbose(),
        )
    }

}
