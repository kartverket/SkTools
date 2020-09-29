package no.statkart.sktools.gradle.plugins.wsdlgen;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;

public class WsdlGenPlugin implements Plugin<Project> {
    public static final String WSDLGEN_TASK_NAME = "wsdlGen";

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);
        JavaPluginConvention javaPluginConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
        SourceSetContainer sourceSets = javaPluginConvention.getSourceSets();
        SourceSet mainSourceSet = sourceSets.getByName("main");

        FileCollection wsClasspath = mainSourceSet.getRuntimeClasspath().plus(
            mainSourceSet.getCompileClasspath() // I tilfelle JAX-WS API er compileOnly
        );

        File outputDir = new File(project.getBuildDir(), WSDLGEN_TASK_NAME);

        JavaExec wsdlGenTask = project.getTasks().create(WSDLGEN_TASK_NAME, JavaExec.class);

        wsdlGenTask.getInputs().files(wsClasspath);
        wsdlGenTask.getOutputs().dir(outputDir);

        wsdlGenTask.setClasspath(DependencyUtil.getWsdlGenClasspath(project));
        wsdlGenTask.setMain("no.statkart.sktools.utils.wsdlgen.SKGenWSDL");
        wsdlGenTask.args("-d", outputDir);

        wsdlGenTask.doFirst(task -> wsdlGenTask.args("-cp", wsClasspath.getAsPath()));
    }
}
