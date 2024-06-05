package no.statkart.sktools.gradle.plugins.wsdlgen;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;

public class WsdlGenPlugin implements Plugin<Project> {
    public static final String WSDLGEN_TASK_NAME = "wsdlGen";

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);

        SourceSet mainSourceSet = project.getExtensions().getByType(SourceSetContainer.class).getByName("main");

        FileCollection wsClasspath = mainSourceSet.getRuntimeClasspath().plus(
            mainSourceSet.getCompileClasspath() // I tilfelle JAX-WS API er compileOnly
        );

        File outputDir = project.getLayout().getBuildDirectory().dir(WSDLGEN_TASK_NAME).get().getAsFile();

        WsdlGenTask wsdlGenTask = project.getTasks().create(WSDLGEN_TASK_NAME, WsdlGenTask.class);
        wsdlGenTask.getCompileClasspath().setFrom(wsClasspath);
        wsdlGenTask.getDestinationDirectory().set(outputDir);
    }
}
