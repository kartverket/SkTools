package no.statkart.sktools.gradle.plugins.wsgen;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.SourceSet;

import java.io.File;

public class WsdlGenPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPlugins().apply(WarPlugin.class);

        Configuration jaxwsConfiguration = project.getConfigurations().create("jaxws");
        jaxwsConfiguration.setDescription("JAX-WS tools");

        JavaPluginConvention javaConventions = (JavaPluginConvention) project.getConvention().getPlugins().get("java");
        SourceSet sourceSet = javaConventions.getSourceSets().getByName("main");

        WsdlGenTask wsdlGenTask = project.getTasks().create(sourceSet.getTaskName("gen", "Wsdls"), WsdlGenTask.class);
        wsdlGenTask.setInput(sourceSet.getOutput());
        wsdlGenTask.setClasspath(sourceSet.getCompileClasspath());
        wsdlGenTask.setJaxwsClasspath(jaxwsConfiguration);
        wsdlGenTask.setDestinationDir(new File(project.getBuildDir(), wsdlGenTask.getName()));
    }
}
