package no.statkart.sktools.gradle.plugins.xjc

import no.statkart.sktools.gradle.plugins.xjc.util.GradleUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Directory

/**
 * todo: dokumentasjon
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 */
class XjcPlugin implements Plugin<Project> {

    final static String JAXB_CONFIGURATION_NAME = 'jaxb'
    final static String XJC_TASK_NAME = 'xjcGenerate'


    @Override
    void apply(Project project) {
        XjcPluginConvention convention = new XjcPluginConvention(project)
        project.convention.plugins.xjc = convention
        project.getPlugins().apply(JavaPlugin.class);
        Task generateTask = project.task(XJC_TASK_NAME, type: XjcTask)
        Task compileJavaTask = project.getTasks().getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME)
        compileJavaTask.dependsOn(generateTask)
        GradleUtil.makeIdeaShowBuildDirectory(project)
        ConfigurationContainer configurations = project.getConfigurations();
        Configuration moduleConfiguration = configurations.add(JAXB_CONFIGURATION_NAME).setVisible(false)
                .setDescription("Classpath for jaxb library and extensions.");
        generateTask.dependsOn(moduleConfiguration)

        Task xjcCreateJavaDir = project.task('xjcCreateJavaDir', type: Directory) { dir = convention.targetDirectory }
        Task ideaTask = project.getTasks().getByName('ideaModule')
        ideaTask.dependsOn(xjcCreateJavaDir)
    }
}
