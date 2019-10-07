package no.statkart.sktools.gradle.plugins.weblogic.wswar

import no.statkart.sktools.gradle.plugins.weblogic.compile.WeblogicCompileSpec
import org.gradle.api.AntBuilder
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.util.PatternSet

import java.nio.file.Path

/**
 * Steg for kompilering av JAX-WS implementasjon for server.
 *
 * @author Leif Lislegård
 */
class WeblogicJaxWsCompiler implements no.statkart.sktools.gradle.plugins.weblogic.compile.Compiler<WeblogicCompileSpec> {
    protected static Logger logger = Logging.getLogger(WeblogicJaxWsCompiler.class)
    static final String WEBLOGIC_CLASSPATH_ID = "weblogic_classpath_id"
    static final String JWSC_CLASSPATH_ID = "jwsc_classpath_id"

    File baseDir;
    AntBuilder ant;
    String warName;


    @Override
    WorkResult execute(WeblogicCompileSpec spec) {

        createAntClassPath(ant, spec.classpath, JWSC_CLASSPATH_ID)
        createAntClassPath(ant, spec.weblogicClasspath, WEBLOGIC_CLASSPATH_ID)


        ant.taskdef(name: 'jwsc', classname: 'weblogic.wsee.tools.anttasks.JwscTask', classpathref: WEBLOGIC_CLASSPATH_ID)

        def attributes = [
                srcdir: baseDir,
                destdir: spec.destinationDir,
                keepGenerated: true,
                classpathref: JWSC_CLASSPATH_ID,
                includeantruntime: false,
                destEncoding: 'UTF-8',
                tempdir: spec.getTempDir()
        ]

        attributes += spec.compileOptions.optionMap()

        if (attributes.encoding) {
            attributes.srcEncoding = attributes.encoding
            attributes.remove('encoding')
        }


        logger.info('Calling jwsc with attributes = {}', attributes)

        def antTask = ant.jwsc(attributes) {

            //todo: context path skal kunne konfigureres (muligens ved deply/pakking av ear?)
            // - dette for å støtte deploymenter som ikke er exploded
            module(name: warName, contextpath: 'notimportantsincewethrowawaytheear', explode: true) {

                //todo: bør kunne parameteriseres?
                FileTree filteredSourceFiles = spec.source.asFileTree.matching(new PatternSet(includes:['**/*WSBean.java']))
                Path base = baseDir.toPath()
                filteredSourceFiles.each { File file ->
                    String path = base.relativize(file.toPath()).toString() //relative path from base
                    jws(file: path, generateWsdl: true, type: 'JAXWS') {
                        wlhttptransport()
                    }
                }

            }
        }

        return { spec.destinationDir.list().size() > 0 } as WorkResult
    }

    private void createAntClassPath(AntBuilder ant, Iterable classpath, String id) {
        logger.debug('Defining Ant classpath id={}', id)
        ant.path(id: id) {
            classpath.each {
                logger.debug("\t{} += {}", id, it)
                pathelement(location: it)
            }
        }
    }



}
