package no.statkart.sktools.gradle.plugins.weblogic.wsclient

import java.util.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * todo: dokumentasjon
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class WsClientTask extends DefaultTask {

    /** todo: kan antakeligvis bruke {@link org.gradle.api.internal.AbstractTask#getDynamicObjectHelper()}.setConvention(Convention) **/
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