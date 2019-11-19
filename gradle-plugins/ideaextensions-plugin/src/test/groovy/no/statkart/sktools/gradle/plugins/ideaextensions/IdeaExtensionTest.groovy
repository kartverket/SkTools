package no.statkart.sktools.gradle.plugins.ideaextensions

import org.testng.Assert
import org.testng.annotations.Test

import static no.statkart.sktools.gradle.plugins.ideaextensions.InspectionProfileTestContext.buildInspectionProfile

/**
 * Unit-test
 */
class IdeaExtensionTest {

    /**
     * Tester angivelse av ignore paths
     */
    @Test
    void testAddIgnorePaths() {
        final def testCase = new InspectionProfileTestContext()
        testCase.templateXml = testCase.IDEA_IPR_EMPTY_XML
        testCase.addInspectionProfileFile(testCase.INSPECTION_PROFILE_1_XML)
        testCase.addInspectionProfileFile(testCase.INSPECTION_PROFILE_2_XML)

        def rootNode = new XmlParser().parseText(testCase.templateXml)

        IdeaExtensionsPlugin.addInspectionProfile(rootNode, testCase.extension)
        assert rootNode.component.findAll { it.@name == "InspectionProjectProfileManager" }.size() == 1 //forventet kun ett element

        Node managerNode = rootNode.component.findAll { it.@name == "InspectionProjectProfileManager" }[0]
        Assert.assertEquals(managerNode.profiles.profile.size(), 2, "forventer to profiler")
    }

    /**
     * Tester angivelse xml for inspection profiles
     * @since 1.3
     */
    @Test
    void testAddInspectionProfileCleanIdea12() {
        final def testCase = new IdeaTestContext()
        testCase.templateXml = testCase.IDEA_IWS_EMPTY_XML

        def rootNode = new XmlParser().parseText(testCase.templateXml)
        testCase.extension.ignorePaths = ["dir1", "sub/dir2"]

        IdeaExtensionsPlugin.addIgnoreMasksAndPaths(rootNode, testCase.extension)
        assert rootNode.component.findAll { it.@name == "ChangeListManager" }.size() == 1 //forventet kun ett element

        Node changeListManager = rootNode.component.findAll { it.@name == "ChangeListManager" }[0]
        def ignoredPathNodes = changeListManager.ignored.findAll { it.@path != null }
        Assert.assertEquals(ignoredPathNodes.size(), 2, "ignore paths")
        Assert.assertEquals(ignoredPathNodes[0].@path, "dir1/")
        Assert.assertEquals(ignoredPathNodes[1].@path, "sub/dir2/")
    }

    /**
     * SKTOOLS-142: Tester angivelse xml for inspection profiles
     * @since 2.0
     */
    @Test
    void testAddInspectionProfileCleanIdea14() {
        final def testCase = new InspectionProfileTestContext()
        testCase.templateXml = testCase.IDEA_IPR_EMPTY_XML
        testCase.addInspectionProfileFile(testCase.INSPECTION_PROFILE_1_XML)
        testCase.addInspectionProfileFile(testCase.INSPECTION_PROFILE_2_XML)

        def rootNode = new XmlParser().parseText(testCase.templateXml)

        IdeaExtensionsPlugin.addInspectionProfile(rootNode, testCase.extension)
        assert rootNode.component.findAll { it.@name == "InspectionProjectProfileManager" }.size() == 1 //forventet kun ett element

        Node managerNode = rootNode.component.findAll { it.@name == "InspectionProjectProfileManager" }[0]
        Assert.assertEquals(managerNode.profile.size(), 2, "forventer to profiler")
    }

    /**
     * Tester angivelse xml for inspection profiles der profiler finnes ifra før (merge)
     * @since 1.3
     */
    @Test
    void testAddInspectionProfileMerge() {
        final def testContext = new InspectionProfileTestContext()
        testContext.templateXml = testContext.IDEA_TEMPLATE_WITH_INSPECTIONS_XML
        testContext.addInspectionProfileFile(buildInspectionProfile(testContext.INSPECTION_PROFILE_1_NAME, 'invalidBooleanValue'))
        testContext.addInspectionProfileFile(testContext.INSPECTION_PROFILE_2_XML)

        def rootNode = new XmlParser().parseText(testContext.templateXml)

        IdeaExtensionsPlugin.addInspectionProfile(rootNode, testContext.extension)
        assert rootNode.component.findAll { it.@name == "InspectionProjectProfileManager" }.size() == 1 //forventet kun ett element

        Node managerNode = rootNode.component.findAll { it.@name == "InspectionProjectProfileManager" }[0]
        Assert.assertEquals(managerNode.profiles.profile.size(), 2, "forventer to profiler")
        Assert.assertEquals(managerNode.profiles.profile[0].option.find {it.@name == 'myName'}.@value, testContext.INSPECTION_PROFILE_1_NAME)
        Assert.assertEquals(managerNode.profiles.profile[0].option.find {it.@name == 'myLocal'}.@value, 'invalidBooleanValue')
    }


    /**
     * Tester deklarering av gradle
     * @since 1.3
     */
    @Test
    void testAddGradleClean() {
        final def testContext = new GradleTestContext()
        testContext.templateXml = testContext.IDEA_IPR_EMPTY_XML

        def rootNode = new XmlParser().parseText(testContext.templateXml)
        Assert.assertEquals(rootNode.component.findAll { it.@name == "GradleSettings" }.size(), 0, "forventet ingen elementer")

        IdeaExtensionsPlugin.addGradle(rootNode, testContext.extension)
        Assert.assertEquals(rootNode.component.findAll { it.@name == "GradleSettings" }.size(), 1, "forventet ett element")
    }

    /**
     * Tester angivelse xml for inspection profiles der profiler finnes ifra før (merge)
     * @since 1.3
     */
    @Test
    void testGradleMerge() {
        final def testContext = new GradleTestContext()
        testContext.templateXml = testContext.IDEA_TEMPLATE_WITH_GRADLE_XML

        def rootNode = new XmlParser().parseText(testContext.templateXml)

        Assert.assertEquals(rootNode.component.findAll { it.@name == "GradleSettings" }.size(), 1)
        Assert.assertNotNull(rootNode.component.find { it.@name == "GradleSettings" }.option.find { it.@name == "gradleHome" }, "forventet at option finnes")
        Assert.assertEquals(rootNode.component.find { it.@name == "GradleSettings" }.option.find { it.@name == "gradleHome" }.@value, testContext.GRADLE_SETTINGS_1_GRADLE_HOME)

        IdeaExtensionsPlugin.addGradle(rootNode, testContext.extension)
        Assert.assertEquals(rootNode.component.findAll { it.@name == "GradleSettings" }.size(), 1)
        Assert.assertNotNull(rootNode.component.find { it.@name == "GradleSettings" }.option.find { it.@name == "gradleHome" }, "forventet at option finnes")
        Assert.assertEquals(rootNode.component.find { it.@name == "GradleSettings" }.option.find { it.@name == "gradleHome" }.@value, testContext.project.gradle.gradleHomeDir)

        IdeaExtensionsPlugin.addGradle(rootNode, testContext.extension)
        Assert.assertEquals(rootNode.component.findAll { it.@name == "GradleSettings" }.size(), 1, "ikke flere elementer av denne typen")
    }

}
