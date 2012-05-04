package no.statkart.sktools.gradle.plugins.weblogic.wswar

import org.gradle.api.AntBuilder
import org.gradle.api.tasks.WorkResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.gradle.api.tasks.util.PatternSet
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.FileResolver
import no.statkart.sktools.gradle.plugins.weblogic.compile.DefaultWeblogicCompileSpec
import org.gradle.api.file.FileCollection

/**
 * Steg for kompilering av JAX-WS implementasjon for server.
 *
 * @author Leif Lislegård
 */
class WeblogicJaxWsCompiler implements org.gradle.api.internal.tasks.compile.Compiler<DefaultWeblogicCompileSpec>, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(WeblogicJaxWsCompiler.class)
    private static final String WEBLOGIC_CLASSPATH_ID = "weblogic_classpath_id"
    private static final String JWSC_CLASSPATH_ID = "jwsc_classpath_id"

    File baseDir;
    AntBuilder ant;
    String warName;
    FileResolver fileResolver;


    @Override
    WorkResult execute(DefaultWeblogicCompileSpec spec) {

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
        ]

        attributes += spec.compileOptions.optionMap()

        if (attributes.encoding) {
            attributes.srcEncoding = attributes.enocding
            attributes.remove('enocding')
        }


        logger.info ('Calling jwsc with attributes = ' + attributes)

        def antTask = ant.jwsc(attributes) {
            if (logger.isDebugEnabled()) {
                logger.debug("Adding 'sourcepath' -> \n${spec.source.asFileTree.files.join('\n')}")
            }
            spec.source.addToAntBuilder(ant, 'sourcepath', FileCollection.AntType.MatchingTask)

            //todo: context path skal kunne konfigureres (muligens ved deply/pakking av ear?)
            // - dette for å støtte deploymenter som ikke er exploded
            module(name: warName, contextpath: 'notimportantsincewethrowawaytheear', explode: true) {

                //todo: bør kunne parameteriseres?
                FileTree filteredSourceFiles = spec.source.asFileTree.matching(new PatternSet(includes:['**/*WSBean.java']))
                FileResolver resolver = fileResolver.withBaseDir(baseDir)
                filteredSourceFiles.each {
                    String path = resolver.resolveAsRelativePath(it)
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
