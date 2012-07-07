package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Task for deploy
 *
 * @since 1.2
 * @author Leif Lislegård
 */
class WeblogicDeployTask extends AbstractWeblogicDeployTask {

    @Input
    File file

    @Input
    boolean upload = true

    @Input
    String timeout = '18000'



    @TaskAction
    void deploy() {
        logger.quiet("Deployment av ${deploymentName} til Weblogic paa ${url}")

        ant.wldeploy(
                action: 'deploy',
                upload: upload,

                name: deploymentName,
                source: file,
                targets: targets,

                adminurl: url,
                user: username,
                password: password,

                timeout: timeout,

                failonerror: failOnError,
                verbose: verbose,
        )
    }

}