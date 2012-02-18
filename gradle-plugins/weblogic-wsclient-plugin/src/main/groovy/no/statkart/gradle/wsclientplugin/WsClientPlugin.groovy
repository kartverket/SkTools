package no.statkart.gradle.wsclientplugin

import no.statkart.gradle.util.GradleUtil
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.tasks.TaskAction
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import java.util.zip.ZipFile
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Directory

class WsClientPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def convention = new WsClientPluginConvention(project)
        project.getConvention().getPlugins().put('wsClient', convention)
        project.getPlugins().apply(JavaPlugin.class);
        Task wsClientTask = project.task('wsClient', type: WsClientTask)
        Task compileTask = project.getTasks().getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME)
        compileTask.dependsOn(wsClientTask)
        Configuration moduleConfiguration = project.configurations.add('basewar').setVisible(false)
                .setTransitive(false).setDescription("Classpath for wars to base wsclient on.");
        wsClientTask.dependsOn(moduleConfiguration)

        GradleUtil.makeIdeaShowBuildDirectory(project)
        Task wsClientCreateJavaDir = project.task('wsClientCreateJavaDir', type: Directory) { dir = convention.wsTargetDir }
        Task wsClientCreateResourceDir = project.task('wsClientCreateResourceDir', type: Directory) { dir = convention.wsResourcesDir }
        Task ideaTask = project.getTasks().getByName('ideaModule')
        ideaTask.dependsOn([wsClientCreateJavaDir, wsClientCreateResourceDir])
    }

}

class WsClientTask extends DefaultTask {
    WsClientPluginConvention convention = getProject().convention.plugins.wsClient

    WsClientTask() {
        outputs.dir(convention.wsTargetDir)
        outputs.dir(convention.wsResourcesDir)
        if (!project.sourceSets.main.java.srcDirs.contains(convention.wsTargetDir)) {
            project.sourceSets.main.java.srcDirs += convention.wsTargetDir
        }
        if (!project.sourceSets.main.resources.srcDirs.contains(convention.wsResourcesDir)) {
            project.sourceSets.main.resources.srcDirs += convention.wsResourcesDir
        }
    }

    @TaskAction
    def wsClient() {
        ant.taskdef(name: 'clientgen', classname: 'weblogic.wsee.tools.anttasks.ClientGenTask', classpath: convention.weblogicLibraries.asPath)
        ant.delete(dir: convention.wsTargetDir)
        convention.wsTargetDir.mkdirs()
        project.configurations.basewar.findAll { it.getName().endsWith('.war') }.each { File file ->
            new ZipFile(file).entries().findAll { it.getName().endsWith('.wsdl') }.each { entry ->
                ant.clientgen(wsdl: "jar:file:${file.getPath()}!/${entry.getName()}", destdir: convention.wsTargetDir, includeGlobalTypes: 'true', copywsdl: 'true', type: 'JAXWS')
            }
        }
        ant.delete(dir: convention.wsTargetDir) {include(name: '**/*.class')}
        ant.copy(todir: convention.wsResourcesDir) {
            fileset(dir: convention.wsTargetDir, includes: '**/*.wsdl,**/*.xsd')
        }
        convention.fixExceptionsFor.each { props ->
            logger.info("Fixing exceptions for module $props.name")
            fixExceptions(convention.wsTargetDir, props.exceptionPackageImport, props.exceptionPackage, props.exceptionFilePattern, props.regexpMatch, props.regexpReplace)
        }
        fixWSUrl(ant, convention.wsTargetDir)
    }

    void fixExceptions(File genSourceDir, String exceptionPackageImport, String exceptionPackage, String exceptionFilePattern, String regexpMatch, String regexpReplace) {
        def exceptionPackageDir = new File(genSourceDir, exceptionPackage)
        exceptionPackageDir.mkdirs()
        ant.move(todir: exceptionPackageDir, overwrite: true, flatten: true, verbose: true) {
            fileset(dir: genSourceDir) {
                include(name: exceptionFilePattern)
            }
        }
        ant.replaceregexp(match: regexpMatch, replace: regexpReplace) {
            fileset(dir: exceptionPackageDir) {
                include(name: '*Exception.java')
            }
        }
        ant.replaceregexp {
            regexp(pattern: 'package (.+);')
            substitution(expression: ('package \\1;${line.separator}${line.separator}' + exceptionPackageImport))
            fileset(dir: new File(exceptionPackageDir, '../service'), erroronmissingdir: true) {
                include(name: '**/*Service*.java')
            }
        }
    }

    static void fixWSUrl(ant, File dir) {
        // Unshitify Oracle - fault wsdl-urls are created and we're fixing it
        ant.replaceregexp {
            regexp(pattern: /URL baseUrl;[^=]+\s(.*getResource).*;[^=]*.*baseUrl, "(.*)".*;([^{]*)MalformedURL/)
            substitution(expression: ('url \\1("/\\2");\\3'))
            fileset(dir: dir, erroronmissingdir: true) {
                include(name: '**/*WS*.java')
            }
        }
    }

}

class WsClientPluginConvention {
    FileCollection weblogicLibraries
    List<Map<String, String>> fixExceptionsFor;

    File wsTargetDir
    File wsResourcesDir

    WsClientPluginConvention(Project project) {
        wsTargetDir = project.file('build/generated/main/java')
        wsResourcesDir = project.file('build/generated/main/resources')
    }

    def wsClient(Closure closure) {
        closure.delegate = this
        closure()
    }
}
