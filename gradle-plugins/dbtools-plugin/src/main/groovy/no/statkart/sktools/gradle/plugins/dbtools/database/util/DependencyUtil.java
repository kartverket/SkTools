package no.statkart.sktools.gradle.plugins.dbtools.database.util;

import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsPlugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata;
import org.gradle.util.GUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class DependencyUtil {

    static final Properties pluginProperties = new Properties();
    static {
        try {
            pluginProperties.load(DbtoolsPlugin.class.getResourceAsStream("/no/statkart/sktools/dbtools-gradle-plugin.properties"));
        } catch (IOException ignored) {
            System.err.println("Error loading plugin properties!");
        }
    }

    /**
     * Gir deg dependencies avhengig av om det kjøres som test eller ikke.
     */
    public static FileCollection getDatabasePatcherClasspath(Project project) {
        InputStream testKitMetadataStream = testEnvironmentClasspath();
        if (testKitMetadataStream == null) {
            final ScriptHandler buildscript = project.getBuildscript().getRepositories().isEmpty() ? project.getRootProject().getBuildscript() : project.getBuildscript();
            return buildscript.getConfigurations().detachedConfiguration(dbutilsDependency(project));
        }
        Properties properties = GUtil.loadProperties(testKitMetadataStream);
        String classpath = properties.getProperty(PluginUnderTestMetadata.IMPLEMENTATION_CLASSPATH_PROP_KEY);
        // En trenger classpath for dbtools (db-tools)
        // disse ligger i egen modul
        return project.files((Object[]) classpath.split(";")); //NB: for GradleRunner i debug mode
    }

    static Dependency dbutilsDependency(Project project) {
        Object dependencyNotation = Objects.requireNonNull(pluginProperties.getProperty("sktools_dbtools"), "Skal settes av byggesystem");
        return project.getDependencies().create(dependencyNotation);
    }

    /**
     * Classpath satt opp for Gradle TestKit
     */
    static InputStream testEnvironmentClasspath() {
        return DependencyUtil.class.getResourceAsStream('/' + PluginUnderTestMetadata.METADATA_FILE_NAME); //dersom denne finnes på classpath kjører man tester
    }



}
