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
    public final static String SINGLEVM_CONFIGURATION_NAME = "singlevm";

    @Override
    public void apply(final Project project) {
        project.getPlugins().apply(JavaPlugin.class);

        final Configuration providedConfiguration = project.getConfigurations().create(PROVIDED_CONFIGURATION_NAME);
        providedConfiguration.setVisible(true);
        providedConfiguration.setTransitive(true);
        providedConfiguration.setDescription("Configuration for dependencies needed when compiling and when running locally, but not when deploying to JEE container.");

        project.getConfigurations().getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).extendsFrom(providedConfiguration);
        // Egentlig skulle også testImplementation vært her, men det kommer via singlevm, så det er overflødig

        final Configuration singlevmConfiguration = project.getConfigurations().create(SINGLEVM_CONFIGURATION_NAME);
        singlevmConfiguration.setVisible(true);
        singlevmConfiguration.setTransitive(true);
        singlevmConfiguration.setDescription("Configuration for dependencies needed when when running in Single-VM, but not when running against a server, or that are deployed to JEE container by other means.");
        singlevmConfiguration.extendsFrom(providedConfiguration);

        project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).extendsFrom(singlevmConfiguration);
        project.getConfigurations().getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(singlevmConfiguration);
    }
}
