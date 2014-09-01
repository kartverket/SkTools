package no.statkart.sktools.gradle.plugins.properties

import no.statkart.sktools.gradle.plugins.properties.extension.PropertyUtils
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.PropertiesProjectBuilder
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import org.testng.annotations.Test

/**
 * @see PropertiesPlugin
 */
class PropertiesPluginTest {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testAppplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-properties-plugin'

        assert project.extensions.propertyUtils != null
        Assert.assertTrue(project.extensions.propertyUtils instanceof PropertyUtils)

    }

    /**
     * SKTOOLS-70
     */
    @Test
    void testInteroperabilityWithMavenPublish() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        if (project.gradle.gradleVersion < '1.6') {
            println "Skipping test for gradle ${project.gradle.gradleVersion}"
            return; //incubating feature in gradle 1.6
        }

        project.apply plugin: 'maven-publish'
        project.apply plugin: 'sktools-properties-plugin'

        assert project.extensions.propertyUtils != null

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

    }

    /**
     * SKTOOLS-70
     * Asserts that a child project reads parent properties when property is not found (gradle feature)
     */
    @Test
    void testParentProjectProperties() {
        ProjectHelper rootProjectHelper = PropertiesProjectBuilder.builder().applyJavaPlugin().applyPropertiesPlugin().build()
        ProjectHelper child1ProjectHelper = PropertiesProjectBuilder.builder().withParent(rootProjectHelper).applyPropertiesPlugin().build()

        rootProjectHelper.setProjectProperties(testProperty: "TestValue");
        Assert.assertEquals(child1ProjectHelper.project.property("testProperty"), "TestValue")

        child1ProjectHelper.setProjectProperties(filteredProperty: "filtered\${testProperty}");
        Assert.assertEquals(child1ProjectHelper.project.property("filteredProperty"), "filtered\${testProperty}")

        //ekspanderer properties..
        child1ProjectHelper.configureProject {
            propertyUtils.expandProjectProperties()
        }

        Assert.assertEquals(child1ProjectHelper.project.property("filteredProperty"), "filteredTestValue")
    }
}
