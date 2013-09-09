package no.statkart.sktools.gradle.plugins.ideaextensions

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

/**
 * @since 1.3
 * @author Leif Lislegård
 */
class InspectionProfileTestCase {

    final IdeaExtensionsPluginExtension extension
    protected String ideaTemplate


    InspectionProfileTestCase() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-ideaextensions-plugin'

        extension = project.extensions.getByName(IdeaExtensionsPlugin.EXTENSION_NAME)
    }

    InspectionProfileTestCase addInspectionProfileFile(String xml) {
        File file = File.createTempFile('inspection_profile', '.xml')
        file.deleteOnExit()
        file.withPrintWriter("UTF-8") {
            it.println xml
        }
        extension.inspectionProfiles += file
        return this
    }

    InspectionProfileTestCase setIdeaTemplate(String xml) {
        ideaTemplate = xml
        return this
    }

    IdeaExtensionsPluginExtension getExtension() {
        return extension
    }

    Node buildIdeaTemplateNode() {
        new XmlParser().parseText(ideaTemplate)
    }

    protected static String buildInspectionProfile(def name, def local) {
        """
          <profile version="1.0" is_locked="false">
            <option name="myName" value="${name}" />
            <option name="myLocal" value="${local}" />
          </profile>
        """.trim()
    }

    static String INSPECTION_PROFILE_1_NAME = 'Test profile 1'
    static String INSPECTION_PROFILE_1_LOCAL = 'false'
    static String INSPECTION_PROFILE_1_XML = buildInspectionProfile(INSPECTION_PROFILE_1_NAME, INSPECTION_PROFILE_1_LOCAL)
    static String INSPECTION_PROFILE_2_XML = buildInspectionProfile('Test profile 2', 'false')


    static String IDEA_TEMPLATE_EMPTY_XML = """
<project version="4">
  <component name="CompilerConfiguration">
    <option name="DEFAULT_COMPILER" value="Javac" />
  </component>
  <component name="CopyrightManager" default="">
    <module2copyright />
  </component>
</project>
"""

    static String IDEA_TEMPLATE_WITH_INSPECTIONS_XML = """
<project version="4">
  <component name="CompilerConfiguration">
    <option name="DEFAULT_COMPILER" value="Javac" />
  </component>
  <component name="InspectionProjectProfileManager">
    <profiles>
    ${INSPECTION_PROFILE_1_XML}
    </profiles>
    <option name="PROJECT_PROFILE" value="Test profile 1" />
    <option name="USE_PROJECT_PROFILE" value="true" />
    <version value="1.0" />
  </component>
  <component name="CopyrightManager" default="">
    <module2copyright />
  </component>
</project>
"""



}
