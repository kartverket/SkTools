package no.statkart.sktools.gradle.plugins.webstart

import org.testng.annotations.Test
import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import no.statkart.sktools.gradle.testutils.ProjectHelper
import org.gradle.api.tasks.bundling.War
import no.statkart.sktools.gradle.testutils.builder.WebstartProjectBuilder
import org.gradle.api.Project

/**
 * Test av {@link JnlpServletWarTask}
 * @author Leif Lislegård
 */
class JnlpServletWarTaskTest {

    /**
     * Tester plugin integrasjon med WarPlugin og WarTasks
     */
    @Test
    void testWarTaskIntegration() {
        ProjectHelper projectHelper = WebstartProjectBuilder.builder().applyWebstartPlugin().applyWarPlugin().build()
        projectHelper.configureProject {

            webstart {
                warTasks
            }

            project.task('makeWar', type:War)
        }

        projectHelper.initializeProject()
        projectHelper.executeTask(WebstartPlugin.JNLP_SERVLET_JARS_TASK_NAME)   //eksekverer denne slik at output for task blir generert

        Project project = projectHelper.project
        ['war', 'makeWar'].each {
            assert project.tasks[it].classpath.asPath =~ "jnlp-servlet.jar"
        }


    }

    /**
     * Demonstrerer bruk uten aktivering av plugin. (kun task)
     */
    @Test
    void testCustomUsage() {

        ProjectHelper projectHelper = GradleProjectBuilder.builder().build()

        projectHelper.configureProject {

            project.task('findJars', type:JnlpServletWarTask)

            project.task('makeWar', type:War) {
//                dependsOn 'findJars'
                classpath project.tasks['findJars']
            }

        }

        projectHelper.executeTask('findJars')   //eksekverer denne slik at output for task blir generert

        War warTask = (War) projectHelper.project.tasks['makeWar']
        assert warTask.getClasspath().getAsPath() =~ "jnlp-servlet.jar"

    }
}
