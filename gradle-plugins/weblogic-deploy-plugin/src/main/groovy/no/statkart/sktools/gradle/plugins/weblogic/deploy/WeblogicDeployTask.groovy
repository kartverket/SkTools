package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * Task for deploy
 *
 * @since 1.2
 * @author Leif Lislegård
 */
class WeblogicDeployTask extends AbstractWeblogicDeployTask {
    protected static final Logger logger = Logging.getLogger(WeblogicDeployTask.class);

    @InputFiles
    Object file

    @Input
    boolean upload = true

    @Input
    String timeout = '18000'

    @Input
    boolean library = false

    @Input
    @Optional
    String appversion = null

    @TaskAction
    void deploy() {
        logger.quiet("Deployment av ${getDeploymentName()} til Weblogic paa ${getUrl()}")

        Map args = [
                action: 'deploy',
                upload: getUpload(),

                name: getDeploymentName(),
                source: project.files(getFile()).singleFile,
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

        if (getAppversion() != null) {
            args.appversion = getAppversion()
        }

        ant.wldeploy(args)
    }

    public Logger getLogger() {
        return logger;
    }

}