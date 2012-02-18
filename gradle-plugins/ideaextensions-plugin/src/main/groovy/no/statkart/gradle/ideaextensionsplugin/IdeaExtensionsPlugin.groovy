package no.statkart.gradle.ideaextensionsplugin

import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import no.statkart.gradle.util.FileUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.plugins.ide.idea.IdeaPlugin

/**
 * @author Thor Åge Eldby
 */
class IdeaExtensionsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.getPlugins().apply(IdeaPlugin.class);
        IdeaExtensionsConvention convention = new IdeaExtensionsConvention()
        project.getConvention().getPlugins().put('statKartIdeaExtensions', convention)
        Task ideaTask = project.tasks.getByName('idea')
        ideaTask.doLast {
            if (project == project.getRootProject()) {
                modifyIwsFile(project, convention.masks)
            }
        }
    }

    private def modifyIwsFile(Project project, Collection<String> masks) {
        File ideaProjectFile = project.file("${project.name}.iws")
        GPathResult xml = new XmlSlurper().parse(ideaProjectFile)
        addIgnoreMasksAndPaths(project, xml, masks)
        def writer = new OutputStreamWriter(new FileOutputStream(ideaProjectFile), 'UTF-8')
        XmlUtil.serialize(new StreamingMarkupBuilder().bind {
            mkp.yield xml
        }, writer)
        writer.close()
    }

    private def addIgnoreMasksAndPaths(Project project, GPathResult xml, Collection<String> masks) {
        xml.component.grep { it.@name == 'ChangeListManager' }.each {
            it.ignored.each { it.replaceNode {} }
            masks.each { mask ->
                it.appendNode { ignored(mask: mask) }
            }
            project.getSubprojects().each { subproject ->
                String path = FileUtil.relativeTo(project.projectDir, subproject.file('build')).replaceAll('\\\\', '/') + '/'
                it.appendNode { ignored(path: path) }
            }
        }
    }

}

class IdeaExtensionsConvention {
    Collection<String> masks = ['*.iws', '*.ipr', '*.iml', '*.log']

    def statKartIdeaExtensions(Closure closure) {
        closure.delegate = this
        closure()
    }
}
