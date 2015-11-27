package no.statkart.sktools.gradle.plugins.ideaextensions

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

/**
 * @since 1.3
 * @author Leif Lislegård
 */
class IdeaTestContext<T extends IdeaTestContext> {

    static String IDEA_IPR_EMPTY_XML = buildIdeaIprTemplate()

    final Project project
    final IdeaExtensionsPluginExtension extension
    protected String ideaIprTemplate


    IdeaTestContext() {
        project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-ideaextensions-plugin'

        extension = project.extensions.getByName(IdeaExtensionsPlugin.EXTENSION_NAME)
    }

    T setIdeaIprTemplate(String xml) {
        ideaIprTemplate = xml
        return (T) this
    }

    IdeaExtensionsPluginExtension getExtension() {
        return extension
    }



    static String buildIdeaIprTemplate(String... components) {
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
