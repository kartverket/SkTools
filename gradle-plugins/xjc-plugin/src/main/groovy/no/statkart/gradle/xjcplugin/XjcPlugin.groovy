package no.statkart.gradle.xjcplugin

import no.statkart.gradle.util.GradleUtil
import no.statkart.gradle.util.FileUtil
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.Task
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Directory

/**
 * @author eldtho
 */
class XjcPlugin implements Plugin<Project> {

    public static final String JAXB_CONFIGURATION_NAME = 'jaxb'

    @Override
    void apply(Project project) {
        XjcPluginConvention convention = new XjcPluginConvention(project)
        project.convention.plugins.statKartXjc = convention
        project.getPlugins().apply(JavaPlugin.class);
        Task generateTask = project.task('xjcGenerate', type: XjcGenerateTask)
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

class XjcGenerateTask extends DefaultTask {
    XjcPluginConvention convention = getProject().convention.plugins.statKartXjc
    List schemas = convention.schemas
    String targetDirectory = convention.targetDirectory

    XjcGenerateTask() {
        Project project = getProject()
        project.afterEvaluate {
            schemas.each { s -> inputs.dir(s.dir) }
            if (!project.sourceSets.main.java.srcDirs.contains(targetDirectory)) {
                project.sourceSets.main.java.srcDirs += targetDirectory
            }
            outputs.dir(targetDirectory)
        }
    }

    @TaskAction
    def generate() {
        if (schemas.isEmpty()) {
            throw new GradleException("No schemas defined for xjc generation: statKartXjc.schema(dir, includes)")
        }
        FileCollection libraries = project.configurations[XjcPlugin.JAXB_CONFIGURATION_NAME]
        ant.taskdef(name: 'xjc', classname: 'com.sun.tools.xjc.XJCTask', classpath: libraries.asPath)
        ant.mkdir(dir: targetDirectory)
        schemas.each { s ->
            ant.xjc(destDir: targetDirectory, extension: s.withGrunnbokDoc != null) {
                if (s.withGrunnbokDoc) {
                    arg(line: "-grunnbokDoc $s.withGrunnbokDoc")
                }
                if (s.withListAdapter) {
                    arg(line: '-listgen')
                }
                schema(dir: s.dir, includes: s.includes)
            }
        }
    }
}

class XjcPluginConvention {
    File targetDirectory
    List<Schema> schemas = new ArrayList<Schema>()

    XjcPluginConvention(Project project) {
        targetDirectory = FileUtil.append(project.getBuildDir(), 'generated', 'main', 'java')
    }

    void schema(dir, includes, withGrunnbokDoc, withListAdapter) {
        schemas.add(new Schema(dir: dir, includes: includes, withGrunnbokDoc: withGrunnbokDoc, withListAdapter: withListAdapter))
    }

    void schema(dir, includes, withGrunnbokDoc) {
        schemas.add(new Schema(dir: dir, includes: includes, withGrunnbokDoc: withGrunnbokDoc))
    }

    void schema(dir, includes) {
        schemas.add(new Schema(dir: dir, includes: includes))
    }

    def statKartXjc(Closure closure) {
        closure.delegate = this
        closure()
    }
}

class Schema {
    String dir;
    String includes;
    String withGrunnbokDoc = null;
    boolean withListAdapter = false;
}
