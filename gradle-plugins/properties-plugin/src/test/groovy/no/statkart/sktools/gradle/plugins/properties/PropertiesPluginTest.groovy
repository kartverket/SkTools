package no.statkart.sktools.gradle.plugins.properties


import no.statkart.sktools.gradle.testutils.TestKitBase
import org.assertj.core.api.Assertions
import org.gradle.testkit.runner.BuildResult
import org.testng.annotations.Test

/**
 * @see PropertiesPlugin
 */
@Test
class PropertiesPluginTest extends TestKitBase {

    /**
     * Tester registrering av plugin via navn
     * og at extension er registrert.
     */
    @Test
    void testApplyPlugin() {
        writeFile("build.gradle", '''
            plugins {
              id 'sktools-properties-plugin'
            }
            
            assert propertyUtils instanceof no.statkart.sktools.gradle.plugins.properties.extension.PropertyUtils
        ''')

        assertNoFailures(testGradleBuild("tasks"))
    }

    /**
     * SKTOOLS-70
     */
    @Test
    void testInteroperabilityWithMavenPublish() {
        writeFile("build.gradle", '''
            plugins {
              id 'maven-publish'
              id 'sktools-properties-plugin'
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
        writeFile("subproject/build.gradle", '''
            plugins {
              id 'sktools-properties-plugin'
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

        writeFile("build.gradle", '''
            ext.testProperty = 'TestValue'
        ''')

        writeFile("settings.gradle", "include ':subproject'")


        BuildResult result = testGradleBuild("echoProperty")
        Assertions.assertThat(result.getOutput())
                .contains('My filtered property is: filteredTestValue');
    }

}
