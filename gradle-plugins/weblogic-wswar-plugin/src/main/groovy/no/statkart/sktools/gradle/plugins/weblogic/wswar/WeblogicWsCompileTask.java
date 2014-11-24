package no.statkart.sktools.gradle.plugins.weblogic.wswar

import no.statkart.sktools.gradle.plugins.weblogic.WeblogicTaskInterface
import no.statkart.sktools.gradle.plugins.weblogic.compile.DefaultWeblogicCompileSpec
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.CompileOptions

/**
 * Task for kompilering av java-ws weblogic server implementasjon
 *
 * PS: Merk at denne kun kompilerer ws spesifik implementasjon. Det forutsettes derfor at resten ligger som kompilert kode på classpath
 *
 * @since 1.1
 * @author Leif Lislegård
 */
class WeblogicWsCompileTask extends AbstractCompile implements WeblogicTaskInterface {
    protected static final Logger logger = Logging.getLogger(WeblogicWsCompileTask.class);

    private WeblogicJaxWsCompiler compiler
    private final DefaultWeblogicCompileSpec spec = new DefaultWeblogicCompileSpec();

    private FileCollection weblogicClasspath

    File getGenDir() { return genDir ?: project.file("${project.buildDir}/weblogic/jwsc") }
    File genDir

    @OutputDirectory
    File getClassesDir() { return classesDir ?: project.file("${destinationDir}/WEB-INF/classes") }
    File classesDir  //for filer som skal på classpath

    @OutputDirectory
    File getGenSourcesDir() { genSourcesDir ?: project.file("gen/weblogic/jwsc") }
    File genSourcesDir



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
        spec.setTempDir(getTemporaryDir())

        spec.setDestinationDir(getGenDir());
        spec.setSource(getSource());
        spec.setClasspath(getClasspath());


        project.delete(spec.getDestinationDir()) //no dirty files

        WorkResult result = compiler.execute(spec);
        setDidWork(result.getDidWork());


        //extract application files
        project.delete(getDestinationDir()) //no dirty files
        project.copy {
           into this.getDestinationDir()
           from("${this.getGenDir()}/${project.name}.war") {
               exclude 'WEB-INF/classes/**'
               exclude 'WEB-INF/web.xml'
               exclude '**/*.java'
           }
           includeEmptyDirs = false
        }

        //extract generated classes
        project.delete(getClassesDir()) //no dirty files
        project.copy {
           into this.getClassesDir()
           from("${getGenDir()}/${project.name}.war/WEB-INF/classes")
        }

        //extract source files
        project.delete(genSourcesDir) //no dirty files
        project.copy {
           into this.genSourcesDir
           from("${this.getGenDir()}/${project.name}.war") {
               exclude 'WEB-INF/**' //only java source
           }
        }


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

    public FileCollection getWeblogicClasspath() {
        return weblogicClasspath;
    }

    public Logger getLogger() {
        return logger;
    }
}
