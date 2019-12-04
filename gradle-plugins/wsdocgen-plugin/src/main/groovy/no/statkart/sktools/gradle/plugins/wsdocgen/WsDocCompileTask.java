package no.statkart.sktools.gradle.plugins.wsdocgen;

import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.process.CommandLineArgumentProvider;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Leif Lislegård
 * @since 2.0
 */
public class WsDocCompileTask extends JavaCompile {
    private WsDocGroup docGroup;

    /**
     * Gradle 1.2/2.0 - no arg constructor or @Inject annotated constructor
     */
    @Inject
    public WsDocCompileTask(WsDocGroup docGroup) {
        super();
        setDocGroup(docGroup);

        getLogging().captureStandardError(LogLevel.LIFECYCLE);
        getLogging().captureStandardOutput(LogLevel.DEBUG);

        CompileOptions compileOptions = getOptions();
        compileOptions.setWarnings(false);

        compileOptions.getCompilerArgs().add("-proc:only"); //only annotation processing is done, without any subsequent compilation.
        compileOptions.getCompilerArgs().add("-processor");
        compileOptions.getCompilerArgs().add("no.statkart.sktools.utils.wsdocgen.processor.WSDocProcessor"); //Names of the annotation processors to run. This bypasses the default discovery process.

        compileOptions.setListFiles(getLogger().isDebugEnabled());
        compileOptions.setVerbose(getLogger().isInfoEnabled());

        compileOptions.getCompilerArgumentProviders().add(lazyCompilerArgs());
        initEncoding(compileOptions);

    }

    private void initEncoding(final CompileOptions options) {
        String encoding = getEncoding();
        if (encoding != null && !encoding.isEmpty()) {
            options.setEncoding(encoding);
        }
    }

    private CommandLineArgumentProvider lazyCompilerArgs() {
        return () -> {
            final List<String> compilerArgs = new ArrayList<>();

            final File xsl = getServiceXsltFile();
            if (!xsl.exists()) {
                throw new RuntimeException("xslt file not found: " + getProject().relativePath(xsl));
            }
            compilerArgs.add("-Axslt=" + xsl.getPath()); //xslt file

            if (getLookupPath() != null) {
                compilerArgs.add("-AjavaDocLookupPath=" + getLookupPath()); //lookup path
            }

            if (getIndexXsltFile() != null) {
                compilerArgs.add("-AindexXslt=" + getIndexXsltFile().getPath()); //SKTOOLS-105
            }

            return compilerArgs;
        };
    }


    @Optional
    @Input //not up to date when changed
    public String getLookupPath() {
        return getDocGroup().lookupPath;
    }

    @Optional
    @Input
    public String getEncoding() {
        return getDocGroup().encoding;
    }

    @Override
    public CompileOptions getOptions() {
        final CompileOptions options = super.getOptions();
        options.setListFiles(getLogger().isDebugEnabled());
        options.setVerbose(getLogger().isInfoEnabled());

        return options;
    }

    @InputFile
    public File getServiceXsltFile() {
        if (getDocGroup().serviceXsltPath != null) {
            return getProject().file(getDocGroup().serviceXsltPath);
        }
        return null;
    }

    @Optional
    @InputFile //file content aware
    File getIndexXsltFile() {
        if (getDocGroup().indexXsltPath != null) {
            return getProject().file(getDocGroup().indexXsltPath);
        } else {
            return null; //optional null
        }
    }

    private WsDocGroup getDocGroup() {
        return docGroup;
    }

    private void setDocGroup(WsDocGroup docGroup) {
        this.docGroup = docGroup;

        if (docGroup.includes != null) {
            setIncludes(docGroup.includes); //up to date affects getSource()
        }
    }

}
