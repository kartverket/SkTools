package no.statkart.sktools.gradle.plugins.weblogic.wswar

import org.gradle.api.AntBuilder
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.compile.CompileOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.gradle.api.internal.tasks.compile.JavaCompiler
import org.gradle.api.tasks.util.PatternSet
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.BaseDirFileResolver

/**
 * Steg for kompilering av JAX-WS implementasjon for server.
 *
 * @author Leif Lislegård
 */
class WeblogicJaxWsCompiler implements JavaCompiler {

    private static Logger logger = LoggerFactory.getLogger(WeblogicJaxWsCompiler)

    private static final String WEBLOGIC_CLASSPATH_ID = "weblogic_classpath_id"
    private static final String JWSC_CLASSPATH_ID = "jwsc_classpath_id"

    FileCollection source;
    File destinationDir;
    Iterable<File> classpath;
    String sourceCompatibility;
    String targetCompatibility;
    CompileOptions compileOptions = new CompileOptions();

    Iterable<File> weblogicClasspath;
    File baseDir;
    AntBuilder ant;
    String warName;



    WeblogicJaxWsCompiler() {
        //setting defaults for this compiler.
        compileOptions.setFork(true)
        compileOptions.setListFiles(true)
        compileOptions.setVerbose(logger.isDebugEnabled())
        compileOptions.setFailOnError(true) //defaults to true
//        compileOptions.setCompiler('javac1.6')

        compileOptions.fieldName2AntMap().put("encoding", "srcEncoding")    //optional attributt som har et annet navn i weblogic task
    }

    void setDependencyCacheDir(File dir) {
        // don't care
    }



    WorkResult execute() {

        ant.setProperty('build.compiler', 'modern')
        if (getSourceCompatibility() != null) {
            ant.setProperty('ant.build.javac.source', getSourceCompatibility())
        }
        if (getTargetCompatibility() != null) {
            ant.setProperty('ant.build.javac.target', getTargetCompatibility())
        }

        createAntClassPath(ant, classpath, JWSC_CLASSPATH_ID)
        createAntClassPath(ant, weblogicClasspath, WEBLOGIC_CLASSPATH_ID)


        ant.taskdef(name: 'jwsc', classname: 'weblogic.wsee.tools.anttasks.JwscTask', classpathref: WEBLOGIC_CLASSPATH_ID)

        def attributes = [
                srcdir: baseDir,
                destdir: destinationDir,
                keepGenerated: true,
                classpathref: JWSC_CLASSPATH_ID,
                includeantruntime: false,
                destEncoding: 'UTF-8',
        ]

        attributes += compileOptions.optionMap()


        logger.info ('Calling jwsc with attributes = ' + attributes)

        def antTask = ant.jwsc(attributes) {
            //todo: context path skal kunne konfigureres (muligens ved deply/pakking av ear?)
            // - dette for å støtte deploymenter som ikke er exploded
            module(name: warName, contextpath: 'notimportantsincewethrowawaytheear', explode: true) {

                //todo: bør kunne parameteriseres?
                FileTree filteredSourceFiles = source.asFileTree.matching(new PatternSet(includes:['**/*WSBean.java']))
                BaseDirFileResolver resolver = new BaseDirFileResolver(baseDir)
                filteredSourceFiles.each {
                    String path = resolver.resolveAsRelativePath(it)
                    jws(file: path, generateWsdl: true, type: 'JAXWS') {
                        wlhttptransport()
                    }
                }

            }
        }

        return { destinationDir.list().size() > 0 } as WorkResult
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
