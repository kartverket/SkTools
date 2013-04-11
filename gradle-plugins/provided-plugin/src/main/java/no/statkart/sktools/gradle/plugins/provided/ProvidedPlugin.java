package no.statkart.sktools.gradle.plugins.provided;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;

/**
 * Oppretter provided-konfigurasjon for alle SourceSets og konfigurerer IdeaModule (om/når idea-plugin applyes).
 *
 * @author Tor Egil R. Strand
 * @since 1.2
 */
public class ProvidedPlugin implements Plugin<Project> {
    public final static String PROVIDED_CONFIGURATION_NAME = "provided";

    @Override
    public void apply(final Project project) {
        final Configuration providedConfiguration = project.getConfigurations().create(PROVIDED_CONFIGURATION_NAME);
        providedConfiguration.setVisible(true);
        providedConfiguration.setTransitive(true);
        providedConfiguration.setDescription("Configuration for dependencies needed when compiling and when running locally, but not when deploying to JEE container.");

        // Koble opp for main og test source set fra Java Plugin
        project.getPlugins().withType(JavaPlugin.class).all(new Action<JavaPlugin>() {
            @Override
            public void execute(JavaPlugin javaPlugin) {
                JavaPluginConvention javaPluginConvention = project.getConvention().getPlugin(JavaPluginConvention.class);

                SourceSet mainSourceSet = javaPluginConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                mainSourceSet.setCompileClasspath(mainSourceSet.getCompileClasspath().plus(providedConfiguration));

                SourceSet testSourceSet = javaPluginConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
                testSourceSet.setCompileClasspath(testSourceSet.getCompileClasspath().plus(providedConfiguration));
                testSourceSet.setRuntimeClasspath(testSourceSet.getRuntimeClasspath().plus(providedConfiguration));
            }
        });

        // Legg til for Idea Plugin
        project.getPlugins().withType(IdeaPlugin.class).all(new Action<IdeaPlugin>() {
            @Override
            public void execute(IdeaPlugin ideaPlugin) {
                IdeaModel ideaModel = project.getExtensions().getByType(IdeaModel.class);
                ideaModel.getModule().getScopes().get("COMPILE").get("plus").add(providedConfiguration);
            }
        });
    }
}
