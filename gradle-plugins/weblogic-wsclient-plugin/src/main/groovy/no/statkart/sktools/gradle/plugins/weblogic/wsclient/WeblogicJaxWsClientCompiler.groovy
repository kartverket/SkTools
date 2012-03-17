package no.statkart.sktools.gradle.plugins.weblogic.wsclient

import org.gradle.api.AntBuilder
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.tasks.compile.JavaCompiler
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.compile.CompileOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.gradle.api.internal.file.collections.SimpleFileCollection
import org.gradle.api.tasks.util.PatternSet
import org.apache.commons.io.FileUtils
import org.gradle.api.file.FileTree

/**
 * Steg for kompilering av JAX-WS stubber for klient basert på wsdl filer.
 *
 * @author Leif Lislegård
 */
class WeblogicJaxWsClientCompiler implements JavaCompiler {
    private static Logger logger = LoggerFactory.getLogger(WeblogicJaxWsClientCompiler)

    private static final String WEBLOGIC_CLASSPATH_ID = "weblogic_classpath_id"

    FileCollection source;
    File destinationDir;
    String sourceCompatibility;
    String targetCompatibility;
    CompileOptions compileOptions = new CompileOptions();

    Iterable<File> weblogicClasspath;
    AntBuilder ant;
    Collection<WebServiceConfig> webServices;

    //for overriding av package navn for genererte stubber
    String packageName;

    //todo: xmlcatalog


    WeblogicJaxWsClientCompiler() {
        //setting defaults for this compiler.
        compileOptions.setFork(true)
        compileOptions.setListFiles(true)
        compileOptions.setVerbose(logger.isDebugEnabled())
        compileOptions.setFailOnError(true) //defaults to true
        //compileOptions.setCompiler('javac1.6')
    }

    void setDependencyCacheDir(File dir) {
        // don't care
    }

    void setClasspath(Iterable<File> classpath) {
        // don't care - see weblogicClasspath instead
    }






    WorkResult execute() {

        ant.setProperty('build.compiler', 'modern')
        if (getSourceCompatibility() != null) {
            ant.setProperty('ant.build.javac.source', getSourceCompatibility())
        }
        if (getTargetCompatibility() != null) {
            ant.setProperty('ant.build.javac.target', getTargetCompatibility())
        }

        createAntClassPath(ant, weblogicClasspath, WEBLOGIC_CLASSPATH_ID)

        ant.taskdef(name: 'clientgen', classname: 'weblogic.wsee.tools.anttasks.ClientGenTask', classpathref: WEBLOGIC_CLASSPATH_ID)

        def attributes = [
                wsdl: null,
                destdir: destinationDir,
                type: 'JAXWS',
                includeantruntime: false,
        ]

        /**
         * Dersom {@code false} genereres META-INF/jax-ws-catalog.xml med relativ mapping til wsdl schema filer
         * Dersom {@code true} legges wsdl shema filer til META-INF/wsdls/*
         */
        attributes.copywsdl = true

        if (packageName != null) {
            attributes.packageName = packageName
        }

        //attributes.catalog //todo: teste ut denne

        attributes += compileOptions.optionMap()

        webServices.each { WebServiceConfig webService ->

            FileCollection oldFiles = new SimpleFileCollection(new SimpleFileCollection(destinationDir).getAsFileTree().getFiles())
            FileCollection newFiles = new SimpleFileCollection(destinationDir).minus(oldFiles)

            //genererer en og en webService modul..
            FileTree fileTree = webService.schemaFiles.getAsFileTree()

            //access all files in case of FileCollection that wraps an archive.
            // This wil trigger gradle to explode all the defined source to a temp directory
            fileTree.files

            FileCollection wsdls = fileTree.matching(new PatternSet(includes: ['**/*.wsdl'], caseSensitive:false)) //iterates over the WSDLs only
            wsdls.files.each {
                if (!it.exists()) {
                    logger.warn("wsdl input file ${it} does not exist!")
                }
                attributes.wsdl = it
                logger.info('Calling clientgen with attributes = ' + attributes)
                def result = ant.clientgen(attributes) {
                    //nested <fileset> fungerer ikke (testet for WLS 10.3.1), må angi en og en wsdl-fil
                }
            }

            if (webService.exception != null) {
                logger.info("Reusing exceptions for module ${webService}")
                reuseExceptions(destinationDir, newFiles, webService.exception, ant)
            }

        }

        return { destinationDir.list().size() > 0 } as WorkResult
    }

    protected static void reuseExceptions(File genSourceDir, FileCollection files, ExceptionConfig exceptionConfig, AntBuilder ant) {
        String packageString = exceptionConfig.packageOrPathString.replace('/', '.').replace('\\', '.')
        FileTree javaFiles = files.getAsFileTree().matching(new PatternSet(includes: ['**/*.java']))

        File exceptionPackageDir = new File(genSourceDir, packageString.replace((char) '.', File.separatorChar))
        exceptionPackageDir.mkdirs()

        //flytter alle exceptions til felles katalog
        javaFiles.matching(exceptionConfig.exceptionFilePatternSet).files.each { File file ->
            File relocatedFile = new File(exceptionPackageDir, file.getName())
            logger.debug("merging exception ${relocatedFile} <- ${file}")

            FileUtils.copyFileToDirectory(file, exceptionPackageDir)

            //kjører regexp replace på package statement for flyttet fil
            relocatedFile.text = relocatedFile.text.replaceFirst(/(?ms)package[^;]+/, "packgage " + packageString)

//            assert text ==~ /(?ms).*import\s+no\.statkart\.grunnbok\.skif\.util\.ListIterable;.*/ //(?ms) matches regex over multiple lines.

        }

        //legger til import statements for de andre java filene.
        javaFiles.minus(new SimpleFileCollection(exceptionPackageDir)).files.each { File file ->
            logger.debug("adding exception import statement in ${file}")
            file.text = "import ${packageString}.*;\n${file.text}"
        }


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

