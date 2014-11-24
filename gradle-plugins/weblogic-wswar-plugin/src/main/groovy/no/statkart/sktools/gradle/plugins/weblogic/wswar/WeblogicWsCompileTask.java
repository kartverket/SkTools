package no.statkart.sktools.gradle.plugins.weblogic.wswar;

import no.statkart.sktools.gradle.plugins.weblogic.WeblogicTaskInterface;
import no.statkart.sktools.gradle.plugins.weblogic.compile.DefaultWeblogicCompileSpec;
import org.codehaus.groovy.runtime.MethodClosure;
import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.CompileOptions;

import java.io.File;

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

    private final WeblogicJaxWsCompiler compiler;
    private final DefaultWeblogicCompileSpec spec = new DefaultWeblogicCompileSpec();

    private FileCollection weblogicClasspath;

    private File classesDir = null;  //for filer som skal på classpath
    private File genSourcesDir = null;
    private File genDir = null;


    public WeblogicWsCompileTask() {
        final Project project = getProject();

        getLogging().captureStandardOutput(LogLevel.INFO);
        getLogging().captureStandardError(LogLevel.DEBUG);

        compiler = new WeblogicJaxWsCompiler();
        compiler.setAnt(project.getAnt());
        compiler.setBaseDir(project.file("src"));
        compiler.setWarName(project.getName() + ".war");
        compiler.setFileResolver(((ProjectInternal) project).getFileResolver());

        //setting defaults for this compiler.
        getOptions().setFork(true);
        getOptions().setListFiles(true);
        getOptions().setVerbose(logger.isDebugEnabled());
        getOptions().setFailOnError(true); //defaults to true

    }

    @TaskAction
    protected void compile() {
        final Project project = getProject();

        spec.setWeblogicClasspath(getWeblogicClasspath().getFiles());
        spec.setTempDir(getTemporaryDir());

        spec.setDestinationDir(getGenDir());
        spec.setSource(getSource());
        spec.setClasspath(getClasspath());


        project.delete(getGenDir()); //no dirty files

        WorkResult result = compiler.execute(spec);
        setDidWork(result.getDidWork());


        //extract application files
        project.delete(getDestinationDir()); //no dirty files

        project.copy(new MethodClosure(this, "applicationFilesCopySpec"));

        //extract generated classes
        project.delete(getClassesDir()); //no dirty files
        project.copy(new MethodClosure(this, "generatedClassesCopySpec"));

        //extract source files
        project.delete(getGenSourcesDir()); //no dirty files
        project.copy(new MethodClosure(this, "generatedSourcesCopySpec"));
    }


    @SuppressWarnings("UnusedDeclaration") //kalles via groovy closure
    private void applicationFilesCopySpec(CopySpec spec) {
        spec.into(getDestinationDir());
        spec.from(getGenDir().getPath() + "/" + getProject().getName() + ".war")
                .exclude("WEB-INF/classes/**"
                        , "WEB-INF/web.xml"
                        , "**/*.java"
                );
        spec.setIncludeEmptyDirs(false);
    }

    @SuppressWarnings("UnusedDeclaration") //kalles via groovy closure
    private void generatedClassesCopySpec(CopySpec spec) {
        spec.into(getClassesDir());
        spec.from(getGenDir().getPath() + "/" + getProject().getName() + ".war/WEB-INF/classes")
        ;
    }

    @SuppressWarnings("UnusedDeclaration") //kalles via groovy closure
    private void generatedSourcesCopySpec(CopySpec spec) {
        spec.into(getClassesDir());
        spec.from(getGenDir().getPath() + "/" + getProject().getName() + ".war/")
                .exclude("WEB-INF/**") //no compiled class files etc...
        ;
    }


    @OutputDirectory
    public File getClassesDir() {
        return classesDir != null ? classesDir : getProject().file("${destinationDir}/WEB-INF/classes");
    }

    void setClassesDir(File classesDir) {
        this.classesDir = classesDir;
    }

    @OutputDirectory
    public File getGenSourcesDir() {
        return genSourcesDir != null ? genSourcesDir : getProject().file("gen/weblogic/jwsc");
    }

    void setGenSourcesDir(File genSourcesDir) {
        this.genSourcesDir = genSourcesDir;
    }

    public File getGenDir() {
        return genDir != null ? genDir : getProject().file("${project.buildDir}/weblogic/jwsc");
    }


    void setGenDir(File genDir) {
        this.genDir = genDir;
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
