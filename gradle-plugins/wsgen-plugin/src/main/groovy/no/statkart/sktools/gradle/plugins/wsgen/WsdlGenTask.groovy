package no.statkart.sktools.gradle.plugins.wsgen

import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

class WsdlGenTask extends SourceTask {
    @CompileClasspath
    FileCollection classpath

    @CompileClasspath
    FileCollection jaxwsClasspath

    @OutputDirectory
    File destinationDir

    @TaskAction
    protected void genWsdl() {
        getProject().delete(getDestinationDir())
        getProject().mkdir(getDestinationDir())

        def wsBeans = getSource().matching { include '**/*WSBean.class' }
        def cp = getClasspath().asPath

        wsBeans.visit { FileVisitDetails details ->
            if (!details.directory) {
                String path = details.getRelativePath()
                String classname = path.substring(0, path.length() - 6) //removing '.class'
                    .replace('/' as char, '.' as char)

                getProject().javaexec {
                    classpath = getJaxwsClasspath()
                    main = 'com.sun.tools.ws.WsGen'
                    args '-d', getTemporaryDir()
                    args '-keep'
                    args '-encoding', 'UTF-8'
                    args '-classpath', cp
                    args '-Xnocompile'
                    args '-wsdl'
                    args '-r', getDestinationDir()
                    args classname
                }
            }
        }
    }

}
