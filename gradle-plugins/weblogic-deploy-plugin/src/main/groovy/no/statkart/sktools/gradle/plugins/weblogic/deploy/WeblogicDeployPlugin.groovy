package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.Plugin
import org.gradle.api.internal.project.ProjectInternal
import no.statkart.sktools.gradle.plugins.weblogic.WeblogicBasePlugin
import org.gradle.api.tasks.Input
import org.gradle.api.Task

/**
 * Pluging for tasker for deploying til weblogic.
 *
 * @author Tor Egil R. Strand
 * @since 1.2
 */
class WeblogicDeployPlugin implements Plugin<ProjectInternal> {
    public final static String WEBLOGIC_DEPLOY_CONVENTION_NAME = "weblogicDeployConvention"

    @Override
    void apply(ProjectInternal project) {
        project.plugins.apply(WeblogicBasePlugin.class)

        WeblogicDeployConvention convention = new WeblogicDeployConvention()
        project.convention.plugins.put(WEBLOGIC_DEPLOY_CONVENTION_NAME, convention)

        WeblogicDeployTask deployTask = project.tasks.add("deployOnly", WeblogicDeployTask.class)
        deployTask.description = "Ufører deploying uten å undeploye først"
        deployTask.weblogicServerConfiguration = convention.weblogicDeploy

        WeblogicUndeployTask undeployTask = project.tasks.add("undeploy", WeblogicUndeployTask.class)
        deployTask.description = "Undeployer"
        undeployTask.weblogicServerConfiguration = convention.weblogicDeploy

        Task task = project.tasks.add("deploy")
        task.description = "Undeployer og så deployer"
        task.dependsOn {
            // Merk at dette er en closure for sen evaluering
            convention.weblogicDeploy.artifact
        }
        task.doLast {
            undeployTask.execute()
            deployTask.execute()
        }
    }
}

class WeblogicDeployTask extends AbstractWeblogicDeployTask {
    @Input
    def getConfiguration() {
        return weblogicServerConfiguration.artifact
    }

    @Override
    void taskAction() {
        super.taskAction()

        ant.wldeploy(
                action: 'deploy',
                source: project.files(weblogicServerConfiguration.artifact).singleFile,
                name: weblogicServerConfiguration.moduleName,
                user: weblogicServerConfiguration.username,
                password: weblogicServerConfiguration.password,
                adminurl: weblogicServerConfiguration.url,
                targets: weblogicServerConfiguration.targets,
                upload: 'true',
                timeout: '18000',
                verbose: 'true',
                failonerror: 'true'
        )
    }

}

class WeblogicUndeployTask extends AbstractWeblogicDeployTask {
    public boolean failOnError = false

    @Override
    void taskAction() {
        super.taskAction()

        ant.wldeploy(
                action: 'undeploy',
                name: weblogicServerConfiguration.moduleName,
                user: weblogicServerConfiguration.username,
                password: weblogicServerConfiguration.password,
                adminurl: weblogicServerConfiguration.url,
                targets: weblogicServerConfiguration.targets,
                graceful: true,
                verbose: true,
                failonerror: failOnError
        )
    }
}
