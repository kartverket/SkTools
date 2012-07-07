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
        logger.quiet("Undeployment av ${deploymentName} til Weblogic paa ${url}")

        ant.wldeploy(
                action: 'undeploy',
                name: deploymentName,
                targets: targets,

                adminurl: url,
                user: username,
                password: password,

                graceful: graceful,

                failonerror: failOnError,
                verbose: verbose,
        )
    }

}
