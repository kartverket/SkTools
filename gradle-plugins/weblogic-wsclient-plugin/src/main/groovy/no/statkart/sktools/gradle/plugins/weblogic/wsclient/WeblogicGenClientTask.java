package no.statkart.sktools.gradle.plugins.weblogic.wsclient;

import no.statkart.sktools.gradle.plugins.weblogic.compile.DefaultWeblogicCompileSpec;
import no.statkart.sktools.gradle.plugins.weblogic.compile.WeblogicCompileSpec;
import no.statkart.sktools.gradle.plugins.weblogic.WeblogicTaskInterface;
import no.statkart.sktools.gradle.plugins.weblogic.wsclient.internal.WsClientGenerator;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.CompileOptions;

/**
 * Task for generering av weblogic webservice klient
 *
 *
 * @since 1.1
 * @author Leif Lislegård
 */
class WeblogicGenClientTask extends AbstractCompile implements WeblogicTaskInterface {
    protected static final Logger logger = Logging.getLogger(WeblogicGenClientTask.class);

    private WebServiceConfig webServiceConfig;
    private final DefaultWeblogicCompileSpec spec = new DefaultWeblogicCompileSpec();

    private FileCollection weblogicClasspath;
    private String packageName;


    /**
     * Gradle 1.2/2.0 - no arg constructor or @Inject annotated constructor
     */
    public WeblogicGenClientTask() {
        super();
        getLogging().captureStandardOutput(LogLevel.INFO);
        getLogging().captureStandardError(LogLevel.DEBUG);

        getOptions().setFork(true);
        getOptions().setListFiles(true);
        getOptions().setVerbose(logger.isDebugEnabled());
        getOptions().setFailOnError(true); //defaults to true

    }

    @TaskAction
    protected void compile() {
        //clean resources
        getProject().delete(getDestinationDir());

        spec.setWeblogicClasspath(getWeblogicClasspath().getFiles());
        spec.setTempDir(getTemporaryDir());

        spec.setDestinationDir(getDestinationDir());
        spec.setSource(getSource());
        spec.setClasspath(getClasspath());

        gen(spec);
    }


    void gen(WeblogicCompileSpec spec) {
        WsClientGenerator generator = createGenerator();

        generator.gen(getOptions(), getWebServiceConfig());

        //Endrer generert kildekode til å laste WSDLer ifra classpath (jar fil) i stedet for generert søppel URL
        generator.fixResourceLoaders();

        //SKTOOLS-79: Må også slette noen midlertidige filer som ikke ellers blir slettet fort nok til at Gradle ikke plukker dem opp som output-filer den senere savner
        generator.deleteTemporaryFiles();
    }

    WsClientGenerator createGenerator() {
        final WsClientGenerator generator = new WsClientGenerator(getProject()
                , getDestinationDir()
                , getTemporaryDir()
                , getSource()
                , getClasspath()
                , getWeblogicClasspath()
        );
        generator.sourceCompatibility = getSourceCompatibility();
        generator.targetCompatibility = getTargetCompatibility();
        generator.packageName = getPackageName();
        return generator;
    }


    /**
     * Returns the compilation options.
     *
     * @return The compilation options.
     */
    @Nested
    public CompileOptions getOptions() {
        return spec.getCompileOptions();
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
    public FileCollection getClasspath() { //markerer denne som optional
        return super.getClasspath() != null ? super.getClasspath() : getProject().files();
    }

    public Logger getLogger() {
        return logger;
    }

    @Optional
    @Input
    public String getPackageName() {
        return packageName;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public WebServiceConfig getWebServiceConfig() {
        return webServiceConfig;
    }

    public void setWebServiceConfig(WebServiceConfig webServiceConfig) {
        this.webServiceConfig = webServiceConfig;
    }
}
