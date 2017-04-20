package no.statkart.sktools.gradle.plugins.wsimport;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.plugins.ide.idea.IdeaPlugin;

import java.io.File;

public class WsImportPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);

        Configuration jaxwsConfiguration = project.getConfigurations().create("jaxws");

        final File genSrcDir = new File(project.getBuildDir(), "wsimport");

        SourceSet mainSourceSet = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName("main");

        Task resourcesTask = project.getTasks().getByPath(mainSourceSet.getProcessResourcesTaskName());
        WsImportTask wsImportTask = project.getTasks().create("wsimport", WsImportTask.class);
        wsImportTask.setDestinationDir(genSrcDir);
        wsImportTask.setJaxwsClasspath(jaxwsConfiguration);
        wsImportTask.source(mainSourceSet.getResources());
        wsImportTask.dependsOn(resourcesTask);

        Task compileJavaTask = project.getTasks().getByName(mainSourceSet.getCompileJavaTaskName());
        compileJavaTask.dependsOn(wsImportTask);
        mainSourceSet.getJava().srcDir(genSrcDir);

        project.getPlugins().withType(IdeaPlugin.class, new Action<IdeaPlugin>() {
            @Override
            public void execute(IdeaPlugin ideaPlugin) {
                ideaPlugin.getModel().getModule().getGeneratedSourceDirs().add(genSrcDir);
            }
        });
    }
}
