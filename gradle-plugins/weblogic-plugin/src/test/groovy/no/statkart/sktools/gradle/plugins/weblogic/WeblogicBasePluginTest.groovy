package no.statkart.sktools.gradle.plugins.weblogic

import no.statkart.sktools.gradle.testutils.TestKitBase
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.testng.annotations.Test

/**
 * Tester funksjonalitet for {@link WeblogicBasePlugin}
 *
 * @author Leif Lislegård
 */
class WeblogicBasePluginTest extends TestKitBase {

    /**
     * Tester at konfigurasjon er tilgjengelig for prosjekt.
     */
    @Test
    void testConfiguration() {

        //defines dummy.jars
        File someJarFile = file('some.jar')
        File otherJarFile = file('subproject/other.jar')

        //configures rootproject
        final Project rootProject = projectBuilder().withName("root").build().tap {
            apply plugin: WeblogicBasePlugin

            dependencies.weblogicProvided files('some.jar')
        }

        //configures subproject
        final Project subProject = projectBuilder().withName("subproject").withProjectDir(file("subproject")).build().tap {
            apply plugin: WeblogicBasePlugin

            dependencies.weblogicProvided files('other.jar')
        }

        //asserts
        assert rootProject.configurations.getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME).files.contains(someJarFile)
        assert subProject.configurations.getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME).files.contains(otherJarFile)

        assert !subProject.configurations.getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME).files.contains(someJarFile)
        assert !rootProject.configurations.getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME).files.contains(otherJarFile)
    }

    /**
     * Illustrerer integrasjon med JavaPlugin.
     *
     * Illustrert er at weblogic-configurasjonen arver ifra (java) compile konfigurasjonen.
     * Dependencies som legges til 'compile' skal da komme med i 'weblogic'
     */
    @Test
    void demoConfigurationHierarchy() {

        //defines a dummy.jar
        File somJarFile = file('some.jar')

        final Project project = projectBuilder().build().tap {
            apply plugin: WeblogicBasePlugin
            apply plugin: JavaPlugin

            configurations {
                //tenker oss at weblogic classpath configurasjon også skal inneholde alle compile time dependencies..
                weblogicProvided.extendsFrom compile
            }

            dependencies {
                compile files('some.jar')
            }

        }

        assert project.configurations.getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME).contains(somJarFile)
    }


}
