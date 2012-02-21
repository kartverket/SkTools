package no.statkart.sktools.gradle.plugins.weblogic.wsclient;


import no.statkart.sktools.gradle.plugins.weblogic.wsclient.util.GradleUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Directory

/**
 * todo: dokumentasjon
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 */
public class WsClientPlugin implements Plugin<Project> {
    public final static CONVENTION_NAME = 'wsClient'
    public final static WSCLIENT_TASK_NAME = 'wsClient'

    @Override
    void apply(Project project) {
        def convention = new WsClientPluginConvention(project)
        project.getConvention().getPlugins().put(CONVENTION_NAME, convention)
        project.getPlugins().apply(JavaPlugin.class);

        /** todo: kan antakeligvis bruke {@link org.gradle.api.internal.AbstractTask#getDynamicObjectHelper()}.setConvention(Convention) **/
        Task wsClientTask = project.task(WSCLIENT_TASK_NAME, type: WsClientTask)
        Task compileTask = project.getTasks().getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME)
        compileTask.dependsOn(wsClientTask)
        Configuration moduleConfiguration = project.configurations.add('basewar').setVisible(false)
                .setTransitive(false).setDescription("Classpath for wars to base wsclient on.");
        wsClientTask.dependsOn(moduleConfiguration)

        //todo: make the intellij stuff optional?
        GradleUtil.makeIdeaShowBuildDirectory(project)
        Task wsClientCreateJavaDir = project.task("${WSCLIENT_TASK_NAME}CreateJavaDir", type: Directory) { dir = convention.wsTargetDir }
        Task wsClientCreateResourceDir = project.task("${WSCLIENT_TASK_NAME}CreateResourceDir", type: Directory) { dir = convention.wsResourcesDir }

        Task ideaTask = project.getTasks().getByName('ideaModule')
        ideaTask.dependsOn([wsClientCreateJavaDir, wsClientCreateResourceDir])
    }

}