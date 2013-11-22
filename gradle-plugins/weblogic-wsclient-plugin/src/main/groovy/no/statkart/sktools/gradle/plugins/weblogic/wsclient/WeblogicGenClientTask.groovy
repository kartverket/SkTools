package no.statkart.sktools.gradle.plugins.weblogic.wsclient

import no.statkart.sktools.gradle.plugins.weblogic.WeblogicTaskInterface
import org.apache.commons.io.FileUtils
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.CompileOptions

/**
 * Task for generering av weblogic webservice klient
 *
 *
 * @since 1.1
 * @author Leif Lislegård
 */
class WeblogicGenClientTask extends AbstractCompile implements WeblogicTaskInterface {
    private static final String WEBLOGIC_CLASSPATH_ID = "weblogic_classpath_id"
    private static final String WEBLOGIC_WSCLIENT_CLASSPATH_ID = "weblogic_wsclient_classpath_id"

    WebServiceConfig webServiceConfig;

    String packageName;

    private final CompileOptions compileOptions = new CompileOptions();

    private FileCollection weblogicClasspath;
    private File dependencyCacheDir;

    WeblogicGenClientTask() {
        logging.captureStandardOutput LogLevel.INFO
        logging.captureStandardError LogLevel.DEBUG

        getOptions().setFork(true)
        getOptions().setListFiles(true)
        getOptions().setVerbose(logger.isDebugEnabled())
        getOptions().setFailOnError(true) //defaults to true

    }

    @TaskAction
    protected void compile() {
        //clean resources
        project.delete(getDestinationDir())

        gen();

        fixResourceLoaders() // Som egen @TaskAction virker det ikke av en eller annen grunn
        deleteTemporaryFiles()
    }

    void gen() {
        org.gradle.api.AntBuilder ant = project.createAntBuilder()

        ant.setProperty('build.compiler', 'modern')
        if (sourceCompatibility != null) {
            ant.setProperty('ant.build.javac.source', sourceCompatibility)
        }
        if (targetCompatibility != null) {
            ant.setProperty('ant.build.javac.target', targetCompatibility)
        }

        createAntClassPath(ant, getWeblogicClasspath(), WEBLOGIC_CLASSPATH_ID)
        createAntClassPath(ant, getClasspath(), WEBLOGIC_WSCLIENT_CLASSPATH_ID)


        ant.taskdef(name: 'clientgen', classname: 'weblogic.wsee.tools.anttasks.ClientGenTask', classpathref: WEBLOGIC_CLASSPATH_ID)

        def attributes = [
                wsdl: null,
                destdir: getDestinationDir(),
                type: 'JAXWS',
                includeantruntime: false,
                tempdir: temporaryDir
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

        attributes += compileOptions.optionMap()

        if (attributes.encoding) {
            attributes.srcEncoding = attributes.encoding
            attributes.destEncoding = attributes.encoding
            attributes.remove('encoding')
        }
        attributes.fork = false

        File lastFile = null
        source.matching {include '**/*.wsdl'}.files.each { File f ->
            if (webServiceConfig.lastWsdl == f.name) {
                lastFile = f
            } else {
                attributes.wsdl = f
                logger.info('Calling clientgen with attributes = ' + attributes)
                def result = ant.clientgen(attributes) {
                    //nested <fileset> fungerer ikke (testet for WLS 10.3.1), må angi en og en wsdl-fil
                }
            }
        }
        if (lastFile != null) {
            attributes.wsdl = lastFile
            logger.info('Calling clientgen last with attributes = ' + attributes)
            def result = ant.clientgen(attributes) {
                //nested <fileset> fungerer ikke (testet for WLS 10.3.1), må angi en og en wsdl-fil
            }
        }

        if (webServiceConfig.exception != null) {
            logger.info("Reusing exceptions for module ${webServiceConfig}")
            reuseExceptions(getDestinationDir(), webServiceConfig.exception)
        }
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

    private void createAntClassPath(org.gradle.api.AntBuilder ant, Iterable classpath, String id) {
        logger.debug('Defining Ant classpath id={}', id)
        ant.path(id: id) {
            classpath.each {
                logger.debug("\t{} += {}", id, it)
                pathelement(location: it)
            }
        }
    }

    /**
     * Action som retter loading av wsdl filer ifra webstart klienter osv.
     * Rettinger blir påført i klikdekoden.
     */
//    @TaskAction
    protected void fixResourceLoaders() {
        ant.replaceregexp {
            regexp(pattern: /URL baseUrl;[^=]+\s(.*getResource).*;[^=]*.*baseUrl, "(.*)".*;([^{]*)MalformedURL/)
            substitution(expression: ('url \\1("/\\2");\\3'))
            fileset(dir: getDestinationDir(), erroronmissingdir: true) {
                include(name: '**/*.java')
            }
        }
    }

    /**
     * Det etterlates noen midlertidige filer i output-katalogen. Disse slettes automatisk (kanskje når Java-prosessen
     * er ferdig), men innen da har Gradle sett dem og registrert dem som output-filer. Siden de er vekk ved neste
     * kjøring anser Gradle tasken som ikke up-to-date.
     */
    protected void deleteTemporaryFiles() {
        def dontWantThese = project.fileTree(dir: getDestinationDir(), include: 'META-INF/wsdls/*', exclude: 'META-INF/wsdls/*.*')
        dontWantThese.files.each { it.delete() }
    }


    @Optional
    @OutputDirectory
    public File getDependencyCacheDir() {
        return dependencyCacheDir;
    }

    public void setDependencyCacheDir(File dependencyCacheDir) {
        this.dependencyCacheDir = dependencyCacheDir;
    }

    /**
     * Returns the compilation options.
     *
     * @return The compilation options.
     */
    @Nested
    public CompileOptions getOptions() {
        return this.compileOptions;
    }

    public void setWeblogicClasspath(FileCollection weblogicClasspath) {
        this.weblogicClasspath = weblogicClasspath;
    }

    @InputFiles
    public FileCollection getWeblogicClasspath() {
        return weblogicClasspath;
    }

    @InputFiles
    @Optional
    FileCollection getClasspath() { //markerer denne som optional
        return super.getClasspath() ? super.getClasspath() : getProject().files()
    }


}
