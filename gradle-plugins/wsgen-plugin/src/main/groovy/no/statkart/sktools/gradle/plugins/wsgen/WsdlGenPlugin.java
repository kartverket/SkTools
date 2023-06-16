package no.statkart.sktools.gradle.plugins.wsgen;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;

/**
 * Konfigurasjon:
 *
 * <pre>
 *   apply plugin: 'sktools-wsgen-plugin'
 *
 *   dependencies {
 *      jaxws 'com.sun.xml.ws:jaxws-tools:2.3.5'
 *      jaxws 'com.sun.xml.ws:wscompile:2.3.5' //gammelt koordinat
 *   }
 * </pre>
 */
public class WsdlGenPlugin implements Plugin<Project> {
    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply(WarPlugin.class);

        Configuration jaxwsConfiguration = project.getConfigurations().create("jaxws")
            .setDescription("JAX-WS tools")
            .defaultDependencies(dependencies -> dependencies.add(project.getDependencies().create("com.sun.xml.ws:jaxws-tools:2.3.5")));

        //default verdi for enkelt å komme igang / testing ...
        project.getConfigurations().getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)
            .defaultDependencies(dependencies -> dependencies.add(project.getDependencies().create("com.sun.xml.ws:jaxws-rt:2.3.5")));

        SourceSet sourceSet = project.getExtensions().getByType(SourceSetContainer.class).getByName("main");

        WsdlGenTask wsdlGenTask = project.getTasks().create(sourceSet.getTaskName("gen", "Wsdls"), WsdlGenTask.class);
        wsdlGenTask.source(sourceSet.getOutput());
        wsdlGenTask.setClasspath(project.files(sourceSet.getCompileClasspath(), sourceSet.getOutput().getClassesDirs()));
        wsdlGenTask.setJaxwsClasspath(jaxwsConfiguration);
        wsdlGenTask.setDestinationDir(new File(project.getBuildDir(), wsdlGenTask.getName()));
    }
}
