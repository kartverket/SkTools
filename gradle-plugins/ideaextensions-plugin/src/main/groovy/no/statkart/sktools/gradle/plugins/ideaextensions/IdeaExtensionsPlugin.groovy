package no.statkart.sktools.gradle.plugins.ideaextensions

import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import no.statkart.sktools.gradle.plugins.ideaextensions.util.FileUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.plugins.ide.idea.IdeaPlugin

/**
 * todo: dokumentasjon. Hva gjøres med iws fila?
 *
 * todo: kan man evt legge til slik at default valg av VCS er 'Perforce' ?
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 */
class IdeaExtensionsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.getPlugins().apply(IdeaPlugin.class);
        IdeaExtensionsConvention convention = new IdeaExtensionsConvention()
        project.getConvention().getPlugins().put('ideaExtensions', convention)
        Task ideaTask = project.tasks.getByName('idea')
        ideaTask.doLast {
            if (project == project.getRootProject()) {
                modifyIwsFile(project, convention)
            }
        }
    }

    private def modifyIwsFile(Project project, IdeaExtensionsConvention convention) {
        File ideaProjectFile = project.file("${project.name}.iws")
        GPathResult xml = new XmlSlurper().parse(ideaProjectFile)
        addIgnoreMasksAndPaths(project, xml, convention)
        def writer = new OutputStreamWriter(new FileOutputStream(ideaProjectFile), 'UTF-8')
        XmlUtil.serialize(new StreamingMarkupBuilder().bind {
            mkp.yield xml
        }, writer)
        writer.close()
    }

    private def addIgnoreMasksAndPaths(Project project, GPathResult xml, IdeaExtensionsConvention convention) {
        xml.component.grep { it.@name == 'ChangeListManager' }.each {
            it.ignored.each { it.replaceNode {} }
            convention.masks.each { mask ->
                it.appendNode { ignored(mask: mask) }
            }
            project.getSubprojects().each { subproject ->
                String path = FileUtil.relativeTo(project.projectDir, subproject.file('build')).replaceAll('\\\\', '/') + '/'
                it.appendNode { ignored(path: path) }
            }
        }
    }
}
