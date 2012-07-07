package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.Plugin
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Plugin for deployment-tasker til Weblogic Server.
 *
 * @author Tor Egil R. Strand
 * @author Leif Lislegård
 * @since 1.2
 */
class WeblogicDeployPlugin implements Plugin<ProjectInternal> {
    public final static String WEBLOGIC_DEPLOY_CONVENTION_NAME = "weblogicDeployConvention"

    @Override
    void apply(ProjectInternal project) {

        WeblogicDeployConvention convention = new WeblogicDeployConvention(project)
        project.convention.plugins.put(WEBLOGIC_DEPLOY_CONVENTION_NAME, convention)

    }
}

class WeblogicDeployTask extends AbstractWeblogicDeployTask {

    @Input
    File file

    @Input
    boolean upload = true

    @Input
    String timeout = '18000'




    @TaskAction
    void taskAction() {

        def ant = getAnt()
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

class WeblogicUndeployTask extends AbstractWeblogicDeployTask {


    @Input
    boolean graceful = false



    @TaskAction
    void taskAction() {

        def ant = getAnt()
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
