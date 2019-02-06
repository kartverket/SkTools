package no.statkart.sktools.gradle.plugins.wsimport

import no.statkart.sktools.gradle.testutils.ProjectHelper
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.Test

import static org.testng.Assert.assertNotNull
import static org.testng.Assert.assertTrue

public class WsImportPluginTest {
    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void applyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-wsimport-plugin'

        assertTrue project.plugins.hasPlugin('java')
        assertTrue project.plugins.hasPlugin('sktools-wsimport-plugin')
        assertNotNull project.configurations.findByName('jaxws')
        assertNotNull project.tasks.findByName('wsimport')

        project.apply plugin: 'idea'

        assertTrue project.idea.module.generatedSourceDirs.contains(new File(project.getBuildDir(), "wsimport"))
    }

    @Test
    void runTask() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'sktools-wsimport-plugin'

        def wsdlDir = project.mkdir('src/main/resources/META-INF/wsdls')

        ProjectHelper.copyFile(getClass().getResourceAsStream('/TestServiceWS.wsdl'), new File(wsdlDir, 'TestServiceWS.wsdl'))
        ProjectHelper.copyFile(getClass().getResourceAsStream('/TestServiceWS_schema1.xsd'), new File(wsdlDir, 'TestServiceWS_schema1.xsd'))
        ProjectHelper.copyFile(getClass().getResourceAsStream('/TestServiceWS_schema2.xsd'), new File(wsdlDir, 'TestServiceWS_schema2.xsd'))

        ProjectHelper projectHelper = new ProjectHelper(project)
        projectHelper.executeTask('wsimport')
    }
}
