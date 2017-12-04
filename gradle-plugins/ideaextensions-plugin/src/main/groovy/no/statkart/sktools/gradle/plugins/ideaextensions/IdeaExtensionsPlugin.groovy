package no.statkart.sktools.gradle.plugins.ideaextensions

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

        IdeaExtensionsPluginExtension extension = project.extensions.create(EXTENSION_NAME, IdeaExtensionsPluginExtension.class, project)

        if (project.parent == null) { //root
            project.idea.project.ipr.withXml { provider ->
                final Node rootNode = provider.asNode()

                addGradle(rootNode, extension)
                addVcsMappings(rootNode, extension)
                addInspectionProfile(rootNode, extension)
                addCodeStyle(rootNode, extension)
            }

            project.idea.workspace.iws.withXml { provider ->
                final Node rootNode = provider.asNode()

                addIgnoreMasksAndPaths(rootNode, extension)
            }

        } else { //ikke root
            if (extension.createAllSourceDirs) {
                project.tasks.ideaModule.doFirst {
                    //SKIF-178: oppretter kataloger for alle sourceSet
                    it.project.getConvention().getPlugin(JavaPluginConvention.class).sourceSets.each {
                        it.getAllSource().srcDirs.each {
                            if (!it.exists()) {
                                project.logger.quiet("..creating folder {}", project.relativePath(it));
                                project.mkdir(it)
                            }
                        }
                        //oppretter også mapper for generert kode (introdusert i SKIF-173)
                        it.getOutput().getDirs().each {
                            if (!it.exists()) {
                                project.logger.quiet("..creating folder {} (output)", project.relativePath(it));
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
    static def addIgnoreMasksAndPaths(Node rootNode, IdeaExtensionsPluginExtension convention) {
        rootNode.component.grep { it.@name == 'ChangeListManager' }.each { Node component ->
            component.ignored.each {
                component.remove(it) //tar bort alle tidligere ignores
            }

            convention.ignoreMasks.each { mask ->
                Node node = NodeBuilder.newInstance().ignored(mask: mask)
                convention.project.logger.info("Adding node '{}'", node);
                component.append(node)
            }

            convention.ignorePaths.each { path ->
                String relPath = convention.project.relativePath(path).replaceAll('\\\\', '/') + '/'
                Node node = NodeBuilder.newInstance().ignored(path: relPath)
                convention.project.logger.info("Adding node '{}'", node);
                component.append(node)
            }

            //legger også til ignore for alle build-kataloger
            convention.project.getSubprojects().each { subproject ->
                String relPath = convention.project.relativePath(subproject.buildDir).replaceAll('\\\\', '/') + '/'
                Node node = NodeBuilder.newInstance().ignored(path: relPath)
                convention.project.logger.info("Adding node '{}'", node);
                component.append(node)
            }
        }
    }

    /**
     * @since 1.2
     */
    static def addGradle(Node rootNode, IdeaExtensionsPluginExtension convention) {
        Node component = rootNode.component.find { it.@name == 'GradleSettings' }
        if (component == null) {
            component = new NodeBuilder().component(name: 'GradleSettings') {
                option(name: 'gradleHome', value: 'replace me!')
            }
            rootNode.append(component)
        }

        component.option.find { it.@name == 'gradleHome' }.replaceNode {
            option(name: 'gradleHome', value: convention.project.gradle.gradleHomeDir) {}
        }

    }

    /**
     * Legger til VCS Directory Mappings (AKA aktiverering av perforce)
     * @since 1.2
     */
    static def addVcsMappings(Node rootNode, IdeaExtensionsPluginExtension convention) {
        rootNode.component.grep { it.@name == 'VcsDirectoryMappings' }.each { Node  component ->

            //sletter alle noder
            component.mapping.each { component.remove(it) }

            //legger inn nye noder
            convention.vcsDirectoryMappings.each { path, vcs ->
                path = path.equals('') ? path : '$PROJECT_DIR$/' + path
                if ('Subversion'.equalsIgnoreCase(vcs)) {
                    vcs = 'svn'
                }

                component.append(new NodeBuilder().mapping(directory: path, vcs: vcs))
            }
        }
    }

    /**
     * Legger til code style dersom angitt
     * @since 1.3
     */
    static def addCodeStyle(Node rootNode, IdeaExtensionsPluginExtension convention) {
        for (def path : convention.codeStyles) {
            final File codeStyleFile = convention.project.file(path)
            final Node codeStyle = new XmlParser().parse(codeStyleFile)

            //sletter evt gamle noder
            rootNode.component.findAll { it.@name == codeStyle.@name }.each { Node component ->
                rootNode.remove(component)
            }
            rootNode.append(codeStyle)
        }
    }

    /**
     * @since 1.2
     */
    static def addInspectionProfile(Node rootNode, IdeaExtensionsPluginExtension convention) {
        def defaultProfileName = null;
        for (def path : convention.inspectionProfiles) {
            def inspectionProfileFile = convention.project.file(path)

            if (inspectionProfileFile != null) {
                Node profileNode = buildInspectionProfile(inspectionProfileFile)
                final String profileName = profileNode.option.find {it.@name == 'myName'}.@value

                if (!defaultProfileName) {
                    defaultProfileName = profileName
                }

                Node component = rootNode.component.find { it.@name == 'InspectionProjectProfileManager'}
                if (component == null) {
                    component = buildInspectionProfileManager(defaultProfileName)
                    rootNode.append(component)
                } else {
                    //sletter evt duplikater som er kommet inn ved feil rettet i SKTOOLS-82
                    rootNode.component.findAll { it.@name == 'InspectionProjectProfileManager' }.each {
                        if (it != component) {
                            rootNode.remove(component)
                        }
                    }
                }

                //idea > 14
                Node profiles;
                if (!component.profiles) { //blir strippet vekk av nyere versjoner av IntelliJ.
                    profiles = new NodeBuilder().profiles {};
                    component.append(profiles)
                } else {
                    profiles = component.profiles.first()
                    removeProfileWithName(profileName, profiles.profile)
                }
                profiles.append(profileNode)

                //idea 14+
                removeProfileWithName(profileName, component.profile)
                component.append(profileNode)
            }
        }
    }

    /**
     * sletter evt gammelt duplikat med samme navn
     */
    private static Object removeProfileWithName(profileName, Iterable<Node> profileNodes) {
        profileNodes.each {
            def name = it.option.find { it.@name = 'myName' }.@value
            if (profileName.equals(name)) {
                it.parent().remove(it)
            }
        }
    }

    /** @since 1.3 */
    private static Node buildInspectionProfileManager(String defaultProfileName) {
        def builder = new NodeBuilder()
        builder.component(name: 'InspectionProjectProfileManager') {
            option(name: 'PROJECT_PROFILE', value: defaultProfileName)
            option(name: 'USE_PROJECT_PROFILE', value: 'true')
            version(value: '1.0')
        }
    }

    /** @since 1.3 */
    private static Node buildInspectionProfile(File inspectionProfileFile) {
        Node profileNode = new XmlParser().parse(inspectionProfileFile)

        //rename profile node for backward compatibility
        if (profileNode.name().equals('inspections')) {
            Node clone = new Node(null, 'profile', profileNode.attributes())
            for (Node child : profileNode.children()) {
                clone.append(child)
            }
            profileNode = clone
        }

        return profileNode
    }

}
