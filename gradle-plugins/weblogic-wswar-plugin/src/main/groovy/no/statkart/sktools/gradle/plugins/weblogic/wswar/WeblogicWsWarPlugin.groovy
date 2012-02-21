package no.statkart.sktools.gradle.plugins.weblogic.wswar

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin

/**
 * todo: dokumentasjon
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 */
class WeblogicWsWarPlugin implements Plugin<Project> {

    final static CONVENTION_NAME = 'statKartWeblogicWsWar'
    final static WS_WAR_TASK_NAME = 'weblogicWsWar'

    @Override
    void apply(Project project) {
        WeblogicWsWarPluginConvention convention = new WeblogicWsWarPluginConvention()
        project.getConvention().getPlugins().put(CONVENTION_NAME, convention)
        project.getPlugins().apply(JavaPlugin.class);
        Task warTask = project.task(WS_WAR_TASK_NAME, type: WeblogicWsWarTask)
        Task jarTask = project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME)
        jarTask.dependsOn(warTask)
        warTask.dependsOn(project.getConfigurations().getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME))
        // We need for the compile task not be ran. So that we're certain that all compilation is done by jwsc
        project.task(JavaPlugin.COMPILE_JAVA_TASK_NAME, type: DefaultTask, overwrite: true)
        project.afterEvaluate {
            if (it.state.failure == null) {
                warTask.prepare()
            }
        }
    }
}
