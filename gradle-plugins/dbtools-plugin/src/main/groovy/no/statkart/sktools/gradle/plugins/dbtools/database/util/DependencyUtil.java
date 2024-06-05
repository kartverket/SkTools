package no.statkart.sktools.gradle.plugins.dbtools.database.util;

import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsPlugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.util.GUtil;

import javax.annotation.Nullable;
import java.io.File;
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
        Properties testProperties = injectedTestProperties();
        if (testProperties == null) {
            final ScriptHandler buildscript = project.getBuildscript().getRepositories().isEmpty() ? project.getRootProject().getBuildscript() : project.getBuildscript();
            return buildscript.getConfigurations().detachedConfiguration(dbutilsDependency(project));
        }

        String classpath = testProperties.getProperty("sktools_dbtools_classpath");
        // En trenger classpath for dbtools (db-tools)
        // disse ligger i egen modul
        return project.files((Object[]) classpath.split(File.pathSeparator)); //NB: for GradleRunner i debug mode
    }

    static Dependency dbutilsDependency(Project project) {
        Object dependencyNotation = Objects.requireNonNull(pluginProperties.getProperty("sktools_dbtools"), "Skal settes av byggesystem");
        return project.getDependencies().create(dependencyNotation);
    }

    /**
     * Test properties når man kjører tester, ellers null.
     */
    @Nullable
    static Properties injectedTestProperties() {
        InputStream stream = DbtoolsPlugin.class.getResourceAsStream("/DbtoolsPluginTest.properties");
        //dersom denne finnes på classpath kjører man tester
        return stream == null ? null : GUtil.loadProperties(stream);
    }



}
