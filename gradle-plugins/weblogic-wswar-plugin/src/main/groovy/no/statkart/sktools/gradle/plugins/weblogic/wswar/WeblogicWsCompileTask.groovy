package no.statkart.sktools.gradle.plugins.weblogic.wswar

import no.statkart.sktools.gradle.plugins.weblogic.WeblogicTaskInterface

import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel

import org.gradle.api.tasks.compile.Compile

/**
 * Task for kompilering av java-ws weblogic server implementasjon
 *
 *
 * @since 1.1
 * @author Leif Lislegård
 */
class WeblogicWsCompileTask extends Compile implements WeblogicTaskInterface {

    private FileCollection weblogicClasspath


    WeblogicWsCompileTask() {
        javaCompiler = new WeblogicJaxWsCompiler()
        logging.captureStandardOutput LogLevel.INFO
        logging.captureStandardError LogLevel.DEBUG
    }

    public void setWeblogicClasspath(FileCollection weblogicClasspath) {
        this.weblogicClasspath = weblogicClasspath;
    }

    public FileCollection getWeblogicClasspath() {
        return weblogicClasspath;
    }

    protected void compile() {
        WeblogicJaxWsCompiler compiler = getJavaCompiler()
        compiler.weblogicClasspath = getWeblogicClasspath().files
        compiler.ant = getAnt()
        compiler.baseDir = project.file('src')
        compiler.warName = project.name + ".war"
        super.compile()
    }

}
