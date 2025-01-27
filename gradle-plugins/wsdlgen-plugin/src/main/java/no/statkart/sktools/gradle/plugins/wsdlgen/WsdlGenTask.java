package no.statkart.sktools.gradle.plugins.wsdlgen;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
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
        project.javaexec(spec -> {
            spec.classpath(DependencyUtil.getWsdlGenClasspath(project));
            spec.setMain("no.statkart.sktools.utils.wsdlgen.SKGenWSDL");
            spec.args("-d", destinationDirectory.get().getAsFile());
            getLogger().debug("Using WEB_SERVICE_CLASSPATH=\n" + compileClasspath.get().getAsPath());
            spec.environment("WEB_SERVICE_CLASSPATH", compileClasspath.get().getAsPath());
        });
    }
}
