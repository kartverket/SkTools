package no.statkart.sktools.gradle.plugins.wsdocgen

import org.gradle.api.tasks.SourceTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.logging.LogLevel

/**
 *
 * @author Leif Lislegård
 */
class WsDocGenTask extends SourceTask {

    static final String ANT_CLASS_PATH_ID = 'class_path'


    @Input
    FileCollection classpath;

    @Input
    Collection<Group> groups;


    WsDocGenTask() {
        logging.captureStandardOutput LogLevel.INFO
        logging.captureStandardError LogLevel.DEBUG
    }


    @TaskAction
    def generate() {

        AntBuilder ant = getAnt()

        createAntClassPath(ant, getClasspath(), ANT_CLASS_PATH_ID)

        getGroups().each { Group group ->
            group.targetDir.mkdirs()

            def attributes = [
                    factory: "no.statkart.grunnbok.tools.docgen.ws.WebserviceAnnotationProcessorFactory",
                    destdir: group.targetDir,
                    compile: false,
                    debug: true,
                    includeantruntime: false,
                    classpathref: ANT_CLASS_PATH_ID,
                    sourcepath: "",
            ]

            getLogger().info ('Calling apt with attributes = ' + attributes)

            ant.apt(attributes) {
                getSource().addToAntBuilder(ant, 'src', FileCollection.AntType.MatchingTask)
                if (group.lookupPath) {
                    option(name: 'LookupPath', value: group.lookupPath)
                }
                group.includes.each {
                    include(name: it)
                }

            }
        }

    }


    @TaskAction
    def addStyleScheet() {

        getGroups().each { Group group ->
            group.targetDir.mkdirs()

            File targetFile = new File(group.targetDir, 'ws-style.css')
            targetFile.append(this.class.getResourceAsStream('ws-style.css'))
        }

    }



    private void createAntClassPath(AntBuilder ant, Iterable classpath, String id) {
        logger.info('Defining Ant classpath id={}', id)
        ant.path(id: id) {
            classpath.each {
                logger.info("\t{} += {}", id, it)
                pathelement(location: it)
            }
        }
    }

}
