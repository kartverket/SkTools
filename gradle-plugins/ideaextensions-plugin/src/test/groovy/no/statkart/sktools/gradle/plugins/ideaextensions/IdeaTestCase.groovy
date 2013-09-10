package no.statkart.sktools.gradle.plugins.ideaextensions

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

/**
 * @since 1.3
 * @author Leif Lislegård
 */
class IdeaTestCase<T extends IdeaTestCase> {

    static String IDEA_TEMPLATE_EMPTY_XML = buildIdeaTemplate()

    final Project project
    final IdeaExtensionsPluginExtension extension
    protected String ideaTemplate


    IdeaTestCase() {
        project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-ideaextensions-plugin'

        extension = project.extensions.getByName(IdeaExtensionsPlugin.EXTENSION_NAME)
    }

    T setIdeaTemplate(String xml) {
        ideaTemplate = xml
        return (T) this
    }

    IdeaExtensionsPluginExtension getExtension() {
        return extension
    }

    Node buildIdeaTemplateNode() {
        new XmlParser().parseText(ideaTemplate)
    }


    static String buildIdeaTemplate(String... components) {
        """
<project version="4">
  <component name="CompilerConfiguration">
    <option name="DEFAULT_COMPILER" value="Javac" />
  </component>
  <component name="CopyrightManager" default="">
    <module2copyright />
  </component>
  ${components.join('\n\n')}
</project>
"""
    }

}
