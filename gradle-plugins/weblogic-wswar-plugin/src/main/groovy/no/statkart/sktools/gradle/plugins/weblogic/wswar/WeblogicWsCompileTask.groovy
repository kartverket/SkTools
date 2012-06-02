package no.statkart.sktools.gradle.plugins.weblogic.wswar

import no.statkart.sktools.gradle.plugins.weblogic.WeblogicTaskInterface
import no.statkart.sktools.gradle.plugins.weblogic.compile.DefaultWeblogicCompileSpec
import no.statkart.sktools.gradle.plugins.weblogic.compile.WeblogicCompileSpec
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.CompileOptions

/**
 * Task for kompilering av java-ws weblogic server implementasjon
 *
 *
 * @since 1.1
 * @author Leif Lislegård
 */
class WeblogicWsCompileTask extends AbstractCompile implements WeblogicTaskInterface {

    private WeblogicJaxWsCompiler compiler
    private final DefaultWeblogicCompileSpec spec = new DefaultWeblogicCompileSpec();

    private FileCollection weblogicClasspath


    WeblogicWsCompileTask() {
        logging.captureStandardOutput LogLevel.INFO
        logging.captureStandardError LogLevel.DEBUG

        compiler = new WeblogicJaxWsCompiler()

        //setting defaults for this compiler.
        getOptions().setFork(true)
        getOptions().setListFiles(true)
        getOptions().setVerbose(logger.isDebugEnabled())
        getOptions().setFailOnError(true) //defaults to true

    }

    @TaskAction
    protected void compile() {
        compiler.ant = getAnt()
        compiler.baseDir = project.file('src')
        compiler.warName = project.name + ".war"
        compiler.fileResolver = ((ProjectInternal)getProject()).getFileResolver()

        spec.weblogicClasspath = getWeblogicClasspath().files
        spec.setTempDir(project.file('build/tmp/weblogic'))

        spec.setSource(getSource());
        spec.setDestinationDir(getDestinationDir());
        spec.setClasspath(getClasspath());

        WorkResult result = compiler.execute(spec);
        setDidWork(result.getDidWork());

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

    public FileCollection getWeblogicClasspath() {
        return weblogicClasspath;
    }

}
