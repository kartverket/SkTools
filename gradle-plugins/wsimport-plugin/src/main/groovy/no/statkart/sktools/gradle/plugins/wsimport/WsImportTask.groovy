package no.statkart.sktools.gradle.plugins.wsimport

import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternSet

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

public class WsImportTask extends SourceTask {
    FileCollection jaxwsClasspath;

    @OutputDirectory
    File destinationDir;

    String packageOrPathString

    private PatternSet exceptionFilePatternSet = new PatternSet(includes: ['**/*Exception.java'])

    boolean verbose = false;

    // Bruker UTF-8 som standard fordi: 1) UTF-8 er gyldig windows-1252, men windows-1252 er ikke gyldig UTF-8; 2) Den faktiske koden skal ikke innholde ikke-ASCII-tegn, kun eventuelt kommentarer
    String encoding = StandardCharsets.UTF_8.name();

    String lastWsdl = null;

    /**
     * Angir hvilken WSDL som skal prosesseres sist. Dette må være den som trekker inn mest.
     */
    public void lastWsdl(String lastWsdl) {
        setLastWsdl(lastWsdl)
    }

    /**
     * Angir at exceptions skal samles i denne pakken fremfor å ligge i hver service-pakke.
     */
    public void exceptionReusePackage(String packageOrPathString) {
        setPackageOrPathString(packageOrPathString)
    }

    @TaskAction
    protected void genServices() {
        ant.taskdef(name: 'wsimport', classname: 'com.sun.tools.ws.ant.WsImport', classpath: getJaxwsClasspath().getAsPath())
        ant.delete(dir: getDestinationDir(), quiet: 'true')
        ant.mkdir(dir: getDestinationDir())

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

    String getPackageString() {
        return packageOrPathString?.replace('/', '.')?.replace('\\', '.')
    }

    PatternSet getExceptionFilePatternSet() {
        return exceptionFilePatternSet
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
        javaFiles.matching(getExceptionFilePatternSet()).files.each { File file ->
            File relocatedFile = new File(exceptionPackageDir, file.getName())
            logger.info("merging exception ${relocatedFile} <- ${file}")

            Files.copy(file.toPath(), exceptionPackageDir.toPath().resolve(file.name), StandardCopyOption.REPLACE_EXISTING)
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
}
