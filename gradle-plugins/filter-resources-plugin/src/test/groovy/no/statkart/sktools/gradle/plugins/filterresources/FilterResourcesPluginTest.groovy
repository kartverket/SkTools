package no.statkart.sktools.gradle.plugins.filterresources

import no.statkart.sktools.gradle.testutils.TestKitBase
import org.gradle.api.Project
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.testng.Assert
import org.testng.annotations.Test

import static no.statkart.sktools.gradle.plugins.filterresources.FilterPropertiesTestutil.writeTwoSimpleResources
import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.contentOf
import static org.assertj.core.api.Assertions.linesOf

class FilterResourcesPluginTest extends TestKitBase {

    /**
     * Tester registrering av plugin via navn
     * og at extension er registrert.
     */
    @Test
    void testApplyPlugin() {
        final Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-filter-resources-plugin'
        }

        Assert.assertTrue(project.convention.plugins.filterProperties instanceof FilterResourcesConvention)
    }


    /**
     * Tester bruk med standard verdier.
     */
    @Test
    void testDefaultValues() {
        writeFileUTF8("build.gradle", '''\
            plugins {
              id 'java'
              id 'sktools.filter-resources'
            }

            project.ext.myProperty2 = 'testValue'
            project.ext.myEmail = 'unittest'

        ''')

        writeFileUTF8("settings.gradle", '''\
            rootProject.name = 'PropertiesFilterTest'
        ''')

        writeTwoSimpleResources(projectPath, "src/main/filterResources");

        testGradleBuild("filterResources")

        assertThat(linesOf(file("build/filteredResources/main/simpleResource1.txt")))
                .contains("name=PropertiesFilterTest"
                        , "version=unspecified"
                        , "myProperty1=@myProperty1@")

        assertThat(linesOf(file("build/filteredResources/main/simpleResource2.txt")))
                .contains("myProperty1=@myProperty1@"
                        , "myProperty2=testValue"
                        , "myEmail=unittest@statkart.no")
    }

    /**
     * Tester bruk med egne properties.
     */
    @Test
    void testCustomProperties() {
        writeFileUTF8("build.gradle", '''\
            plugins {
              id 'java'
              id 'sktools.filter-resources'
            }

            project.ext.myProperty1 = 'testValue'

            filterResources {
                properties project.ext.properties
                properties myProperty1: 'overriddenValue', 'name': 'overriddenName'
                properties version: project.version
            }

        ''')

        writeTwoSimpleResources(projectPath, "src/main/filterResources");

        testGradleBuild("filterResources")

        assertThat(linesOf(file("build/filteredResources/main/simpleResource1.txt")))
                .contains("name=overriddenName"
                        , "version=unspecified"
                        , "myProperty1=overriddenValue")
    }


    /**
     * Tester at genererte filer blir slettet ved {@code clean}
     */
    @Test
    void testClean() {
        writeFileUTF8("build.gradle", '''\
            plugins {
              id 'java'
              id 'sktools.filter-resources'
            }

            sourceSets {
              main {
                filterResources.into 'gen/filtered' //utenfor mappen "build" som alltid slettes
              }
            }
        ''')

        writeTwoSimpleResources(projectPath, "src/main/filterResources");

        testGradleBuild("filterResources")
        assertThat(file("gen/filtered/simpleResource1.txt")).exists()

        testGradleBuild("clean")
        assertThat(file("gen/filtered/simpleResource1.txt")).doesNotExist()
    }

    /**
     * Egen betemte filstier
     */
    @Test
    void testCustomPathsConfiguration() {
        writeFileUTF8("build.gradle", '''\
            plugins {
              id 'java'
              id 'sktools.filter-resources'
            }

            sourceSets {
                main {
                    filterResources {
                        srcDir 'src/special/main'
                        into 'gen/special/main'
                    }
                }
                test {
                    filterResources {
                        srcDir 'src/special/test'
                        into 'gen/special/test'
                    }
                }
            }
        ''')

        //skriver noen filer til disk
        writeFileUTF8("src/special/main/file1.txt", "file1.version=@version@")
        writeFileUTF8("src/special/test/file2.txt", "file2.version=@version@")


        BuildResult buildResult = testGradleBuild("filterResources", "filterTestResources")
        assertThat(buildResult.tasks(TaskOutcome.SUCCESS)).hasSize(2);
        assertThat(buildResult.tasks(TaskOutcome.SKIPPED)).hasSize(0);

        assertThat(file("gen/special/main/file1.txt")).exists()
        assertThat(file("gen/special/test/file2.txt")).exists()

        assertThat(file("gen/special/main/file2.txt")).doesNotExist()
        assertThat(file("gen/special/test/file1.txt")).doesNotExist()
    }


    /**
     * Classpath får filtrerte filer
     */
    @Test
    void testClasspathForSourceSet() {
        writeFileUTF8("build.gradle", '''\
            plugins {
              id 'java'
              id 'sktools.filter-resources'
            }

            sourceSets {
                coolCode {
                    filterResources.srcDirs = ['src/code/unfiltered', 'src/easter/eggs']
                    filterResources.into 'build/gen/so/cool'
                }
            }

            task printRuntimeClasspath() {
              doLast {
                sourceSets.main.runtimeClasspath.each { println it }
              }
            }

            task printCoolCodeRuntimeClasspath() {
              doLast {
                sourceSets.coolCode.runtimeClasspath.each { println it }
              }
            }

        ''')

        writeFileUTF8("src/easter/eggs/resource1.txt", "text1")
        writeFileUTF8("src/code/unfiltered/resource2.txt", "text2")


        def runtimeClasspath = testGradleBuild("printRuntimeClasspath").getOutput()
        assertThat(runtimeClasspath)
                .contains(":printRuntimeClasspath")
                .contains(file("build/classes/java/main").toString())
                .contains(file("build/resources/main").toString());

        def coolCodeRuntimeClasspath = testGradleBuild("printCoolCodeRuntimeClasspath").getOutput()
        assertThat(coolCodeRuntimeClasspath)
                .contains(":printCoolCodeRuntimeClasspath")
                .contains(file("build/classes/java/coolCode").toString())
                .contains(file("build/resources/coolCode").toString());

        testGradleBuild("processCoolCodeResources")
        assertThat(file("build/resources/coolCode/resource1.txt")).exists()
        assertThat(file("build/resources/coolCode/resource2.txt")).exists()
    }


    /**
     * Integrasjon med IntelliJ
     */
    @Test
    void testIdeaIntegration() {
        writeFileUTF8("build.gradle", '''\
            plugins {
              id 'java'
              id 'sktools.filter-resources'
              id 'idea'
            }

            sourceSets {
                main {
                    filterResources.into 'build/gen/so/cool' //custom placement
                }
            }
        ''')

        testGradleBuild("ideaModule")
        assertThat(contentOf(file(rootProjectName() + ".iml")))
                .contains('"file://$MODULE_DIR$/build/gen/so/cool"') //customized placement
                .contains('"file://$MODULE_DIR$/build/filteredResources/test"') //conventional placement
    }

}
