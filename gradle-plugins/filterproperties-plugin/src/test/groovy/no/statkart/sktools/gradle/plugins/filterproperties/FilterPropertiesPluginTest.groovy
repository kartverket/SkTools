package no.statkart.sktools.gradle.plugins.filterproperties

import org.testng.annotations.Test
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert

import no.statkart.sktools.gradle.testutils.builder.FilterPropertiesProjectBuilder
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.filewriter.FilterPropertiesTestutilFilewriter
import org.gradle.api.plugins.BasePlugin

/**
 * @author Leif Lislegård
 */
class FilterPropertiesPluginTest {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testAppplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-filterproperties-plugin'

        assert project.convention.plugins.filterProperties != null
        Assert.assertTrue(project.convention.plugins.filterProperties instanceof FilterPropertiesConvention)

    }

    /**
     * Tester og demonstrerer angivelse av konfigurasjon
     */
    @Test
    void testConventionConfiguration() {
        ProjectHelper projectHelper = FilterPropertiesProjectBuilder.builder().applyJavaPlugin().applyFilterPropertiesPlugin().build()
        FilterPropertiesConvention convention = (FilterPropertiesConvention) projectHelper.project.convention.plugins.get(FilterPropertiesPlugin.CONVENTION_NAME)

        projectHelper.configureProject {
            filteredProperties {
                properties = [singleProperty:'singleValue']
            }
        }

        assert convention.properties == ['singleProperty':'singleValue']

        projectHelper.setProjectProperties(['projectProperty':'projectValue'])


        projectHelper.configureProject {
            filteredProperties {
                properties = projectProperties()
                properties 'singleProperty': 'singleValue'
            }
        }

        assert convention.properties.containsKey('singleProperty')
        assert convention.properties.containsKey('projectProperty')

    }


    /**
     * Tester bruk med standard verdier.
     */
    @Test
    void testDefaultValues() {
        ProjectHelper projectHelper = FilterPropertiesProjectBuilder.builder().applyJavaPlugin().applyFilterPropertiesPlugin().withName('PropertiesFilterTest').build()

        use(FilterPropertiesTestutilFilewriter) {
            projectHelper.writeTwoSimpleResources('src/main/unfilteredResources')
        }

        projectHelper.setProjectProperties([myProperty2:'testValue', myEmail:'unittest'])
        assert projectHelper.project.getName() == "PropertiesFilterTest"

        projectHelper.initializeProject()



        projectHelper.executeTask(FilterPropertiesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)
        projectHelper.assertTaskExecutedNotSkipped(FilterPropertiesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)

        projectHelper.assertFileExists("gen/src/main/resources/simpleResource1.txt") { File file ->
            assert file.text.contains("name=PropertiesFilterTest")
            assert file.text.contains("version=unspecified")
            assert file.text.contains("myProperty1=@myProperty1@")
        }

        projectHelper.assertFileExists("gen/src/main/resources/simpleResource2.txt") { File file ->
            assert file.text.contains("myProperty1=@myProperty1@")
            assert file.text.contains("myProperty2=testValue")
            assert file.text.contains("myEmail=unittest@statkart.no")
        }

    }

    /**
     * Tester bruk med egne properties.
     */
    @Test
    void testCustomProperties() {
        ProjectHelper projectHelper = FilterPropertiesProjectBuilder.builder().applyJavaPlugin().applyFilterPropertiesPlugin().withName('PropertiesFilterTest').build()

        use(FilterPropertiesTestutilFilewriter) {
            projectHelper.writeTwoSimpleResources('src/main/unfilteredResources')
        }

        projectHelper.setProjectProperties([myProperty1:'testValue'])

        projectHelper.configureProject {
            filteredProperties {
                properties projectProperties()
                properties myProperty1: 'overidenValue',
                        'name': 'overidenName'

            }
        }

        projectHelper.initializeProject()



        projectHelper.executeTask(FilterPropertiesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)
        projectHelper.assertTaskExecutedNotSkipped(FilterPropertiesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)

        projectHelper.assertFileExists("gen/src/main/resources/simpleResource1.txt") { File file ->
            assert file.text.contains("name=overidenName")
            assert file.text.contains("myProperty1=overidenValue")
        }

    }


    /**
     * Tester 'processResources' task blir satt opp riktig.
     */
    @Test
    void testResources() {
        ProjectHelper projectHelper = FilterPropertiesProjectBuilder.builder().applyJavaPlugin().applyFilterPropertiesPlugin().withName('PropertiesFilterTest').build()

        use(FilterPropertiesTestutilFilewriter) {
            projectHelper.writeTwoSimpleResources('src/main/unfilteredResources')
        }

        projectHelper.setProjectProperties([myProperty1:'testValue'])
        projectHelper.initializeProject()

        projectHelper.executeTask("processResources")
        projectHelper.assertTaskExecutedNotSkipped("processResources")
        projectHelper.assertTaskExecutedNotSkipped(FilterPropertiesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)

        projectHelper.assertFileExists("build/resources/main/simpleResource1.txt") { File file ->
            assert file.text.contains("myProperty1=testValue")
        }

    }

    /**
     * Tester at genererte filer blir cleanet ved {@code clean}
     */
    @Test
    void testClean() {
        ProjectHelper projectHelper = FilterPropertiesProjectBuilder.builder().applyJavaPlugin().applyFilterPropertiesPlugin().withName('PropertiesFilterTest').build()

        use(FilterPropertiesTestutilFilewriter) {
            projectHelper.writeTwoSimpleResources('src/main/unfilteredResources')
        }

        projectHelper.initializeProject()

        projectHelper.executeTask(FilterPropertiesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)
        projectHelper.assertTaskExecutedNotSkipped(FilterPropertiesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)

        projectHelper.assertFileExists("gen/src/main/resources")
        projectHelper.assertFileExists("gen/src/main/resources/simpleResource1.txt")
        projectHelper.assertFileExists("gen/src/main/resources/simpleResource2.txt")

        projectHelper.executeTask(BasePlugin.CLEAN_TASK_NAME)

        projectHelper.assertFileNotExists("gen/src/main/resources")


    }


}
