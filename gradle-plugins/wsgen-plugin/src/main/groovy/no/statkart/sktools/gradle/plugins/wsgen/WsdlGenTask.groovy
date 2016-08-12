package no.statkart.sktools.gradle.plugins.wsgen

import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

public class WsdlGenTask extends ConventionTask {
    @InputFiles
    FileCollection input;

    @InputFiles
    FileCollection classpath;

    FileCollection jaxwsClasspath;

    @OutputDirectory
    File destinationDir;

    @TaskAction
    protected void genWsdl() {
        ant.taskdef(name: 'wsgen', classname: 'com.sun.tools.ws.ant.WsGen', classpath: getJaxwsClasspath().getAsPath())
        ant.delete(dir: getDestinationDir(), quiet: 'true')
        ant.mkdir(dir: getDestinationDir())

        def wsBeans = getInput().asFileTree.matching { include '**/*WSBean.class' }

        def completeClasspath = getClasspath() + getInput();
        ant.path(id: this.getPath() ,path: completeClasspath.asPath)

        wsBeans.visit { FileVisitDetails details ->
            if (!details.directory) {
                String path = details.relativePath
                String classname = path.replace('/', '.').substring(0, path.length() - 6)
                ant.wsgen(sei: classname, classpathref: this.getPath(), destdir: getTemporaryDir(), resourcedestdir: getDestinationDir(), genwsdl: 'true')
            }
        }
    }
}
