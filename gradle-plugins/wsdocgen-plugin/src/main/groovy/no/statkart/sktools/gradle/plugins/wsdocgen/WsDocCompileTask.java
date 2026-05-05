package no.statkart.sktools.gradle.plugins.wsdocgen;

import org.gradle.api.logging.LogLevel;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
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
public abstract class WsDocCompileTask extends JavaCompile {
    private final Property<String> lookupPath;
    private final Property<String> encoding;

    private final Property<File> serviceXsltPath;
    private final Property<File> indexXsltPath;

    /**
     * Gradle 1.2/2.0 - no arg constructor or @Inject annotated constructor
     */
    @Inject
    public WsDocCompileTask(ObjectFactory objectFactory) {
        lookupPath = objectFactory.property(String.class);
        encoding = objectFactory.property(String.class);
        serviceXsltPath = objectFactory.property(File.class);
        indexXsltPath = objectFactory.property(File.class);

        getLogging().captureStandardError(LogLevel.LIFECYCLE);
        getLogging().captureStandardOutput(LogLevel.DEBUG);

        CompileOptions compileOptions = getOptions();
        compileOptions.setWarnings(false);

        compileOptions.getCompilerArgs().add("-proc:only"); //only annotation processing is done, without any subsequent compilation.
        compileOptions.getCompilerArgs().add("-processor");
        compileOptions.getCompilerArgs().add("no.statkart.sktools.utils.wsdocgen.processor.WSDocProcessor"); //Names of the annotation processors to run. This bypasses the default discovery process.

        configureDynamicCompileOptions();

        compileOptions.getCompilerArgumentProviders().add(lazyCompilerArgs());
        doFirst(task -> configureDynamicCompileOptions());
    }

    @SuppressWarnings("Convert2Lambda")
    private CommandLineArgumentProvider lazyCompilerArgs() {
        return new CommandLineArgumentProvider() {
            @Override
            public Iterable<String> asArguments() {
                final List<String> compilerArgs = new ArrayList<>(3);

                final File xsl = getServiceXsltFile().get();
                if (!xsl.exists()) {
                    throw new RuntimeException("xslt file not found: " + getProject().relativePath(xsl));
                }
                compilerArgs.add("-Axslt=" + xsl.getPath()); //xslt file

                if (getLookupPath().isPresent()) {
                    compilerArgs.add("-AjavaDocLookupPath=" + getLookupPath().get()); //lookup path
                }

                if (getIndexXsltFile().isPresent()) {
                    compilerArgs.add("-AindexXslt=" + getIndexXsltFile().get().getPath()); //SKTOOLS-105
                }

                return compilerArgs;
            }
        };
    }

    @Optional
    @Input //not up to date when changed
    public Property<String> getLookupPath() {
        return lookupPath;
    }

    @Optional
    @Input
    public Property<String> getEncoding() {
        return encoding;
    }

    private void configureDynamicCompileOptions() {
        final CompileOptions options = getOptions();
        options.setListFiles(getLogger().isDebugEnabled());
        options.setVerbose(getLogger().isInfoEnabled());
        options.setEncoding(getEncoding().getOrNull());
    }

    @InputFile
    public Property<File> getServiceXsltFile() {
        return serviceXsltPath;
    }

    @Optional
    @InputFile //file content aware
    public Property<File> getIndexXsltFile() {
        return indexXsltPath;
    }

}
