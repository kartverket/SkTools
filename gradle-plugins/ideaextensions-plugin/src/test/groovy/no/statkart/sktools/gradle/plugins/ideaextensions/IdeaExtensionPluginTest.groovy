package no.statkart.sktools.gradle.plugins.ideaextensions

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import org.testng.annotations.Test

import static no.statkart.sktools.gradle.plugins.ideaextensions.InspectionProfileTestCase.IDEA_TEMPLATE_WITH_INSPECTIONS_XML
import static no.statkart.sktools.gradle.plugins.ideaextensions.InspectionProfileTestCase.INSPECTION_PROFILE_2_XML
import static no.statkart.sktools.gradle.plugins.ideaextensions.InspectionProfileTestCase.buildInspectionProfile
import static no.statkart.sktools.gradle.plugins.ideaextensions.InspectionProfileTestCase.INSPECTION_PROFILE_1_NAME
import static no.statkart.sktools.gradle.plugins.ideaextensions.InspectionProfileTestCase.INSPECTION_PROFILE_1_LOCAL

/**
 * @author Leif Lislegård
 */
class IdeaExtensionPluginTest {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testAppplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-ideaextensions-plugin'


        assert project.ideaExtensions != null
        Assert.assertTrue(project.ideaExtensions instanceof IdeaExtensionsPluginExtension)

    }

    /**
     * Tester og demonstrerer angivelse av konfigurasjon
     */
    @Test(enabled = false)
    void testConventionConfiguration() {
        //todo
    }

    /**
     * Tester angivelse xml for inspection profiles
     */
    @Test
    void testAddInspectionProfileClean() {
        def testCase = new InspectionProfileTestCase()
        testCase.ideaTemplate = testCase.IDEA_TEMPLATE_EMPTY_XML
        testCase.addInspectionProfileFile(testCase.INSPECTION_PROFILE_1_XML)
        testCase.addInspectionProfileFile(testCase.INSPECTION_PROFILE_2_XML)

        def extension = testCase.getExtension()
        def rootNode = testCase.buildIdeaTemplateNode()

        IdeaExtensionsPlugin.addInspectionProfile(rootNode, extension)

        assert rootNode.component.findAll { it.@name == "InspectionProjectProfileManager" }.size() == 1 //forventet kun ett element

        Node managerNode = rootNode.component.findAll { it.@name == "InspectionProjectProfileManager" }[0]

        assert managerNode.profiles.profile.size() == 2; // forventer to profiler

    }

    /**
     * Tester angivelse xml for inspection profiles der profiler finnes ifra før (merge)
     */
    @Test
    void testAddInspectionProfileMerge() {
        def testCase = new InspectionProfileTestCase()
        testCase.ideaTemplate = IDEA_TEMPLATE_WITH_INSPECTIONS_XML
        testCase.addInspectionProfileFile(buildInspectionProfile(INSPECTION_PROFILE_1_NAME, 'invalidBooleanValue'))
        testCase.addInspectionProfileFile(INSPECTION_PROFILE_2_XML)

        def extension = testCase.getExtension()
        def rootNode = testCase.buildIdeaTemplateNode()

        IdeaExtensionsPlugin.addInspectionProfile(rootNode, extension)

        assert rootNode.component.findAll { it.@name == "InspectionProjectProfileManager" }.size() == 1 //forventet kun ett element

        Node managerNode = rootNode.component.findAll { it.@name == "InspectionProjectProfileManager" }[0]

        assert managerNode.profiles.profile.size() == 2; // forventer to profiler

        assert managerNode.profiles.profile[0].option.find {it.@name == 'myName'}.@value == INSPECTION_PROFILE_1_NAME
        assert managerNode.profiles.profile[0].option.find {it.@name == 'myLocal'}.@value == 'invalidBooleanValue'

    }


}
