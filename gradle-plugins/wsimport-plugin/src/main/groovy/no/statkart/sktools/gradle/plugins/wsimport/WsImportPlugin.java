package no.statkart.sktools.gradle.plugins.wsimport;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.plugins.ide.idea.IdeaPlugin;

import java.io.File;

/**
 * Konfigurasjon:
 *
 * <pre>
 *   dependencies {
 *      jaxws 'com.sun.xml.ws:jaxws-tools:2.3.6'
 *      jaxws 'com.sun.xml.ws:wscompile:2.3.6' //gammelt koordinat
 *   }
 * </pre>
 *
 * Skjema filer legges som standard i {@code sourceSets.main.resources.srcDir}
 */
public class WsImportPlugin implements Plugin<Project> {
    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply(JavaPlugin.class);

        Configuration jaxwsConfiguration = project.getConfigurations().create("jaxws")
            .setDescription("JAX-WS tools")
            .defaultDependencies(dependencies -> dependencies.add(project.getDependencies().create("com.sun.xml.ws:jaxws-tools:2.3.6")));

        //default verdi for enkelt å komme igang / testing ...
        project.getConfigurations().getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)
            .defaultDependencies(dependencies -> dependencies.add(project.getDependencies().create("com.sun.xml.ws:jaxws-rt:2.3.6")));


      final File genSrcDir = project.getLayout().getBuildDirectory().dir("wsimport").get().getAsFile();

        SourceSet mainSourceSet = project.getExtensions().getByType(SourceSetContainer.class).getByName("main");

        Task resourcesTask = project.getTasks().getByPath(mainSourceSet.getProcessResourcesTaskName());
        WsImportTask wsImportTask = project.getTasks().create("wsimport", WsImportTask.class);
        wsImportTask.setDestinationDir(genSrcDir);
        wsImportTask.setJaxwsClasspath(jaxwsConfiguration);
        wsImportTask.source(mainSourceSet.getResources());
        wsImportTask.dependsOn(resourcesTask);

        mainSourceSet.getJava().srcDir(wsImportTask);

        project.getPlugins().withType(IdeaPlugin.class, new Action<IdeaPlugin>() {
            @Override
            public void execute(IdeaPlugin ideaPlugin) {
                ideaPlugin.getModel().getModule().getGeneratedSourceDirs().add(genSrcDir);
                project.getTasks().getByName("ideaModule").doFirst(new Action<Task>() {
                    @Override
                    public void execute(Task task) {
                        genSrcDir.mkdirs();
                    }
                });
            }
        });
    }
}
