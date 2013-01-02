package no.statkart.sktools.gradle.plugins.ideaextensions

import groovy.util.slurpersupport.GPathResult

import no.statkart.sktools.gradle.plugins.ideaextensions.util.FileUtil
import org.gradle.api.Plugin
import org.gradle.api.Project

import org.gradle.api.plugins.JavaPluginConvention

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
 * Dersom plugin er aktiv for modul/subprosjekt så genereres det opp tomme mapper for alle sourceSets. [SKIF-178]
 *  - Dette gjør da at {@link org.gradle.api.tasks.SourceSet main og test sourceSet} får tagget alle source katalogene sine i IntelliJ prosjektet, selv om de er tomme.
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

        if (project.parent == null) { //root

            project.tasks.ideaWorkspace.doLast {
                FileUtil.modifyXmlFile(project.file(it.outputFile)) { xml ->
                    addIgnoreMasksAndPaths(xml, convention)
                }
            }

            project.tasks.ideaProject.doLast {
                FileUtil.modifyXmlFile(it.outputFile) { xml ->
                    addVcsMappings(xml, convention)
                }
            }

        } else { //ikke root
            project.tasks.ideaModule.doFirst {
                //SKIF-178: oppretter kataloger for alle sourceSet
                it.project.getConvention().getPlugin(JavaPluginConvention.class).sourceSets.each {
                    it.getAllSource().srcDirs.each {
                        if (!it.exists()) {
                            println "..creating folder ${project.relativePath(it)}"
                            project.mkdir(it)
                        }
                    }
                    //oppretter også mapper for generert kode (introdusert i SKIF-173)
                    it.getOutput().getDirs().each {
                        if (!it.exists()) {
                            println "..creating folder ${project.relativePath(it)} (output)"
                            project.mkdir(it)
                        }
                    }
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
