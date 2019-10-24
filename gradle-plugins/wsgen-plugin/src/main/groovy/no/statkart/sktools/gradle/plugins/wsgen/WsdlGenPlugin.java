package no.statkart.sktools.gradle.plugins.wsgen;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.SourceSet;

import java.io.File;

/**
 * Konfigurasjon:
 *
 * <pre>
 *   apply plugin: 'sktools-wsgen-plugin'
 *
 *   dependencies {
 *      jaxws 'com.sun.xml.ws:jaxws-tools:2.2.10'
 *      jaxws 'com.sun.xml.ws:wscompile:2.2.10' //gammel
 *   }
 * </pre>
 */
public class WsdlGenPlugin implements Plugin<Project> {
    @Override
    public void apply(final Project project) {
        project.getPlugins().apply(WarPlugin.class);

        Configuration jaxwsConfiguration = project.getConfigurations().create("jaxws");
        jaxwsConfiguration.setDescription("JAX-WS tools");
        jaxwsConfiguration.defaultDependencies(new Action<DependencySet>() {
            @Override
            public void execute(DependencySet dependencies) {
                dependencies.add(project.getDependencies().create("com.sun.xml.ws:jaxws-tools:2.2.10"));
            }
        });

        JavaPluginConvention javaConventions = (JavaPluginConvention) project.getConvention().getPlugins().get("java");
        SourceSet sourceSet = javaConventions.getSourceSets().getByName("main");

        WsdlGenTask wsdlGenTask = project.getTasks().create(sourceSet.getTaskName("gen", "Wsdls"), WsdlGenTask.class);
        wsdlGenTask.source(sourceSet.getOutput());
        wsdlGenTask.setClasspath(project.files(sourceSet.getCompileClasspath(), sourceSet.getOutput().getClassesDirs()));
        wsdlGenTask.setJaxwsClasspath(jaxwsConfiguration);
        wsdlGenTask.setDestinationDir(new File(project.getBuildDir(), wsdlGenTask.getName()));
    }
}
