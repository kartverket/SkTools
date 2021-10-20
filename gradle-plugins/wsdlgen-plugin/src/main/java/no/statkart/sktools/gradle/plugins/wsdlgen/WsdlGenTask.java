package no.statkart.sktools.gradle.plugins.wsdlgen;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

@SuppressWarnings("UnstableApiUsage")
public class WsdlGenTask extends DefaultTask {
    private final DirectoryProperty destinationDirectory = this.getProject().getObjects().directoryProperty();
    private final ConfigurableFileCollection compileClasspath = this.getProject().files();

    @CompileClasspath
    public ConfigurableFileCollection getCompileClasspath() {
        return compileClasspath;
    }

    public void setCompileClasspath(Iterable<?> iterable) {
        compileClasspath.setFrom(iterable);
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
            getLogger().debug("Using WEB_SERVICE_CLASSPATH=\n" + compileClasspath.getAsPath());
            spec.environment("WEB_SERVICE_CLASSPATH", compileClasspath.getAsPath());
        });
    }
}
