package no.statkart.sktools.gradle.plugins.wsdlgen;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Set;

public class WsdlGenTask extends DefaultTask {
    private final DirectoryProperty destinationDirectory = getProject().getObjects().directoryProperty()
        .convention(getProject().getLayout().getBuildDirectory().dir(getName()));

    private final Property<FileCollection> compileClasspath = getProject().getObjects().property(FileCollection.class)
        .convention(getProject().provider(() -> {
            SourceSet mainSourceSet = sourceSetContainer();
            return mainSourceSet.getRuntimeClasspath().plus(
                mainSourceSet.getCompileClasspath() // I tilfelle JAX-WS API er compileOnly
            );
        }));

    private final Property<Boolean> isJakarta = getProject().getObjects().property(Boolean.class)
        .convention(getProject().provider(() -> {
            Configuration configuration = getProject().getConfigurations().getByName(sourceSetContainer().getCompileClasspathConfigurationName());
            for (ResolvedArtifact resolvedArtifact : configuration.getResolvedConfiguration().getResolvedArtifacts()) {
                ModuleVersionIdentifier artifact = resolvedArtifact.getModuleVersion().getId();
                if ("jakarta.xml.ws".equals(artifact.getGroup()) && "jakarta.xml.ws-api".equals(artifact.getName())) {
                    String version = artifact.getVersion();
                    if (version != null && !version.isEmpty() && version.charAt(0) >= '3') {
                        getLogger().lifecycle("Jakarta detected - enabling Jakarta EE!");
                        return Boolean.TRUE;
                    }
                }
            }
            return Boolean.FALSE;
        }));

    private final Property<String> sourceSet = getProject().getObjects().property(String.class)
        .convention("main");
    private SourceSet sourceSetContainer() {
        return getProject().getExtensions().getByType(SourceSetContainer.class).getByName(sourceSet.get());
    }


    @CompileClasspath
    public Property<FileCollection> getCompileClasspath() {
        return compileClasspath;
    }

    public void setCompileClasspath(Iterable<?> iterable) {
        compileClasspath.set(getProject().files(iterable));
    }

    @OutputDirectory
    public DirectoryProperty getDestinationDirectory() {
        return this.destinationDirectory;
    }

    public void setDestinationDirectory(Provider<? extends Directory> destinationDirectory) {
        this.destinationDirectory.set(destinationDirectory);
    }

    public void setDestinationDirectory(File file) {
        destinationDirectory.set(file);
    }

    public void setDestinationDirectory(Directory directory) {
        destinationDirectory.set(directory);
    }

    @TaskAction
    public void exec() {
        Project project = getProject();
        String applicationClasspath = compileClasspath.get().getAsPath();
        Set<File> classpath = DependencyUtil.getWsdlGenClasspath(project, isJakarta.get()).getResolvedConfiguration().getFiles();
        project.javaexec(spec -> {
            spec.classpath(classpath);
            spec.setMain("no.statkart.sktools.utils.wsdlgen.SKGenWSDL");
            spec.args("-d", destinationDirectory.get().getAsFile());
            getLogger().debug("Using WEB_SERVICE_CLASSPATH=\n" + applicationClasspath);
            spec.environment("WEB_SERVICE_CLASSPATH", applicationClasspath);
        });
    }
}
