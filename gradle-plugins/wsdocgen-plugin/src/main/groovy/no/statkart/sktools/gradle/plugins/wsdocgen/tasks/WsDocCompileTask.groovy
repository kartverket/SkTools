package no.statkart.sktools.gradle.plugins.wsdocgen.tasks

import no.statkart.sktools.gradle.plugins.wsdocgen.Group
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.compile.CompileOptions
import org.gradle.api.tasks.compile.JavaCompile
import no.statkart.sktools.gradle.plugins.wsdocgen.WsDocGenPlugin

/**
 * @since 1.3
 * @author Leif Lislegård
 */
class WsDocCompileTask extends JavaCompile {


    @Input
    Group docGroup;

    @InputFile //not up to date when change in file
    File getServiceXsltFile() {
        if (getDocGroup().serviceXsltPath) {
            return project.file(getDocGroup().serviceXsltPath)
        } else {
            logger.warn("WARNING: no xslt file specified - using template for TESTING purposes..")
            return generateTestFile(new File(project.buildDir, "Transform.xsl")) //can't write to output dir because it gets wiped when not up to date...
        }
    }

    @Optional
    @InputFile //not up to date when change in file
    File getIndexXsltFile() {
        if (getDocGroup().indexXsltPath) {
            return project.file(getDocGroup().indexXsltPath)
        } else {
            return null
        }
    }

    private File generateTestFile(File testFile) {
        testFile.getParentFile().mkdirs()
        testFile.createNewFile()

        testFile.withWriter { def writer ->
            getClass().getResourceAsStream("DefaultTransform.xsl").withReader() {
                it.readLines().each { writer.write(it); writer.write("\n") }
            }
            writer.flush()
        }
        return testFile
    }

    WsDocCompileTask() {
        logging.captureStandardOutput LogLevel.INFO
        logging.captureStandardError LogLevel.ERROR

        options.compilerArgs = [
                "-proc:only",
                "-processor", "no.statkart.sktools.utils.wsdocgen.processor.WSDocProcessor",
        ]

        final FileCollection processorClasspath = WsDocGenPlugin.findPluginClasspath(project)
        if (processorClasspath != null) {
            options.compilerArgs << "-processorpath"
            options.compilerArgs << processorClasspath.asFileTree.asPath
        } else {
            //ok under testing
        }


    }


    @Override
    CompileOptions getOptions() {
        final CompileOptions options = super.getOptions()
        options.setListFiles(logger.isDebugEnabled())
        options.setVerbose(logger.isInfoEnabled())

        return options
    }


    @Override
    protected void compile() {

        final File xsl = getServiceXsltFile()
        if (!xsl.exists()) {
            throw new TaskExecutionException("xslt file not found: ${project.relativePath(xsl)}");
        }

        options.compilerArgs += [
                "-Axslt=${xsl}", //xslt file
        ]

        if (getDocGroup().lookupPath) {
            options.compilerArgs << "-AjavaDocLookupPath=${getDocGroup().lookupPath}" //lookup path
        }

        if (indexXsltFile) {
            options.compilerArgs << "-AindexXslt=${indexXsltFile}" //SKTOOLS-105
        }

        getDocGroup().includes.each {
            include(it)
        }

        super.compile()
    }
}
