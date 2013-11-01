package no.statkart.sktools.gradle.plugins.weblogic.wsclient.internal

import org.gradle.api.tasks.compile.AbstractCompile
import no.statkart.sktools.gradle.plugins.weblogic.wsclient.WeblogicWsClientCompileTask
import org.gradle.api.internal.tasks.compile.JavaCompileSpec
import org.gradle.api.tasks.compile.CompileOptions
import org.gradle.api.AntBuilder
import org.gradle.internal.Factory
import org.gradle.api.internal.tasks.compile.JavaCompilerFactory
import org.gradle.api.internal.tasks.compile.InProcessJavaCompilerFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.file.TemporaryFileProvider
import org.gradle.api.internal.tasks.compile.DefaultJavaCompilerFactory
import org.gradle.api.internal.tasks.compile.DelegatingJavaCompiler
import org.gradle.api.internal.tasks.compile.IncrementalJavaCompiler
import org.gradle.api.tasks.TaskAction
import org.gradle.api.internal.tasks.compile.DefaultJavaCompileSpec
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Nested
import org.gradle.api.internal.tasks.compile.Compiler

/**
 * Task som kompilerer java kildekoden. Se {@link WeblogicWsClientCompileTask}
 *
 * PS: extender ikke JavaCompile da man ønsker bakoverkompabilitet med gradle.version < 1.2
 *
 * @author Leif Lislegård
 * @since 1.2
 */
public class WeblogicWsClientCompileTaskImpl extends AbstractCompile implements WeblogicWsClientCompileTask {
    private org.gradle.api.internal.tasks.compile.Compiler<JavaCompileSpec> javaCompiler;
    private File dependencyCacheDir;
    private final CompileOptions compileOptions = new CompileOptions();

    public WeblogicWsClientCompileTaskImpl() {
        Factory<AntBuilder> antBuilderFactory = getServices().getFactory(AntBuilder.class);
        JavaCompilerFactory inProcessCompilerFactory = new InProcessJavaCompilerFactory();
        ProjectInternal projectInternal = (ProjectInternal) getProject();
        JavaCompilerFactory defaultCompilerFactory
        if (projectInternal.gradle.gradleVersion <= '1.6') {
            TemporaryFileProvider tempFileProvider = projectInternal.getServices().get(TemporaryFileProvider.class);
            defaultCompilerFactory = new DefaultJavaCompilerFactory(projectInternal, tempFileProvider, antBuilderFactory, inProcessCompilerFactory);
        } else {
            defaultCompilerFactory = new DefaultJavaCompilerFactory(projectInternal, antBuilderFactory, inProcessCompilerFactory);
        }
        org.gradle.api.internal.tasks.compile.Compiler<JavaCompileSpec> delegatingCompiler = new DelegatingJavaCompiler(defaultCompilerFactory);
        javaCompiler = new IncrementalJavaCompiler(delegatingCompiler, antBuilderFactory, getOutputs());
    }

    @TaskAction
    protected void compile() {
        DefaultJavaCompileSpec spec = new DefaultJavaCompileSpec();
        spec.setSource(getSource());
        spec.setDestinationDir(getDestinationDir());
        spec.setClasspath(getClasspath());
        spec.setDependencyCacheDir(getDependencyCacheDir());
        spec.setSourceCompatibility(getSourceCompatibility());
        spec.setTargetCompatibility(getTargetCompatibility());
        if (project.gradle.gradleVersion >= "1.2") {
            spec.compileOptions = compileOptions;   //bakoverkompabilitet med gradle < 1.2
        }
        WorkResult result = javaCompiler.execute(spec);
        setDidWork(result.getDidWork());
    }

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
        return compileOptions;
    }

    public Compiler<JavaCompileSpec> getJavaCompiler() {
        return javaCompiler;
    }

    public void setJavaCompiler(Compiler<JavaCompileSpec> javaCompiler) {
        this.javaCompiler = javaCompiler;
    }
}