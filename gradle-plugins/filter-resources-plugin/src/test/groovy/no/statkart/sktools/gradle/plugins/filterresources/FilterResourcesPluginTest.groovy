package no.statkart.sktools.gradle.plugins.filterresources

import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import no.statkart.sktools.gradle.testutils.filewriter.FilterPropertiesTestutilFilewriter
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import org.testng.annotations.Test

/**
 *
 */
class FilterResourcesPluginTest {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testAppplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-filter-resources-plugin'

        assert project.convention.plugins.filterProperties != null
        Assert.assertTrue(project.convention.plugins.filterProperties instanceof FilterResourcesConvention)

    }

    /**
     * Tester og demonstrerer angivelse av konfigurasjon
     */
    @Test
    void testConventionConfiguration() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder().build {
            apply plugin: 'java'
            apply plugin: 'sktools-filter-resources-plugin'
        }

        FilterResourcesConvention convention = (FilterResourcesConvention) projectHelper.project.convention.plugins.get(FilterResourcesPlugin.CONVENTION_NAME)

        projectHelper.initializeProject()

        projectHelper.configureProject {
            filterResources {
                properties = [singleProperty: 'singleValue']
            }
        }

        assert convention.properties == ['singleProperty': 'singleValue']

        projectHelper.setProjectProperties(['projectProperty': 'projectValue'])


        projectHelper.configureProject {
            filterResources {
                properties = projectHelper.project.ext.properties
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
        ProjectHelper projectHelper = GradleProjectBuilder.builder("PropertiesFilterTest").build {
            apply plugin: 'java'
            apply plugin: 'sktools-filter-resources-plugin'
        }

        use(FilterPropertiesTestutilFilewriter) {
            projectHelper.writeTwoSimpleResources('src/main/filterResources')
        }

        projectHelper.setProjectProperties([myProperty2: 'testValue', myEmail: 'unittest'])
        assert projectHelper.project.getName() == "PropertiesFilterTest"

        projectHelper.initializeProject()



        projectHelper.executeTask(FilterResourcesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)
        projectHelper.assertTaskExecutedNotSkipped(FilterResourcesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)

        projectHelper.assertFileExists("build/filteredResources/main/simpleResource1.txt") { File file ->
            assert file.text.contains("name=PropertiesFilterTest")
            assert file.text.contains("version=unspecified")
            assert file.text.contains("myProperty1=@myProperty1@")
        }

        projectHelper.assertFileExists("build/filteredResources/main/simpleResource2.txt") { File file ->
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
        ProjectHelper projectHelper = GradleProjectBuilder.builder("PropertiesFilterTest").build {
            apply plugin: 'java'
            apply plugin: 'sktools-filter-resources-plugin'
        }

        use(FilterPropertiesTestutilFilewriter) {
            projectHelper.writeTwoSimpleResources('src/main/filterResources')
        }

        projectHelper.setProjectProperties([myProperty1: 'testValue'])

        projectHelper.configureProject {
            filterResources {
                properties projectHelper.project.ext.properties
                properties myProperty1: 'overidenValue',
                        'name': 'overidenName'

            }
        }

        projectHelper.initializeProject()



        projectHelper.executeTask(FilterResourcesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)
        projectHelper.assertTaskExecutedNotSkipped(FilterResourcesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)

        projectHelper.assertFileExists("build/filteredResources/main/simpleResource1.txt") { File file ->
            assert file.text.contains("name=overidenName")
            assert file.text.contains("myProperty1=overidenValue")
        }

    }

    /**
     * Tester 'processResources' task blir satt opp riktig.
     */
    @Test
    void testResources() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder("PropertiesFilterTest").build {
            apply plugin: 'java'
            apply plugin: 'sktools-filter-resources-plugin'
        }

        use(FilterPropertiesTestutilFilewriter) {
            projectHelper.writeTwoSimpleResources('src/main/filterResources')
        }

        projectHelper.setProjectProperties([myProperty1: 'testValue'])
        projectHelper.initializeProject()

        projectHelper.executeTask("processResources")
//        projectHelper.assertTaskExecutedNotSkipped("processResources")
        projectHelper.assertTaskExecutedNotSkipped(FilterResourcesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)

        projectHelper.assertFileExists("build/filteredResources/main/simpleResource1.txt") { File file ->
            assert file.text.contains("myProperty1=testValue")
        }

    }

    /**
     * Tester at genererte filer blir cleanet ved {@code clean}
     */
    @Test
    void testClean() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder("PropertiesFilterTest").build {
            apply plugin: 'java'
            apply plugin: 'sktools-filter-resources-plugin'
        }

        use(FilterPropertiesTestutilFilewriter) {
            projectHelper.writeTwoSimpleResources('src/main/filterResources')
        }

        projectHelper.initializeProject()

        projectHelper.executeTask(FilterResourcesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)
        projectHelper.assertTaskExecutedNotSkipped(FilterResourcesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)

        projectHelper.assertFileExists("build/filteredResources/main")
        projectHelper.assertFileExists("build/filteredResources/main/simpleResource1.txt")
        projectHelper.assertFileExists("build/filteredResources/main/simpleResource2.txt")

        projectHelper.executeTask(BasePlugin.CLEAN_TASK_NAME)

        projectHelper.assertFileNotExists("build/filteredResources/main")


    }

    /**
     * SKIF-173
     *
     * Tester og demonstrerer angivelse av konfigurasjon
     */
    @Test
    void testCustomPathsConfiguration() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder().build {
            apply plugin: 'java'
            apply plugin: 'sktools-filter-resources-plugin'
        }

        //definerer to source set med filtrerte ressurser
        projectHelper.configureProject {
            sourceSets {
                main {
                    filterResources {
                        srcDir 'src/special/main'
                    }
                    output.filterResourcesOutput 'gen/special/main'
                }
                test {
                    filterResources {
                        srcDir 'src/special/test'
                    }
                    output.filterResourcesOutput 'gen/special/test'
                }
            }
        }

        //skriver noen filer til disk
        use(FilterPropertiesTestutilFilewriter) {
            projectHelper.writeCustomFile('src/special/main/file1.txt') { "file1.version=@version@" }
            projectHelper.writeCustomFile('src/special/test/file2.txt') { "file2.version=@version@" }
        }

        //eksekverer
        projectHelper.initializeProject()
        projectHelper.executeTask(FilterResourcesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)
        projectHelper.executeTask(FilterResourcesPlugin.FILTER_TEST_RESOURCES_TASK_NAME)
        projectHelper.assertTaskExecutedNotSkipped(FilterResourcesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)
        projectHelper.assertTaskExecutedNotSkipped(FilterResourcesPlugin.FILTER_TEST_RESOURCES_TASK_NAME)

        //tester resultat
        projectHelper.assertFileExists("gen/special/main/file1.txt")
        projectHelper.assertFileNotExists("gen/special/test/file1.txt")

        projectHelper.assertFileExists("gen/special/test/file2.txt")
        projectHelper.assertFileNotExists("gen/special/main/file2.txt")

    }

    /**
     * SKIF-173
     *
     * Tester at filtrerte filer ikke kommer med som {@code source} eller {@code resources}
     */
    @Test
    void testSourceSetIkkeOverlapper() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder().build {
            apply plugin: 'java'
            apply plugin: 'sktools-filter-resources-plugin'
        }

        //definerer to source set med filtrerte ressurser
        projectHelper.configureProject {
            sourceSets {
                main {
                    filterResources {
                        srcDir 'src/main/java' //ressursfiler finnes sammed med kildekoden
                    }
                    output.filterResourcesOutput 'gen/main/resources'
                }
                test {
                    filterResources {
                        srcDir 'src/test/resources'
                        include '*.txt' //kun et utvalg av test resources skal filtreres, resten skal resources håndtere
                    }
                    output.filterResourcesOutput 'gen/test/resources'
                }
            }
        }

        //skriver noen filer til disk
        use(FilterPropertiesTestutilFilewriter) {
            projectHelper.writeCustomFile('src/main/java/file1.txt') { "file1.txt" }
            projectHelper.writeCustomFile('src/main/java/file1.java') { "interface file1 {}" }
            projectHelper.writeCustomFile('src/test/resources/file2.txt') { "file2.txt" }
            projectHelper.writeCustomFile('src/test/resources/file2.nofilter') { "file2.nofilter" }
        }

        //eksekverer
        projectHelper.initializeProject()
        projectHelper.executeTask("build")
        projectHelper.assertTaskExecutedNotSkipped(FilterResourcesPlugin.FILTER_MAIN_RESOURCES_TASK_NAME)
        projectHelper.assertTaskExecutedNotSkipped(FilterResourcesPlugin.FILTER_TEST_RESOURCES_TASK_NAME)

        //tester resultat
        projectHelper.assertFileExists("gen/main/resources/file1.txt")
        projectHelper.assertFileExists("gen/test/resources/file2.txt")
        projectHelper.assertFileExists("${projectHelper.project.sourceSets.main.output.classesDir}/file1.class")
        projectHelper.assertFileExists("build/classes/java/main/file1.class")
        projectHelper.assertFileExists("build/resources/test/file2.nofilter")

        //tester tilsvarende på SourceSet
        Project project = projectHelper.project
        assert !project.sourceSets.main.allJava.contains(projectHelper.assertFileExists("src/main/java/file1.txt"))
        assert !project.sourceSets.test.resources.contains(projectHelper.assertFileExists("src/test/resources/file2.txt"))
    }

    /**
     * SKIF-173
     *
     */
    @Test
    void testClasspathForSourceSet() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder().build {
            apply plugin: 'java'
            apply plugin: 'sktools-filter-resources-plugin'
        }


        projectHelper.configureProject {
            sourceSets {
                coolCode {
                    filterResources.srcDirs = ['src/code/unfiltered', 'src/easter/eggs']
                    output.filterResourcesOutput 'build/gen/so/cool'
                }
            }
        }

        projectHelper.project.file('src/easter/eggs').mkdirs()
        projectHelper.project.file('src/easter/eggs/resource1.txt').createNewFile()

        projectHelper.project.file('src/code/unfiltered').mkdirs()
        projectHelper.project.file('src/code/unfiltered/resource2.txt').createNewFile()

        //eksekverer
        projectHelper.initializeProject()

        Project project = projectHelper.project

        //tester main sourceset
        ((SourceSet) project.sourceSets.main).with {
            assert resources.srcDirs.contains(output.filterResourcesOutputDir)

            assert output.contains(output.classesDir)
            assert output.contains(output.resourcesDir)

            assert runtimeClasspath.contains(output.classesDir)
            assert runtimeClasspath.contains(output.resourcesDir)
        }

        //tester coolCode sourceset
        ((SourceSet) project.sourceSets.coolCode).with {
            assert resources.srcDirs.contains(output.filterResourcesOutputDir)

            assert output.contains(output.classesDir)
            assert output.contains(output.resourcesDir)

            assert runtimeClasspath.contains(output.classesDir)
            assert runtimeClasspath.contains(output.resourcesDir)
        }


        projectHelper.executeTask(projectHelper.project.sourceSets.coolCode.filterResourcesTaskName)

        projectHelper.assertFileExists('build/gen/so/cool/resource1.txt', 'Forventer at ressursfil er generert')
        projectHelper.assertFileExists('build/gen/so/cool/resource2.txt', 'Forventer at ressursfil er generert')
    }
}
