package no.statkart.sktools.gradle.plugins.provided;

import org.assertj.core.api.Assertions;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
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

        //en eller annen dependency - dersom ikke denne fungerer bør testen skrives om til en demo
        project.getDependencies().add("provided", project.getDependencies().localGroovy());

        Assert.assertTrue(project.getConfigurations().getByName("default").isEmpty(), "default er ikke tom");

        Configuration provided = project.getConfigurations().getByName("provided");

        Assertions.assertThat(project.getConfigurations().getByName("compileClasspath").getFiles())
                .withFailMessage("compileClasspath inneholder ikke provided")
                .containsAll(provided.getFiles());
        Assertions.assertThat(project.getConfigurations().getByName("runtimeClasspath").getFiles())
                .withFailMessage("runtimeClasspath inneholder ikke provided")
                .containsAll(provided.getFiles());
        Assertions.assertThat(project.getConfigurations().getByName("runtimeElements").getAllDependencies())
                .withFailMessage("runtimeElements inneholder provided")
                .isEmpty();
        Assertions.assertThat(project.getConfigurations().getByName("testCompileClasspath").getFiles())
                .withFailMessage("testCompileClasspath inneholder ikke provided")
                .containsAll(provided.getFiles());
        Assertions.assertThat(project.getConfigurations().getByName("testRuntimeClasspath").getFiles())
                .withFailMessage("testRuntimeClasspath inneholder ikke provided")
                .containsAll(provided.getFiles());

        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
        Assertions.assertThat(javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getCompileClasspath().getFiles())
                .withFailMessage("Main compile classpath inneholder ikke provided")
                .containsAll(provided.getFiles());
        Assertions.assertThat(javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getRuntimeClasspath().getFiles())
                .withFailMessage("Main runtime classpath inneholder provided")
                .containsAll(provided.getFiles());
        Assertions.assertThat(javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME).getCompileClasspath().getFiles())
                .withFailMessage("Test compile classpath inneholder ikke provided")
                .containsAll(provided.getFiles());
        Assertions.assertThat(javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME).getRuntimeClasspath().getFiles())
                .withFailMessage("Test runtime classpath inneholder ikke provided")
                .containsAll(provided.getFiles());
    }

    @Test
    public void testSinglevmConfiguration() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("sktools-provided-plugin");
        project.getPlugins().apply("java");

        //en eller annen dependency - dersom ikke denne fungerer bør testen skrives om til en demo
        project.getDependencies().add("singlevm", project.getDependencies().localGroovy());

        Assert.assertTrue(project.getConfigurations().getByName("default").isEmpty(), "default er ikke tom");

        Configuration provided = project.getConfigurations().getByName("singlevm");

        Assertions.assertThat(project.getConfigurations().getByName("compileClasspath").getFiles())
                .withFailMessage("compileClasspath inneholder singlevm")
                .doesNotContainAnyElementsOf(provided.getFiles());
        Assertions.assertThat(project.getConfigurations().getByName("runtimeClasspath").getFiles())
                .withFailMessage("runtimeClasspath inneholder ikke singlevm")
                .containsAll(provided.getFiles());
        Assertions.assertThat(project.getConfigurations().getByName("runtimeElements").getAllDependencies())
                .withFailMessage("runtimeElements inneholder singlevm")
                .isEmpty();
        Assertions.assertThat(project.getConfigurations().getByName("testCompileClasspath").getFiles())
                .withFailMessage("testCompileClasspath inneholder ikke singlevm")
                .containsAll(provided.getFiles());
        Assertions.assertThat(project.getConfigurations().getByName("testRuntimeClasspath").getFiles())
                .withFailMessage("testRuntimeClasspath inneholder ikke singlevm")
                .containsAll(provided.getFiles());

        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
        Assertions.assertThat(javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getCompileClasspath().getFiles())
                .withFailMessage("Main compile classpath inneholder singlevm")
                .doesNotContainAnyElementsOf(provided.getFiles());
        Assertions.assertThat(javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getRuntimeClasspath().getFiles())
                .withFailMessage("Main runtime classpath inneholder ikke singlevm")
                .containsAll(provided.getFiles());
        Assertions.assertThat(javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME).getCompileClasspath().getFiles())
                .withFailMessage("Test compile classpath inneholder singlevm")
                .containsAll(provided.getFiles());
        Assertions.assertThat(javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME).getRuntimeClasspath().getFiles())
                .withFailMessage("Test runtime classpath inneholder ikke singlevm")
                .containsAll(provided.getFiles());
    }
}
