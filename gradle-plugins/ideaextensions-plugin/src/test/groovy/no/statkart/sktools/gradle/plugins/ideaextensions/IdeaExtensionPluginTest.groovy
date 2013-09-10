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
     * @since 1.3
     */
    @Test
    void testAddInspectionProfileClean() {
        final def testCase = new InspectionProfileTestCase()
        testCase.ideaTemplate = testCase.IDEA_TEMPLATE_EMPTY_XML
        testCase.addInspectionProfileFile(testCase.INSPECTION_PROFILE_1_XML)
        testCase.addInspectionProfileFile(testCase.INSPECTION_PROFILE_2_XML)

        def rootNode = testCase.buildIdeaTemplateNode()

        IdeaExtensionsPlugin.addInspectionProfile(rootNode, testCase.extension)

        assert rootNode.component.findAll { it.@name == "InspectionProjectProfileManager" }.size() == 1 //forventet kun ett element

        Node managerNode = rootNode.component.findAll { it.@name == "InspectionProjectProfileManager" }[0]

        assert managerNode.profiles.profile.size() == 2; // forventer to profiler

    }

    /**
     * Tester angivelse xml for inspection profiles der profiler finnes ifra før (merge)
     * @since 1.3
     */
    @Test
    void testAddInspectionProfileMerge() {
        final def testCase = new InspectionProfileTestCase()
        testCase.ideaTemplate = IDEA_TEMPLATE_WITH_INSPECTIONS_XML
        testCase.addInspectionProfileFile(buildInspectionProfile(INSPECTION_PROFILE_1_NAME, 'invalidBooleanValue'))
        testCase.addInspectionProfileFile(INSPECTION_PROFILE_2_XML)

        def rootNode = testCase.buildIdeaTemplateNode()

        IdeaExtensionsPlugin.addInspectionProfile(rootNode, testCase.extension)

        assert rootNode.component.findAll { it.@name == "InspectionProjectProfileManager" }.size() == 1 //forventet kun ett element

        Node managerNode = rootNode.component.findAll { it.@name == "InspectionProjectProfileManager" }[0]

        assert managerNode.profiles.profile.size() == 2; // forventer to profiler

        assert managerNode.profiles.profile[0].option.find {it.@name == 'myName'}.@value == INSPECTION_PROFILE_1_NAME
        assert managerNode.profiles.profile[0].option.find {it.@name == 'myLocal'}.@value == 'invalidBooleanValue'

    }


    /**
     * Tester deklarering av gradle
     * @since 1.3
     */
    @Test
    void testAddGradleClean() {
        final def testCase = new GradleTestCase()
        testCase.ideaTemplate = testCase.IDEA_TEMPLATE_EMPTY_XML

        def rootNode = testCase.buildIdeaTemplateNode()

        assert rootNode.component.findAll { it.@name == "GradleSettings" }.size() == 0 //forventet ingen elementer

        IdeaExtensionsPlugin.addGradle(rootNode, testCase.extension)

        assert rootNode.component.findAll { it.@name == "GradleSettings" }.size() == 1 //forventet ett element

    }

    /**
     * Tester angivelse xml for inspection profiles der profiler finnes ifra før (merge)
     * @since 1.3
     */
    @Test
    void testGradleMerge() {
        final def testCase = new GradleTestCase()
        testCase.ideaTemplate = testCase.IDEA_TEMPLATE_WITH_GRADLE_XML

        def rootNode = testCase.buildIdeaTemplateNode()

        assert rootNode.component.findAll { it.@name == "GradleSettings" }.size() == 1 //forventet ett element
        assert rootNode.component.find { it.@name == "GradleSettings" }.option.find { it.@name == "gradleHome" }  //forventet at option finnes
        assert rootNode.component.find { it.@name == "GradleSettings" }.option.find { it.@name == "gradleHome" }.@value == testCase.GRADLE_SETTINGS_1_GRADLE_HOME

        IdeaExtensionsPlugin.addGradle(rootNode, testCase.extension)
        assert rootNode.component.findAll { it.@name == "GradleSettings" }.size() == 1 //forventet ett element
        assert rootNode.component.find { it.@name == "GradleSettings" }.option.find { it.@name == "gradleHome" }  //forventet at option finnes
        assert rootNode.component.find { it.@name == "GradleSettings" }.option.find { it.@name == "gradleHome" }.@value == testCase.project.gradle.gradleHomeDir


        IdeaExtensionsPlugin.addGradle(rootNode, testCase.extension)
        assert rootNode.component.findAll { it.@name == "GradleSettings" }.size() == 1 //forventet ett element

    }
}
