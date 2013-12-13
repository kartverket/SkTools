package no.statkart.sktools.gradle.plugins.wsdocgen.tasks

import no.statkart.sktools.gradle.plugins.wsdocgen.Group
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.compile.CompileOptions
import org.gradle.api.tasks.compile.JavaCompile

/**
 * @since 1.3
 * @author Leif Lislegård
 */
class WsDocCompileTask extends JavaCompile {


    @Input
    Group docGroup;

    @Input
    File getXsltFile() {
        if (getDocGroup().xsltFile) {
            return getDocGroup().xsltFile
        } else {
            logger.warn("WARNING: no xslt file specified - using template for TESTING purposes..")
            return generateTestFile(new File(getDestinationDir(), "Transform.xsl"))
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
//        logging.captureStandardOutput LogLevel.INFO
//        logging.captureStandardError LogLevel.DEBUG

        options.compilerArgs = [
                "-proc:only",
                "-processor", "no.statkart.sktools.utils.wsdocgen.processor.WSDocProcessor",
        ]

    }


    @Override
    CompileOptions getOptions() {
        final CompileOptions options = super.getOptions()
        options.setVerbose(true)

        return options
    }


    @Override
    protected void compile() {

        final File xsl = getXsltFile()
        if (!xsl.exists()) {
            throw new TaskExecutionException("xslt file not found: ${project.relativePath(xsl)}");
        }

        options.compilerArgs += [
                "-Axslt=${xsl}", //xslt file
        ]

        if (getDocGroup().lookupPath) {
            options.compilerArgs << "-AjavaDocLookupPath=${getDocGroup().lookupPath}" //lookup path
        }

        getDocGroup().includes.each {
            include(it)
        }

        super.compile()
    }
}
