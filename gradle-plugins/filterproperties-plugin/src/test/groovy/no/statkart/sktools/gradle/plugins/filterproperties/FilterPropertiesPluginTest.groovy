package no.statkart.sktools.gradle.plugins.filterproperties

import org.testng.annotations.Test
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert

import no.statkart.sktools.gradle.testutils.builder.FilterPropertiesProjectBuilder
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.filewriter.FilterPropertiesTestutilFilewriter
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin

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

        projectHelper.initializeProject()

        projectHelper.configureProject {
            filteredProperties {
                properties = [singleProperty:'singleValue']
            }
        }

        assert convention.properties == ['singleProperty':'singleValue']

        projectHelper.setProjectProperties(['projectProperty':'projectValue'])


        projectHelper.configureProject {
            filteredProperties {
                properties = propertyUtils.projectProperties()
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
            projectHelper.writeTwoSimpleResources('src/main/filterResources')
        }

        projectHelper.setProjectProperties([myProperty2:'testValue', myEmail:'unittest'])
        assert projectHelper.project.getName() == "PropertiesFilterTest"

        projectHelper.initializeProject()



        projectHelper.executeTask(FilterPropertiesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)
        projectHelper.assertTaskExecutedNotSkipped(FilterPropertiesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)

        projectHelper.assertFileExists("gen/main/resources/simpleResource1.txt") { File file ->
            assert file.text.contains("name=PropertiesFilterTest")
            assert file.text.contains("version=unspecified")
            assert file.text.contains("myProperty1=@myProperty1@")
        }

        projectHelper.assertFileExists("gen/main/resources/simpleResource2.txt") { File file ->
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
            projectHelper.writeTwoSimpleResources('src/main/filterResources')
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

        projectHelper.assertFileExists("gen/main/resources/simpleResource1.txt") { File file ->
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
            projectHelper.writeTwoSimpleResources('src/main/filterResources')
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
            projectHelper.writeTwoSimpleResources('src/main/filterResources')
        }

        projectHelper.initializeProject()

        projectHelper.executeTask(FilterPropertiesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)
        projectHelper.assertTaskExecutedNotSkipped(FilterPropertiesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)

        projectHelper.assertFileExists("gen/main/resources")
        projectHelper.assertFileExists("gen/main/resources/simpleResource1.txt")
        projectHelper.assertFileExists("gen/main/resources/simpleResource2.txt")

        projectHelper.executeTask(BasePlugin.CLEAN_TASK_NAME)

        projectHelper.assertFileNotExists("gen/main/resources")


    }


    /**
     * SKIF-173
     *
     * Tester og demonstrerer angivelse av konfigurasjon
     */
    @Test
    void testCustomPathsConfiguration() {
        ProjectHelper projectHelper = FilterPropertiesProjectBuilder.builder().applyJavaPlugin().applyFilterPropertiesPlugin().build()

        //definerer to source set med filtrerte ressurser
        projectHelper.configureProject {
            sourceSets {
                main {
                    filterResources {
                       srcDir 'src/special/main'
                    }
                    filterResourcesOutput 'gen/special/main'
                }
                test {
                    filterResources {
                       srcDir 'src/special/test'
                    }
                    filterResourcesOutput 'gen/special/test'
                }
            }
        }

        //skriver noen filer til disk
        use(FilterPropertiesTestutilFilewriter) {
            projectHelper.writeCustomFile('src/special/main/file1.txt') { "file1.version=@version@"}
            projectHelper.writeCustomFile('src/special/test/file2.txt') { "file2.version=@version@"}
        }

        //eksekverer
        projectHelper.initializeProject()
        projectHelper.executeTask(FilterPropertiesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)
        projectHelper.executeTask(FilterPropertiesPlugin.FILTER_TEST_RESOURCES_TASK_NAME)
        projectHelper.assertTaskExecutedNotSkipped(FilterPropertiesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)
        projectHelper.assertTaskExecutedNotSkipped(FilterPropertiesPlugin.FILTER_TEST_RESOURCES_TASK_NAME)


        //tester resultat
        projectHelper.assertFileExists("gen/special/main/file1.txt")
        projectHelper.assertFileNotExists("gen/special/test/file1.txt")

        projectHelper.assertFileExists("gen/special/test/file2.txt")
        projectHelper.assertFileNotExists("gen/special/main/file2.txt")

    }

    /**
     * SKIF-173
     *
     * Tester at filtrerte filer ikke kommer med som {@code source} eller {@resources}
     */
    @Test
    void testSourceSetIkkeOverlapper() {
        ProjectHelper projectHelper = FilterPropertiesProjectBuilder.builder().applyJavaPlugin().applyFilterPropertiesPlugin().build()

        //definerer to source set med filtrerte ressurser
        projectHelper.configureProject {
            sourceSets {
                main {
                    filterResources {
                       srcDir 'src/main/java' //ressursfiler finnes sammed med kildekoden
                    }
                    filterResourcesOutput 'gen/main/resources'
                }
                test {
                    filterResources {
                       srcDir 'src/test/resources'  //kun et utvalg av test resources skal filtreres
                    }
                    filterResourcesOutput 'gen/test/resources'
                }
            }
        }

        //skriver noen filer til disk
        use(FilterPropertiesTestutilFilewriter) {
            projectHelper.writeCustomFile('src/main/java/file1.txt') { "file1.txt"}
            projectHelper.writeCustomFile('src/main/java/file1.java') { "interface file1 {}"}
            projectHelper.writeCustomFile('src/test/resources/file2.txt') { "file2.txt"}
            projectHelper.writeCustomFile('src/test/resources/file2.nofilter') { "file2.nofilter"}
        }

        //eksekverer
        projectHelper.initializeProject()
        projectHelper.executeTask("build")
        projectHelper.assertTaskExecutedNotSkipped(FilterPropertiesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)
        projectHelper.assertTaskExecutedNotSkipped(FilterPropertiesPlugin.FILTER_TEST_RESOURCES_TASK_NAME)


        //tester resultat
        projectHelper.assertFileExists("gen/main/resources/file1.txt")
        projectHelper.assertFileExists("gen/test/resources/file2.txt")
        projectHelper.assertFileExists("build/classes/main/file1.class")
        projectHelper.assertFileExists("build/resources/test/file2.nofilter")

        //tester tilsvarende på SourceSet
        Project project = projectHelper.project
        assert !project.sourceSets.main.allJava.contains(projectHelper.assertFileExists("src/main/java/file1.txt"))
        assert !project.sourceSets.test.resources.contains(projectHelper.assertFileExists("src/test/resources/file2.txt"))
    }

    /**
     * SKIF-173
     *
     * Demonstrerer arving av {@link org.gradle.api.tasks.SourceSet}
     */
    @Test
    void testExtendingSourceSets() {
        ProjectHelper projectHelper = FilterPropertiesProjectBuilder.builder().applyJavaPlugin().applyFilterPropertiesPlugin().build()

        //skriver noen filer til disk
        use(FilterPropertiesTestutilFilewriter) {
            projectHelper.writeCustomFile('src/main/java/file1.txt') { "file1.txt.version=@version@"}
            projectHelper.writeCustomFile('src/main/java/file2.doc') { "file2.doc.version=@version@"}
            projectHelper.writeCustomFile('src/main/java/file3.java') { 'class file3 { String version = "@version@"; }'}
        }

        //definerer to source set med filtrerte ressurser
        projectHelper.configureProject {
            sourceSets {
                main {
                    filterResources {
                       srcDir 'src/main/java'
                       exclude '*.java'
                       exclude '*.doc'
                    }
                }
                test {
                    resources {
                       source main.filterResources    //litt tullete, men allikevel - test resources er samme som ufiltrerte main resources
                    }
                }
            }
        }


        //eksekverer
        projectHelper.initializeProject()
        projectHelper.executeTask("build")
        projectHelper.assertTaskExecutedNotSkipped(FilterPropertiesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)
        projectHelper.assertTaskExecutedNotSkipped(JavaPlugin.PROCESS_TEST_RESOURCES_TASK_NAME)


        //tester resultat
        projectHelper.assertFileExists("gen/main/resources/file1.txt") {!it.text.contains('@version@') }
        projectHelper.assertFileNotExists("gen/main/resources/file2.doc")

        projectHelper.assertFileExists("build/classes/main/file3.class") {it.text.contains('@version@') }

        projectHelper.assertFileExists("build/resources/test/file1.txt") {it.text.contains('@version@') }
        projectHelper.assertFileNotExists("build/resources/test/file2.doc")

    }
}
