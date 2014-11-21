package no.statkart.sktools.gradle.plugins.weblogic.wsclient

import no.statkart.sktools.gradle.plugins.weblogic.WeblogicTaskInterface
import no.statkart.sktools.gradle.plugins.weblogic.wsclient.internal.WsClientGenerator
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
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
    protected static final Logger logger = Logging.getLogger(WeblogicGenClientTask.class);


    WebServiceConfig webServiceConfig;

    @Optional
    @Input
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
        project.delete(getDestinationDir());

        gen();
    }


    void gen() {
        WsClientGenerator generator = createGenerator()

        generator.gen(getOptions(), getWebServiceConfig());

        //Endrer generert kildekode til å laste WSDLer ifra classpath (jar fil) i stedet for generert søppel URL
        generator.fixResourceLoaders();

        //SKTOOLS-79: Må også slette noen midlertidige filer som ikke ellers blir slettet fort nok til at Gradle ikke plukker dem opp som output-filer den senere savner
        generator.deleteTemporaryFiles();
    }

    def WsClientGenerator createGenerator() {
        final WsClientGenerator generator = new WsClientGenerator(project
                , getDestinationDir()
                , getTemporaryDir()
                , getSource()
                , getClasspath()
                , getWeblogicClasspath()
        );
        generator.sourceCompatibility = getSourceCompatibility();
        generator.targetCompatibility = getTargetCompatibility();
        generator.packageName = packageName;
        return generator
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

    public Logger getLogger() {
        return logger;
    }
}
