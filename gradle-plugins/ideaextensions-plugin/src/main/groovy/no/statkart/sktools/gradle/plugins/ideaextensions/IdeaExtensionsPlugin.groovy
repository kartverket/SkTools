package no.statkart.sktools.gradle.plugins.ideaextensions

import groovy.util.slurpersupport.GPathResult

import no.statkart.sktools.gradle.plugins.ideaextensions.util.FileUtil
import org.gradle.api.Plugin
import org.gradle.api.Project

import org.gradle.plugins.ide.idea.IdeaPlugin

/**
 * Modifiserer iws og ipr filer for IntelliJ
 *
 * Legger til 'idea' plugin dersom  ikke allerede er aktivert.
 *
 * <ul>
 *   <li>Legger til masker for ignorerte filer for VCS / Version Control
 *   <li>Aktiverer Perforce som default.
 * </ul>
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class IdeaExtensionsPlugin implements Plugin<Project> {

    static final String CONVENTION_NAME = 'ideaExtensions'

    @Override
    void apply(Project project) {
        project.apply plugin: 'idea'

        IdeaExtensionsConvention convention = new IdeaExtensionsConvention(project)
        project.convention.plugins."${CONVENTION_NAME}" = convention

        project.tasks.idea.doLast {
            if (project == project.getRootProject()) {

                FileUtil.modifyXmlFile(project.file("${project.name}.iws")) { xml ->
                    addIgnoreMasksAndPaths(xml, convention)
                }

                FileUtil.modifyXmlFile(project.file("${project.name}.ipr")) { xml ->
                    addVcsMappings(xml, convention)
                }
            }
        }
    }

    /**
     * Legger til filter for ignorerte filer til VCS systemet
     */
    static def addIgnoreMasksAndPaths(GPathResult xml, IdeaExtensionsConvention convention) {
        Project project = convention.project

        xml.component.grep { it.@name == 'ChangeListManager' }.each {
            it.ignored.each { it.replaceNode {} }
            convention.masks.each { mask ->
                it.appendNode { ignored(mask: mask) }
            }

            //legger også til ignore for alle build-kataloger
            project.getSubprojects().each { subproject ->
                String path = FileUtil.relativeTo(project.projectDir, subproject.buildDir).replaceAll('\\\\', '/') + '/'
                it.appendNode { ignored(path: path) }
            }
        }
    }

    /**
     * Legger til VCS Directory Mappings (AKA aktiverering av perforce)
     * @since 1.1
     */
    static def addVcsMappings(GPathResult xml, IdeaExtensionsConvention convention) {
        xml.component.grep { it.@name == 'VcsDirectoryMappings' }.each {

            //dersom kun EN mapping
            if (it.children().size() == 1) {

                //dersom denne mappingen er tom..
                if (it.mapping.@directory == '' && it.mapping.@vcs == '') {

                    //sletter alle noder
                    it.mapping.each { it.replaceNode {} }

                    //legger inn nye noder
                    convention.vcsDirectoryMappings.each { path, vcs ->
                        path = path.equals('') ? path : '$PROJECT_DIR$/' + path
                        if ('Subversion'.equalsIgnoreCase(vcs)) {
                            vcs = 'svn'
                        }

                        it.appendNode { mapping(directory: path, vcs: vcs) }
                    }
                }
            }
        }
    }


}
