package no.statkart.sktools.gradle.plugins.wsdlgen;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.initialization.dsl.ScriptHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

class DependencyUtil {

    private static final Properties pluginProperties = new Properties();

    static {
        try {
            pluginProperties.load(DependencyUtil.class.getResourceAsStream("/no/statkart/sktools/wsdlgen-gradle-plugin.properties"));
        } catch (IOException ignored) {
            System.err.println("Error loading plugin properties!");
        }
    }

    /**
     * Gir deg dependencies avhengig av om det kjøres som test eller ikke.
     */
    static Configuration getWsdlGenClasspath(Project project) {
        final ScriptHandler buildscript = project.getBuildscript().getRepositories().isEmpty() ? project.getRootProject().getBuildscript() : project.getBuildscript();
        return buildscript.getConfigurations().detachedConfiguration(
            project.getDependencies().enforcedPlatform(pluginProperties.getProperty("sktools_wsdlgen_jakarta_platform")),
            project.getDependencies().create("com.sun.xml.ws:jaxws-rt"), //provided
            wsdlgenDependency(project));
    }

    private static Dependency wsdlgenDependency(Project project) {
        final Properties testProperties = injectedTestProperties();
        final Object dependencyNotation;
        if (testProperties == null) {
            dependencyNotation = Objects.requireNonNull(pluginProperties.getProperty("sktools_wsdlgen"), "Skal settes av byggesystem");
        } else {
            dependencyNotation = project.files((Object[]) testProperties.getProperty("sktools_wsdlgen_classpath").split(File.pathSeparator)); //NB: for GradleRunner i debug mode
        }
        return project.getDependencies().create(dependencyNotation);
    }

    /**
     * Test properties når man kjører tester, ellers null.
     */
    private static Properties injectedTestProperties() {
        try (InputStream stream = DependencyUtil.class.getResourceAsStream("/WsdlGenPluginTest.properties")) {
            if (stream != null) { //dersom denne finnes på classpath kjører man tester
                final Properties properties = new Properties();
                properties.load(stream);
                return properties;
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }
}
