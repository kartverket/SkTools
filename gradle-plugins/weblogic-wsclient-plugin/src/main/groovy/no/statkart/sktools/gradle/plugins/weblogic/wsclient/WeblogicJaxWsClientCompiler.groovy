package no.statkart.sktools.gradle.plugins.weblogic.wsclient

import org.gradle.api.AntBuilder
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.WorkResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.gradle.api.internal.file.collections.SimpleFileCollection
import org.gradle.api.tasks.util.PatternSet
import org.apache.commons.io.FileUtils
import org.gradle.api.file.FileTree
import no.statkart.sktools.gradle.plugins.weblogic.compile.DefaultWeblogicCompileSpec

/**
 * Steg for kodegenerering av JAX-WS stubber for klienter basert på wsdl filer.
 *
 * NB: For Weblogic 10.3.5 biblioteker (eller nyere?) så må tools.jar ligge med på classpath
 *
 * @author Leif Lislegård
 * @author Tor Egil R. Strand
 */
class WeblogicJaxWsClientCompiler implements org.gradle.api.internal.tasks.compile.Compiler<DefaultWeblogicCompileSpec>, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(WeblogicJaxWsClientCompiler.class)
    private static final String WEBLOGIC_CLASSPATH_ID = "weblogic_classpath_id"
    private static final String WEBLOGIC_WSCLIENT_CLASSPATH_ID = "weblogic_wsclient_classpath_id"

    Project project;
    WebServiceConfig webService;

    //for overriding av package navn for genererte stubber
    String packageName;

    //todo: xmlcatalog



    @Override
    WorkResult execute(DefaultWeblogicCompileSpec spec) {
        AntBuilder ant = project.createAntBuilder()

        ant.setProperty('build.compiler', 'modern')
        if (spec.sourceCompatibility != null) {
            ant.setProperty('ant.build.javac.source', spec.sourceCompatibility)
        }
        if (spec.targetCompatibility != null) {
            ant.setProperty('ant.build.javac.target', spec.targetCompatibility)
        }

        createAntClassPath(ant, spec.weblogicClasspath, WEBLOGIC_CLASSPATH_ID)
        createAntClassPath(ant, spec.classpath, WEBLOGIC_WSCLIENT_CLASSPATH_ID)


        ant.taskdef(name: 'clientgen', classname: 'weblogic.wsee.tools.anttasks.ClientGenTask', classpathref: WEBLOGIC_CLASSPATH_ID)

        def attributes = [
                wsdl: null,
                destdir: spec.destinationDir,
                type: 'JAXWS',
                includeantruntime: false,
                tempdir: spec.getTempDir()
                // kodegenerering avhenger kun av providede weblogic klasser. Definerer derfor ingen eksplisitt classpath.
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

        attributes += spec.compileOptions.optionMap()

        if (attributes.encoding) {
            attributes.srcEncoding = attributes.encoding
            attributes.destEncoding = attributes.encoding
            attributes.remove('encoding')
        }
        attributes.fork = false

        spec.source.files.each {
            attributes.wsdl = it
            logger.info('Calling clientgen with attributes = ' + attributes)
            def result = ant.clientgen(attributes) {
                //nested <fileset> fungerer ikke (testet for WLS 10.3.1), må angi en og en wsdl-fil
            }
        }

        if (webService.exception != null) {
            logger.info("Reusing exceptions for module ${webService}")
            reuseExceptions(spec.destinationDir, webService.exception)
        }

        return { spec.destinationDir.list().size() > 0 } as WorkResult
    }

    /**
     * Samler alle exceptions for services til felles pakke.
     * Dette da vi ønsker at den genererte klientkoden skal gjenspeile strukturen til serveren, samt at man ønsker å gjenbruke exception klassene.
     */
    protected void reuseExceptions(File genSourceDir, ExceptionConfig exceptionConfig) {
        String packageString = exceptionConfig.packageOrPathString.replace('/', '.').replace('\\', '.')
//
        File exceptionPackageDir = new File(genSourceDir, packageString.replace((char) '.', File.separatorChar))

        FileCollection javaFiles = project.fileTree(dir: genSourceDir, includes: ['**/*.java'])

        exceptionPackageDir.mkdirs()

        //flytter alle exceptions til felles katalog
        javaFiles.matching(exceptionConfig.exceptionFilePatternSet).files.each { File file ->
            File relocatedFile = new File(exceptionPackageDir, file.getName())
            logger.info("merging exception ${relocatedFile} <- ${file}")

            FileUtils.copyFileToDirectory(file, exceptionPackageDir)
            file.delete()

            //kjører regexp replace på package statement for flyttet fil
            relocatedFile.text = relocatedFile.text.replaceFirst(/(?ms)package[^;]+/, "package " + packageString)

        }

        //legger til import statements for de andre java filene
        javaFiles.files.each { File file ->
            logger.debug("adding exception import statement in ${file}")
            file.text = file.text.replaceFirst('import ', "import ${packageString}.*;\nimport ")
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

