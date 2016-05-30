package no.statkart.sktools.gradle.plugins.provided;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.testfixtures.ProjectBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Test for {@link ProvidedPlugin}.
 *
 * @author Tor Egil R. Strand
 * @since 1.2
 */
public class ProvidedPluginTest {
    /**
     * Tester registrering av plugin via navn.
     */
    @Test
    public void testAppplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build();

        //konfigurerer project
        project.getPlugins().apply("sktools-provided-plugin");

        Assert.assertFalse(project.getPlugins().withType(ProvidedPlugin.class).isEmpty(), "Plugin ikke registrert");
        Assert.assertNotNull(project.getConfigurations().findByName(ProvidedPlugin.PROVIDED_CONFIGURATION_NAME), "Configuration ikke registrert");
    }

    @Test
    public void testProvidedConfiguration() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("sktools-provided-plugin");
        project.getPlugins().apply("java");

        project.getRepositories().maven(new Action<MavenArtifactRepository>() {
            @Override
            public void execute(MavenArtifactRepository repository) {
                repository.setName("PUBLIC");
                repository.setUrl("http://nexus.statkart.no:8090/nexus/content/groups/public/");
            }
        });

        project.getDependencies().add("provided", "com.google.code.findbugs:jsr305:1.3.9");

        Assert.assertTrue(project.getConfigurations().getByName("default").isEmpty(), "default er ikke tom");

        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
        Configuration provided = project.getConfigurations().getByName("provided");
        Assert.assertTrue(javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getCompileClasspath().getFiles().containsAll(provided.getFiles()), "Main compile classpath inneholder ikke provided");
        Assert.assertFalse(javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getRuntimeClasspath().getFiles().containsAll(provided.getFiles()), "Main runtime classpath inneholder provided");
        Assert.assertTrue(javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME).getCompileClasspath().getFiles().containsAll(provided.getFiles()), "Test compile classpath inneholder ikke provided");
        Assert.assertTrue(javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME).getRuntimeClasspath().getFiles().containsAll(provided.getFiles()), "Test runtime classpath inneholder ikke provided");
    }

    @Test
    public void testSinglevmConfiguration() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("sktools-provided-plugin");
        project.getPlugins().apply("java");

        project.getRepositories().maven(new Action<MavenArtifactRepository>() {
            @Override
            public void execute(MavenArtifactRepository repository) {
                repository.setName("PUBLIC");
                repository.setUrl("http://nexus.statkart.no:8090/nexus/content/groups/public/");
            }
        });

        project.getDependencies().add("singlevm", "com.google.code.findbugs:jsr305:1.3.9");

        Assert.assertTrue(project.getConfigurations().getByName("default").isEmpty(), "default er ikke tom");

        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
        Configuration provided = project.getConfigurations().getByName("singlevm");
        Assert.assertFalse(javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getCompileClasspath().getFiles().containsAll(provided.getFiles()), "Main compile classpath inneholder singlevm");
        Assert.assertTrue(javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getRuntimeClasspath().getFiles().containsAll(provided.getFiles()), "Main runtime classpath inneholder ikke singlevm");
        Assert.assertFalse(javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME).getCompileClasspath().getFiles().containsAll(provided.getFiles()), "Test compile classpath inneholder singlevm");
        Assert.assertTrue(javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME).getRuntimeClasspath().getFiles().containsAll(provided.getFiles()), "Test runtime classpath inneholder ikke singlevm");
    }
}
