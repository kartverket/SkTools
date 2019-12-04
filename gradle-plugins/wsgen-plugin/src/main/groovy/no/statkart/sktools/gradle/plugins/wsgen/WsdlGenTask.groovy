package no.statkart.sktools.gradle.plugins.wsgen

import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

public class WsdlGenTask extends SourceTask {
    @CompileClasspath
    FileCollection classpath;

    @CompileClasspath
    FileCollection jaxwsClasspath;

    @OutputDirectory
    File destinationDir;

    @TaskAction
    protected void genWsdl() {
        ant.taskdef(name: 'wsgen', classname: 'com.sun.tools.ws.ant.WsGen', classpath: getJaxwsClasspath().getAsPath())
        getProject().delete(getDestinationDir())
        getProject().mkdir(getDestinationDir())

        def wsBeans = getSource().matching { include '**/*WSBean.class' }

        def classpathRef = getPath().replace(':' as char, '_' as char)
        createAntClassPath(getClasspath(), classpathRef)

        wsBeans.visit { FileVisitDetails details ->
            if (!details.directory) {
                String path = details.getRelativePath()
                String classname = path.substring(0, path.length() - 6) //removing '.class'
                    .replace('/' as char, '.' as char)
                ant.wsgen(
                        sei: classname,
                        classpathref: classpathRef,
                        destdir: getTemporaryDir(),
                        resourcedestdir: getDestinationDir(),
                        genwsdl: 'true',
                        encoding: 'UTF-8',
                        keep: 'true',
                        xnocompile: 'true',
                        fork: 'true',
                )
            }
        }
    }

    private void createAntClassPath(Iterable classpath, String id) {
        getLogger().debug('Defining Ant classpath id={}', id)
        getAnt().path(id: id) {
            classpath.each {
                getLogger().debug('\t{} += {}', id, it)
                pathelement(location: it)
            }
        }
    }

}
