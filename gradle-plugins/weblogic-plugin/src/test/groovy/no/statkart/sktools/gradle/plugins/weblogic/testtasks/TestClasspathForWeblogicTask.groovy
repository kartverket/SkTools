package no.statkart.sktools.gradle.plugins.weblogic.testtasks

import no.statkart.sktools.gradle.plugins.weblogic.WeblogicTaskInterface
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction

/**
 * Tester at weblogicClasspath blir satt på Task. Se testng test metode.
 *
 * @author Leif Lislegård
 */
class TestClasspathForWeblogicTask extends ConventionTask implements WeblogicTaskInterface {

    private FileCollection weblogicClasspath

    public Object testResult


    public TestClasspathForWeblogicTask() {
    }

    public void setWeblogicClasspath(FileCollection weblogicClasspath) {
        this.weblogicClasspath = weblogicClasspath;
    }

    public FileCollection getWeblogicClasspath() {
        return weblogicClasspath;
    }


    //test spesifik

    @TaskAction
    protected void compile() {
        testResult = getWeblogicClasspath().files
//        setDidWork(true)
    }


}
