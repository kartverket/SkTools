package no.statkart.sktools.gradle.plugins.weblogic

import no.statkart.sktools.gradle.testutils.TestKitBase
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.Test
import no.statkart.sktools.gradle.plugins.weblogic.testtasks.TestClasspathForWeblogicTask

/**
 * Test av {@link WeblogicTaskInterface}.
 *
 * @author Leif Lislegård
 */
class WeblogicTaskTest extends TestKitBase {

    /**
     * Tester at weblogicClasspath blir satt
     */
    @Test
    void testWeblogicClasspath() {
        //creates a dummy.jar
        writeFile('some.jar')


        Project project = ProjectBuilder.builder().build().tap {
            apply plugin: WeblogicBasePlugin
            apply plugin: JavaPlugin


            dependencies {
                compile files('some.jar')
            }
        }

        //tenker oss at weblogic classpath configurasjon også skal inneholde alle compile time dependencies..
        project.configurations.getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME).extendsFrom(project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME))


        TestClasspathForWeblogicTask task = project.task('someWeblogicTask', type: TestClasspathForWeblogicTask)
        task.compile()

        //sjekker task objektet
        task.getWeblogicClasspath().files == task.testResult  //forventer at weblogicClasspath er satt og er lik med expected
        project.files(task.testResult) == project.files('some.jar')
    }


}
