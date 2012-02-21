package no.statkart.sktools.gradle.plugins.xjc

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction

/**
 * todo: dokumentasjon
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class XjcTask extends DefaultTask {
    XjcPluginConvention convention = getProject().convention.plugins.xjc
    List schemas = convention.schemas
    String targetDirectory = convention.targetDirectory

    XjcTask() {
        Project project = getProject()
        project.afterEvaluate {
            schemas.each { s -> inputs.dir(s.dir) }
            if (!project.sourceSets.main.java.srcDirs.contains(targetDirectory)) {
                project.sourceSets.main.java.srcDirs += targetDirectory
            }
            outputs.dir(targetDirectory)
        }
    }

    @TaskAction
    def generate() {
        if (schemas.isEmpty()) {
            throw new GradleException("No schemas defined for xjc generation: statKartXjc.schema(dir, includes)")
        }
        FileCollection libraries = project.configurations[XjcPlugin.JAXB_CONFIGURATION_NAME]
        ant.taskdef(name: 'xjc', classname: 'com.sun.tools.xjc.XJCTask', classpath: libraries.asPath)
        ant.mkdir(dir: targetDirectory)
        schemas.each { s ->
            ant.xjc(destDir: targetDirectory, extension: s.withGrunnbokDoc != null) {
                if (s.withGrunnbokDoc) {
                    arg(line: "-grunnbokDoc $s.withGrunnbokDoc")
                }
                if (s.withListAdapter) {
                    arg(line: '-listgen')
                }
                schema(dir: s.dir, includes: s.includes)
            }
        }
    }
}
