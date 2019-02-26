package no.statkart.sktools.gradle.plugins.weblogic

import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import org.gradle.api.plugins.JavaPlugin
import org.testng.annotations.Test

/**
 * Tester funksjonalitet for {@link WeblogicBasePlugin}
 *
 * @author Leif Lislegård
 */
class WeblogicBasePluginTest {

    /**
     * Tester at konfigurasjon er tilgjengelig for prosjekt.
     */
    @Test
    void testConfiguration() {
        //forks a new rootProject in a temp folder
        ProjectHelper rootProjectHelper = GradleProjectBuilder.builder().build()
        def rootDir = rootProjectHelper.toString()

        //defines dummy.jars
        File someJarFile = rootProjectHelper.project.file('some.jar')
        File otherJarFile = rootProjectHelper.project.file('other.jar')

        //creates a subproject
        ProjectHelper subProjectHelper = GradleProjectBuilder.builder().withName('subproject').withParent(rootProjectHelper).build()

        //configures rootproject
        rootProjectHelper.configureProject {
            apply plugin: WeblogicBasePlugin

            dependencies.weblogicProvided files('some.jar')
        }

        //configures subproject
        subProjectHelper.configureProject {
            apply plugin: WeblogicBasePlugin

            dependencies.weblogicProvided files('../other.jar')
        }

        //asserts
        assert rootProjectHelper.project.configurations.getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME).files.contains(someJarFile)
        assert !rootProjectHelper.project.configurations.getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME).files.contains(otherJarFile)

        assert !subProjectHelper.project.configurations.getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME).files.contains(someJarFile)
        assert subProjectHelper.project.configurations.getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME).files.contains(otherJarFile)


    }

    /**
     * Illustrerer integrasjon med JavaPlugin.
     *
     * Illustrert er at weblogic-configurasjonen arver ifra (java) compile konfigurasjonen.
     * Dependencies som legges til 'compile' skal da komme med i 'weblogic'
     */
    @Test
    void demoConfigurationHierarchy() {
        //forks a new rootProject in a temp folder
        ProjectHelper projectHelper = GradleProjectBuilder.builder().build()

        //defines a dummy.jar
        File somJarFile = projectHelper.project.file('some.jar')

        projectHelper.configureProject {
            apply plugin: WeblogicBasePlugin
            apply plugin: JavaPlugin
            dependencies {
                compile files('some.jar')
            }
        }

        //tenker oss at weblogic classpath configurasjon også skal inneholde alle compile time dependencies..
        projectHelper.project.configurations.getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME).extendsFrom(projectHelper.project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME))


        assert projectHelper.project.configurations.getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME).contains(somJarFile)
    }


}
