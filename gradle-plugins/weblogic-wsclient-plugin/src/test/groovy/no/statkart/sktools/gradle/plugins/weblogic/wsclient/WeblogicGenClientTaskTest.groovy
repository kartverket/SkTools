package no.statkart.sktools.gradle.plugins.weblogic.wsclient

import org.testng.annotations.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import org.testng.Assert

/**
 * @author Leif Lislegård
 */
class WeblogicGenClientTaskTest {

    /**
     * Tester {@link WeblogicGenClientTask#fixResourceLoaders()}
     */
    @Test
    void testResourceRewrite() {

        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()
        def dir = project.mkdir('build')


        File targetFile = new File(dir, 'BorettInformasjonServiceWS.java')
        targetFile.append(this.class.getResourceAsStream('BorettInformasjonServiceWS.orig'))
        targetFile.deleteOnExit()


        WeblogicGenClientTask genClientTask = project.task([type:WeblogicGenClientTask], 'genClient')
        genClientTask.setProject(project)
        genClientTask.setDestinationDir(dir)

        genClientTask.fixResourceLoaders()

        Assert.assertEquals(this.class.getResourceAsStream('BorettInformasjonServiceWS.result').text, targetFile.text)

    }





}
