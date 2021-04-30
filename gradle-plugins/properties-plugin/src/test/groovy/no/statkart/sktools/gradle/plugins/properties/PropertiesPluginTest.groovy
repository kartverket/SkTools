package no.statkart.sktools.gradle.plugins.properties

import no.statkart.sktools.gradle.plugins.properties.extension.PropertyUtils
import no.statkart.sktools.gradle.testutils.TestKitBase
import org.assertj.core.api.Assertions
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.testng.Assert
import org.testng.annotations.Test

/**
 * @see PropertiesPlugin
 */
class PropertiesPluginTest extends TestKitBase {

    /**
     * Tester registrering av plugin via navn
     * og at extension er registrert.
     */
    @Test
    void testApplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build().tap {
            apply plugin: 'sktools-properties-plugin'
        }

        Assert.assertTrue(project.extensions.propertyUtils instanceof PropertyUtils)
    }

    /**
     * SKTOOLS-70
     */
    @Test
    void testInteroperabilityWithMavenPublish() {
        writeFileUTF8("build.gradle", '''\
            plugins {
              id 'maven-publish'
              id 'sktools.properties'
            }

            project.propertyUtils.expandProjectProperties()

            //feilen kom her..
            project.plugins.withType(org.gradle.api.publish.maven.plugins.MavenPublishPlugin.class) {
                project.publishing {
                    repositories {
                        maven {
                            url 'http://no.domain'
                            credentials {
                                username = 'dummy'
                                password = 'password'
                            }
                        }
                    }
                }
            }
        ''')

        assertNoFailures(testGradleBuild("tasks"))
    }

    /**
     * SKTOOLS-70
     * Asserts that a child project reads parent properties when property is not found (gradle feature)
     */
    @Test
    void testParentProjectProperties() {
        writeFileUTF8("subproject/build.gradle", '''\
            plugins {
              id 'sktools.properties'
            }

            ext.filteredProperty = "filtered${testProperty}"

            //expanding properties in build config..
            propertyUtils.expandProjectProperties()

            task echoProperty() {
              doFirst {
                 println "My filtered property is: " + filteredProperty
               }
            }
        ''')

        writeFileUTF8("build.gradle", '''\
            ext.testProperty = 'TestValue'
        ''')

        writeFileUTF8("settings.gradle", "include ':subproject'")


        BuildResult result = testGradleBuild("echoProperty")
        Assertions.assertThat(result.getOutput())
                .contains('My filtered property is: filteredTestValue');
    }

}
