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

    @Input
    boolean library = false

    @TaskAction
    void deploy() {
        logger.quiet("Deployment av ${getDeploymentName()} til Weblogic paa ${getUrl()}")

        Map args = [
                action: 'deploy',
                upload: getUpload(),

                name: getDeploymentName(),
                source: getFile(),
                targets: getTargets(),

                adminurl: getUrl(),
                user: getUsername(),
                password: getPassword(),

                timeout: getTimeout(),

                failonerror: getFailOnError(),
                verbose: getVerbose(),
        ]

        if (library) {
            args.library = true
        }

        ant.wldeploy(args)
    }

}