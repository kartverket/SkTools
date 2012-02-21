package no.statkart.sktools.gradle.plugins.weblogic.wswar

import no.statkart.sktools.gradle.plugins.weblogic.wswar.util.FileUtil
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskAction

/**
 * todo: dokumentasjon
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class WeblogicWsWarTask  extends DefaultTask {
    WeblogicWsWarPluginConvention convention = getProject().convention.plugins.statKartWeblogicWsWar
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
