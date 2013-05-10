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
 * @author Tor Egil R. Strand
 */
class IdeaExtensionsPlugin implements Plugin<Project> {

    static final String EXTENSION_NAME = 'ideaExtensions'

    @Override
    void apply(Project project) {
        project.apply plugin: 'idea'

        IdeaExtensionsPluginExtension extension = project.extensions.create(EXTENSION_NAME, IdeaExtensionsPluginExtension.class)

        if (project.parent == null) { //root

            project.tasks.ideaWorkspace.doLast {
                FileUtil.modifyXmlFile(project.file(it.outputFile)) { xml ->
                    addIgnoreMasksAndPaths(xml, project, extension)
                }
            }

            project.idea.project.ipr.withXml { provider ->
                Node rootNode = provider.asNode()

                addGradle(rootNode, project)
                addVcsMappings(rootNode, extension)
                addIgnore(rootNode, extension)
            }

        } else { //ikke root
            if (extension.createAllSourceDirs) {
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


    }

    /**
     * Legger til filter for ignorerte filer til VCS systemet
     */
    static def addIgnoreMasksAndPaths(GPathResult xml, Project project, IdeaExtensionsPluginExtension convention) {
        xml.component.grep { it.@name == 'ChangeListManager' }.each {
            it.ignored.each { it.replaceNode {} }

            convention.ignoreMasks.each { mask ->
                it.appendNode { ignored(mask: mask) }
            }

            convention.ignorePaths.each { path ->
                String relPath = FileUtil.relativeTo(project.projectDir, path).replaceAll('\\\\', '/') + '/'
                it.appendNode { ignored(path: relPath) }
            }

            //legger også til ignore for alle build-kataloger
            project.getSubprojects().each { subproject ->
                String path = FileUtil.relativeTo(project.projectDir, subproject.buildDir).replaceAll('\\\\', '/') + '/'
                it.appendNode { ignored(path: path) }
            }
        }
    }

    /**
     * @since 1.2
     */
    static def addGradle(Node rootNode, Project project) {
        def builder = new NodeBuilder()

        def node = builder.component(name: 'GradleSettings') {
            option(name: 'gradleHome', value: project.gradle.gradleHomeDir)
        }

        rootNode.append(node)
    }

    /**
     * Legger til VCS Directory Mappings (AKA aktiverering av perforce)
     * @since 1.2
     */
    static def addVcsMappings(Node rootNode, IdeaExtensionsPluginExtension convention) {
        rootNode.component.grep { it.@name == 'VcsDirectoryMappings' }.each {

            //sletter alle noder
            it.mapping.each { it.replaceNode {} }

            //legger inn nye noder
            convention.vcsDirectoryMappings.each { path, vcs ->
                path = path.equals('') ? path : '$PROJECT_DIR$/' + path
                if ('Subversion'.equalsIgnoreCase(vcs)) {
                    vcs = 'svn'
                }

                def builder = new NodeBuilder()

                it.append(builder.mapping(directory: path, vcs: vcs) )
            }
        }
    }

    /**
     * @since 1.2
     */
    static def addIgnore(Node rootNode, IdeaExtensionsPluginExtension convention) {
        if (convention.inspectionsFile != null) {
            Node inspectionsXml = new XmlParser().parse(convention.inspectionsFile)
            def name = inspectionsXml.option.find{it.@name == 'myName'}.@value

            def builder = new NodeBuilder()
            def inpsectionComponent = builder.component(name: 'InspectionProjectProfileManager') {
                profiles {
                    profile(version: '1.0', is_locked: 'false') {
                        def p = currentNode
                        inspectionsXml.children().each { n -> p.append(n) }
                    }
                }
                option(name: 'PROJECT_PROFILE', value: name)
//                option(name: 'USE_PROJECT_PROFILE', value: 'true')
//                version(value: '1.0')
            }
            rootNode.append(inpsectionComponent)
        }
    }
}
