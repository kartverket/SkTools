package no.statkart.sktools.gradle.plugins.wsdlcustomizer

import no.statkart.sktools.gradle.testutils.TestKitBase
import org.gradle.api.Project
import org.testng.annotations.Test

import static org.assertj.core.api.Assertions.assertThat

class WsdlCustomizerPluginTest extends TestKitBase {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testApplyPlugin() {
        //forks a new project in a temp folder
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-wsdl-customizer-plugin'
        }

        assertThat(project.getPlugins().getPlugin(WsdlCustomizerPlugin.class)).isNotNull()
    }

    /**
     * Tester plugin via alternativt navn
     */
    @Test
    void testApplyPlugin2() {
        writeFileUTF8("build.gradle", '''\
            plugins {
                id 'sktools.wsdl-customizer'
            }
        ''')
        assertNoFailures(testGradleBuild("tasks"))
    }


}
