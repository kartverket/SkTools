package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.Plugin
import org.gradle.api.internal.project.ProjectInternal
import no.statkart.sktools.gradle.plugins.weblogic.WeblogicBasePlugin
import org.gradle.api.tasks.Input
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction

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
    File source
    @Input
    boolean upload = true

    @Input
    String name
    @Input
    String targets

    @Input
    String url
    @Input
    String username
    @Input
    String password


    @Input
    String timeout = '18000'

    @Input
    boolean failOnError = false
    @Input
    boolean verbose = true


    @TaskAction
    void taskAction() {
        source = project.files(weblogicServerConfiguration.artifact).singleFile

        name = weblogicServerConfiguration.moduleName
        targets = weblogicServerConfiguration.targets

        url = weblogicServerConfiguration.url
        username = weblogicServerConfiguration.username
        password = weblogicServerConfiguration.password

        def ant = getAnt()
        ant.wldeploy(
                action: 'deploy',
                upload: upload,

                name: name,
                source: source,
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
    String name
    @Input
    String targets

    @Input
    String url
    @Input
    String username
    @Input
    String password

    @Input
    boolean graceful = false

    @Input
    boolean failOnError = false
    @Input
    boolean verbose = true


    @TaskAction
    void taskAction() {

        name = weblogicServerConfiguration.moduleName
        targets = weblogicServerConfiguration.targets

        url = weblogicServerConfiguration.url
        username = weblogicServerConfiguration.username
        password = weblogicServerConfiguration.password

        def ant = getAnt()
        ant.wldeploy(
                action: 'undeploy',
                name: name,
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
