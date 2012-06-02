package no.statkart.sktools.gradle.plugins.weblogic.wsclient

import no.statkart.sktools.gradle.plugins.weblogic.WeblogicTaskInterface
import no.statkart.sktools.gradle.plugins.weblogic.compile.DefaultWeblogicCompileSpec
import no.statkart.sktools.gradle.plugins.weblogic.compile.WeblogicCompileSpec
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.CompileOptions
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.InputFiles

/**
 * Task for generering av weblogic webservice klient
 *
 *
 * @since 1.1
 * @author Leif Lislegård
 */
class WeblogicGenClientTask extends AbstractCompile implements WeblogicTaskInterface {

    private WeblogicJaxWsClientCompiler compiler;
    private final DefaultWeblogicCompileSpec spec = new DefaultWeblogicCompileSpec();

    private FileCollection weblogicClasspath;
    private File dependencyCacheDir;


    WeblogicGenClientTask() {
        logging.captureStandardOutput LogLevel.INFO
        logging.captureStandardError LogLevel.DEBUG

        compiler = new WeblogicJaxWsClientCompiler();

        include('**/*.wsdl') //inkluderer denne som input fra sourceset (benyttes bla for skipIfEmpty beregning)

        getOptions().setFork(true)
        getOptions().setListFiles(true)
        getOptions().setVerbose(logger.isDebugEnabled())
        getOptions().setFailOnError(true) //defaults to true

    }

    @TaskAction
    protected void compile() {
        compiler.ant = getAnt()
        compiler.webServices = project.getConvention().getPlugins().get(WeblogicWsClientPlugin.CONVENTION_NAME).webService
        spec.setWeblogicClasspath(getWeblogicClasspath().files)
        spec.setTempDir(project.file('build/tmp/weblogic'))

        spec.setSource(getSource());
        spec.setDestinationDir(getDestinationDir());
        spec.setClasspath(getClasspath());
        spec.setDependencyCacheDir(getDependencyCacheDir());
        spec.setSourceCompatibility(getSourceCompatibility());
        spec.setTargetCompatibility(getTargetCompatibility());

        WorkResult result = compiler.execute(spec);
        setDidWork(result.getDidWork());

    }

    /**
     * Action som retter loading av wsdl filer ifra webstart klienter osv.
     * Rettinger blir påført i klikdekoden.
     */
    @TaskAction
    protected void fixResourceLoaders() {
        ant.replaceregexp {
            regexp(pattern: /URL baseUrl;[^=]+\s(.*getResource).*;[^=]*.*baseUrl, "(.*)".*;([^{]*)MalformedURL/)
            substitution(expression: ('url \\1("/\\2");\\3'))
            fileset(dir: getDestinationDir(), erroronmissingdir: true) {
                include(name: '**/*.java')
            }
        }
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
        return spec.getCompileOptions();
    }

    public org.gradle.api.internal.tasks.compile.Compiler<WeblogicCompileSpec> getCompiler() {
        return compiler;
    }

    public void setCompiler(org.gradle.api.internal.tasks.compile.Compiler<WeblogicCompileSpec> compiler) {
        this.compiler = compiler;
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
