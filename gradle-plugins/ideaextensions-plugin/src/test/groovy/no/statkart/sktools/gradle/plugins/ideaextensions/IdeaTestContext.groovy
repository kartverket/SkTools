package no.statkart.sktools.gradle.plugins.ideaextensions

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

/**
 * @since 1.3
 * @author Leif Lislegård
 */
class IdeaTestContext<T extends IdeaTestContext> {

    static final String IDEA_IPR_EMPTY_XML = buildIdeaIprTemplate()
    static final String IDEA_IWS_EMPTY_XML = buildIdeaIwsTemplate()

    final Project project
    final IdeaExtensionsPluginExtension extension
    protected String templateXml


    IdeaTestContext() {
        project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-ideaextensions-plugin'

        extension = project.extensions.getByName(IdeaExtensionsPlugin.EXTENSION_NAME)
    }

    T setTemplateXml(String xml) {
        templateXml = xml
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


    static String buildIdeaIwsTemplate(String... components) {
        """
<project version="4">
  <component name="ChangeListManager">
    <ignored path=".idea/workspace.xml" />
  </component>
  <component name="RunManager" selected="">
  </component>
  ${components.join('\n\n')}
</project>
"""
    }


}
