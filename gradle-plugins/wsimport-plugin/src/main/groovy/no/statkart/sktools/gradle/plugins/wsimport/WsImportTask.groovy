package no.statkart.sktools.gradle.plugins.wsimport

import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternSet

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Genererer java-kildekode for JAXWS stubber fra WSDL filer.
 */
class WsImportTask extends SourceTask {
    /**
     * Classpath som inneholder wsimport Ant-task
     */
    @Classpath
    FileCollection jaxwsClasspath

    @OutputDirectory
    File destinationDir

    /**
     * Angir hvilken pakke alle exceptions skal samles i. Gjør ingenting hvis ingenting er angitt.
     */
    private String packageOrPathString

    private PatternSet exceptionFilePatternSet = new PatternSet(includes: ['**/*Exception.java'])

    @Internal
    boolean verbose = false

    /**
     * Angir hvilken encoding som skal brukes for generert kildekode.
     * <p>
     * Bruker UTF-8 som standard fordi:
     * <ol>
     *     <li>UTF-8 er gyldig windows-1252, men windows-1252 er ikke gyldig UTF-8</li>
     *     <li>Den faktiske koden skal ikke inneholde ikke-ASCII-tegn siden vi unngår det i API-ene</li>
     * </ol>
     */
    @Input
    String encoding = StandardCharsets.UTF_8.name()

    /**
     * Angir hvilken WSDL som skal prosesseres sist. Dette må være den som trekker inn mest.
     */
    @Input
    @Optional
    String lastWsdl = null

    /**
     * Angir hvilken WSDL som skal prosesseres sist. Dette må være den som trekker inn mest.
     */
    void lastWsdl(String lastWsdl) {
        setLastWsdl(lastWsdl)
    }

    /**
     * Angir at exceptions skal samles i denne pakken fremfor å ligge i hver service-pakke.
     */
    void exceptionReusePackage(String packageOrPathString) {
        this.packageOrPathString  = packageOrPathString
    }

    @TaskAction
    protected void genServices() {
        ant.taskdef(name: 'wsimport', classname: 'com.sun.tools.ws.ant.WsImport', classpath: getJaxwsClasspath().getAsPath())
        getProject().delete(getDestinationDir())
        getProject().mkdir(getDestinationDir())

        def wsdls = getSource().matching { include '**/*.wsdl' }

        List<FileVisitDetails> last = []

        wsdls.visit { FileVisitDetails details ->
            if (!details.directory) {
                if (details.relativePath.toString().equals(lastWsdl)) {
                    last.add(details)
                } else {
                    wsimport(details)
                }
            }
        }

        last.each {
            wsimport(it)
        }

        if (packageOrPathString) {
            reuseExceptions(getDestinationDir())
        }
    }

    protected void wsimport(FileVisitDetails details) {
        ant.wsimport(wsdl: details.file, extension: 'true', destdir: getTemporaryDir(), sourcedestdir: getDestinationDir(), keep: 'true', xnocompile: 'true', wsdllocation: '/' + details.relativePath, verbose: verbose, encoding: encoding)
    }

    /**
     * Pakke exceptions skal flyttes til. Kan være {@code null} dersom de ikke skal flyttes.
     * @see #exceptionReusePackage(java.lang.String)
     */
    @Input
    @Optional
    String getPackageString() {
        return packageOrPathString?.replace('/', '.')?.replace('\\', '.')
    }

    /**
     * Samler alle exceptions for services til felles pakke.
     * Dette da vi ønsker at den genererte klientkoden skal gjenspeile strukturen til serveren, samt at man ønsker å gjenbruke exception klassene.
     */
    protected void reuseExceptions(File genSourceDir) {
        String packageString = getPackageString()
        //
        File exceptionPackageDir = new File(genSourceDir, packageString.replace((char) '.', File.separatorChar))

        FileCollection javaFiles = project.fileTree(dir: genSourceDir, includes: ['**/*.java'])

        exceptionPackageDir.mkdirs()

        //flytter alle exceptions til felles katalog
        javaFiles.matching(exceptionFilePatternSet).files.each { File file ->
            File relocatedFile = new File(exceptionPackageDir, file.getName())
            logger.info('merging exception {} <- {}', relocatedFile, file)

            Files.copy(file.toPath(), exceptionPackageDir.toPath().resolve(file.name), StandardCopyOption.REPLACE_EXISTING)
            file.delete()

            //kjører regexp replace på package statement for flyttet fil
            relocatedFile.write relocatedFile.getText(encoding).replaceFirst(/(?ms)package[^;]+/, "package " + packageString), encoding

        }

        //legger til import statements for de andre java filene
        javaFiles.files.each { File file ->
            logger.debug('adding exception import statement in {}', file)
            file.write file.getText(encoding).replaceFirst('import ', "import ${packageString}.*;\nimport "), encoding
        }


    }
}
