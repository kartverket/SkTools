package no.statkart.gradle.weblogicwswarplugin

import no.statkart.gradle.util.FileUtil
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.GradleException

class WsWarPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        WsWarPluginConvention convention = new WsWarPluginConvention()
        project.getConvention().getPlugins().put('statKartWeblogicWsWar', convention)
        project.getPlugins().apply(JavaPlugin.class);
        Task warTask = project.task('weblogicWsWar', type: WeblogicWsWarTask)
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

class WeblogicWsWarTask extends DefaultTask {
    WsWarPluginConvention convention = getProject().convention.plugins.statKartWeblogicWsWar
    String jswcWarDirName = getProject().name + ".war"
    String buildEarDir = new File(getProject().getBuildDir(), 'ear')
    File buildEarDirFile = new File(buildEarDir)

    void prepare() {
        inputs.dir(convention.sourceDir)
        outputs.dir(new File(buildEarDirFile, jswcWarDirName))
        configureWar()
    }

    private def configureWar() {
        Task jarTask = getProject().getTasks().getByName(JavaPlugin.JAR_TASK_NAME)
        jarTask.extension = 'war'
        jarTask.getMainSpec().getMainSpec().getSourcePaths().clear()
        jarTask.from(new File(buildEarDirFile, jswcWarDirName)) {
            exclude 'WEB-INF/web.xml'
        }
        jarTask.from(getProject().file("src/main/webapp"))
        String webSourceDir = convention.webSourceDir
        if (webSourceDir != null) {
            File webSourceDirFile = getProject().file(webSourceDir)
            if (!webSourceDirFile.exists() || !webSourceDirFile.isDirectory()) {
                throw new GradleException("Unable to find directory $webSourceDir")
            }
            jarTask.from(webSourceDirFile)
        }
    }

    @TaskAction
    def war() {
        ant.taskdef(name: 'jwsc', classname: 'weblogic.wsee.tools.anttasks.JwscTask', classpath: convention.weblogicLibraries.asPath)
        File sourceDirFile = getProject().file(convention.sourceDir)
        if (!sourceDirFile.exists() || !sourceDirFile.isDirectory()) {
            throw new GradleException("The source directory $sourceDirFile.path does not exist")
        }
        ant.jwsc(srcdir: sourceDirFile.getPath(), destdir: buildEarDir, keepGenerated: 'true',
                classpath: convention.classpath.asPath, verbose: 'false', debug: 'true',
                includeantruntime: 'false', fork: 'true') {
            module(name: jswcWarDirName, contextpath: 'notimportantsincewethrowawaytheear', explode: 'true') {
                sourceDirFile.traverse { File file ->
                    if (file.getName().endsWith('WSBean.java')) {
                        String serviceUri = file.getName() - 'Bean.java'
                        String path = FileUtil.relativeTo(sourceDirFile, file)
                        jws(file: path, generateWsdl: 'true', type: 'JAXWS') {
                            wlhttptransport(serviceuri: serviceUri)
                        }
                    }
                }
            }
        }
    }

}

class WsWarPluginConvention {
    String sourceDir
    String webSourceDir
    FileCollection classpath
    FileCollection weblogicLibraries

    def statKartWeblogicWsWar(Closure closure) {
        closure.delegate = this
        closure()
    }
}
