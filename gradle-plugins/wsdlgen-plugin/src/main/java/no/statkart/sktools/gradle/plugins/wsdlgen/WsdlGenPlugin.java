package no.statkart.sktools.gradle.plugins.wsdlgen;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

public class WsdlGenPlugin implements Plugin<Project> {
    public static final String WSDLGEN_TASK_NAME = "wsdlGen";

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);

        project.getTasks().register(WSDLGEN_TASK_NAME, WsdlGenTask.class);
    }
}
