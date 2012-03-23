package no.statkart.sktools.gradle.plugins.weblogic

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
class WeblogicTaskTest {

    /**
     * Tester at weblogicClasspath blir satt
     */
    @Test
    void testWeblogicClasspath() {
        //forks a new rootProject in a temp folder
        Project project = ProjectBuilder.builder().build()
        def rootDir = project.rootDir

        //creates a dummy.jar
        project.file('some.jar').createNewFile()


        project.apply plugin: WeblogicBasePlugin
        project.apply plugin: JavaPlugin

        //tenker oss at weblogic classpath configurasjon også skal inneholde alle compile time dependencies..
        project.configurations.getByName(WeblogicBasePlugin.WEBLOGIC_CONFIGURATION_NAME).extendsFrom(project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME))

        //configure project
        project.with {
            dependencies.compile files('some.jar')
        }

        project.task('someWeblogicTask', type: TestClasspathForWeblogicTask)


        project.someWeblogicTask.execute()

        //sjekker task objektet
        project.someWeblogicTask.with {
            assert state.skipped == false
            assert state.executed == true

            assert getWeblogicClasspath().files == testResult  //forventer at weblogicClasspath er satt og er lik med expected
            assert testResult == project.files('some.jar').files
        }
    }


}
