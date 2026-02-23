package no.statkart.sktools.gradle.plugins.wsdlgen;

import org.gradle.api.Project;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.internal.IoActions;

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
        InputStream stream = DependencyUtil.class.getResourceAsStream("/WsdlGenPluginTest.properties");
        //dersom denne finnes på classpath kjører man tester
        return stream == null ? null : loadProperties(stream);
    }

    private static Properties loadProperties(InputStream inputStream) {
        Properties properties = new Properties();

        try {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IoActions.closeQuietly(inputStream);
        }

        return properties;
    }
}
