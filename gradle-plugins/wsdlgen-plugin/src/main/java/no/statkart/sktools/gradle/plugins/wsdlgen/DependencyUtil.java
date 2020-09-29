package no.statkart.sktools.gradle.plugins.wsdlgen;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.util.GUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class DependencyUtil {

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
    public static FileCollection getWsdlGenClasspath(Project project) {
        Properties testProperties = injectedTestProperties();
        if (testProperties == null) {
            final ScriptHandler buildscript = project.getBuildscript().getRepositories().isEmpty() ? project.getRootProject().getBuildscript() : project.getBuildscript();
            return buildscript.getConfigurations().detachedConfiguration(wsdlgenDependency(project));
        }

        String classpath = testProperties.getProperty("sktools_wsdlgen_classpath");
        // En trenger classpath for wsdlgen. Disse ligger i egen modul.
        return project.files((Object[]) classpath.split(File.pathSeparator)); //NB: for GradleRunner i debug mode
    }

    private static Dependency wsdlgenDependency(Project project) {
        Object dependencyNotation = Objects.requireNonNull(pluginProperties.getProperty("sktools_wsdlgen"), "Skal settes av byggesystem");
        return project.getDependencies().create(dependencyNotation);
    }

    /**
     * Test properties når man kjører tester, ellers null.
     */
    private static Properties injectedTestProperties() {
        InputStream stream = DependencyUtil.class.getResourceAsStream("/WsdlGenPluginTest.properties");
        //dersom denne finnes på classpath kjører man tester
        return stream == null ? null : GUtil.loadProperties(stream);
    }
}