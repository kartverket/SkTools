package no.statkart.sktools.gradle.plugins.weblogic.wsclient.internal

import no.statkart.sktools.gradle.plugins.weblogic.compile.CompileOptions
import no.statkart.sktools.gradle.plugins.weblogic.wsclient.ExceptionConfig
import no.statkart.sktools.gradle.plugins.weblogic.wsclient.WebServiceConfig
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Logikk for generering av wsClient
 */
class WsClientGenerator {
    private static final String WEBLOGIC_CLASSPATH_ID = "weblogic_classpath_id"
    private static final String WEBLOGIC_WSCLIENT_CLASSPATH_ID = "weblogic_wsclient_classpath_id"

    final Logger logger = Logging.getLogger(WsClientGenerator.class);
    final Project project;
    final File destinationDir, temporaryDir;
    final FileTree source;
    final FileCollection classpath, weblogicClasspath;

    //optional attributes...
    public String sourceCompatibility;
    public String targetCompatibility;
    public String packageName;


    public WsClientGenerator(Project project, File destinationDir, File temporaryDir, FileTree source, FileCollection classpath, FileCollection weblogicClasspath) {
        this.project = project
        this.destinationDir = destinationDir
        this.temporaryDir = temporaryDir
        this.source = source
        this.classpath = classpath
        this.weblogicClasspath = weblogicClasspath
    }

    public void gen(CompileOptions compileOptions, WebServiceConfig webServiceConfig) {
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
                destDir: getDestinationDir(),
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
            //encoding attributt i Weblogic 11g, 12c
            // ikke tilgjengelig i Weblogic 9.x
        }
        attributes.fork = false

        //SKTOOLS-94: lastWsdl for workaround der wsclient kun genererer delsett av refererte domeneklasser
        source.matching {include '**/*.wsdl'}.files.split { webServiceConfig.getLastWsdl() != it.name }.each { Collection groups ->
            groups.each { File f ->
                attributes['wsdl'] = f

                if (webServiceConfig.apiPrefix != null) {
                    attributes.wsdlLocation = 'META-INF/wsdls/' + webServiceConfig.apiPrefix + '/' + f.name
                }

                logger.info('Calling clientgen with attributes = ' + attributes)
                def result = ant.clientgen(attributes) {
                    //nested <fileset> fungerer ikke (testet for WLS 10.3.1), må angi en og en wsdl-fil
                }
            }
        }

        if (webServiceConfig.getException() != null) {
            logger.info("Reusing exceptions for module ${webServiceConfig}")
            reuseExceptions(getDestinationDir(), webServiceConfig.getException())
        }
    }


    /**
     * Retter loading av wsdl filer ifra webstart klienter osv.
     * Rettinger blir påført i kildekoden.
     */
    void fixResourceLoaders() {
        project.getAnt().replaceregexp {
            regexp(pattern: /URL baseUrl;[^=]+\s(.*getResource).*;[^=]*.*baseUrl, "(.*)".*;([^{]*)MalformedURL/)
            substitution(expression: ('url \\1("/\\2");\\3'))
            fileset(dir: getDestinationDir(), erroronmissingdir: true) {
                include(name: '**/*.java')
            }
        }
        project.getAnt().replaceregexp {
            // Fra: url = new URL(<WS-Stub>.class.getResource("."), "META-INF/wsdls/<wsdlname>.wsdl");
            // Til: url = <WS-Stub>.class.getResource("/META-INF/wsdls/<wsdlname>.wsdl"); if (url == null) throw new MalformedURLException("Not found");
            regexp(pattern: /url = new URL\(([^"]+)"\."\), "(.*)"\);/)
            substitution(expression: ('url = \\1"/\\2"); if (url == null) throw new MalformedURLException("Not found");'))
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
    void deleteTemporaryFiles() {
        def dontWantThese = project.fileTree(dir: getDestinationDir(), include: 'META-INF/wsdls/*', exclude: 'META-INF/wsdls/*.*')
        dontWantThese.files.each { it.delete() }
    }


    /**
     * Samler alle exceptions for services til felles pakke.
     * Dette da vi ønsker at den genererte klientkoden skal gjenspeile strukturen til serveren, samt at man ønsker å gjenbruke exception klassene.
     */
    protected void reuseExceptions(File genSourceDir, ExceptionConfig exceptionConfig) {
        String packageString = exceptionConfig.getPackageString()
        //
        File exceptionPackageDir = new File(genSourceDir, packageString.replace((char) '.', File.separatorChar))

        FileCollection javaFiles = project.fileTree(dir: genSourceDir, includes: ['**/*.java'])

        exceptionPackageDir.mkdirs()

        //flytter alle exceptions til felles katalog
        javaFiles.matching(exceptionConfig.getExceptionFilePatternSet()).files.each { File file ->
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

}
