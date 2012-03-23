package no.statkart.sktools.gradle.plugins.weblogic.wswar

import org.gradle.api.file.FileCollection
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import no.statkart.sktools.gradle.plugins.weblogic.WeblogicBasePlugin
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.util.ConfigureUtil

/**
 * Konvensjon for plugin.
 *
 * Se {@code #weblogicWsWar(Closure) } for konfigurasjon.
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class WeblogicWsWarConvention {

    final protected Project project
    final protected JavaPluginConvention javaConvention


    WeblogicWsWarConvention(JavaPluginConvention javaConvention) {
        this.project = javaConvention.project;
        this.javaConvention = javaConvention;
    }

    /**
     * Konfigurasjon av convention skjer her.
     * @since 1.0
     */
    def weblogicWsWar(Closure closure) {
        closure.delegate = this
        closure()
    }

    /**
     * Konfigurerer source set for plugin
     * @since 1.1
     */
    def sourceSet(Closure closure) {
        ConfigureUtil.configure(closure, getSourceSet());
//        closure.delegate = getSourceSet()
//        closure()
        return this
    }


    /**
     * @depricated since 1.0
     * @see #sourceSet
     */
    @Deprecated
    private FileCollection classpath
    void setClasspath(FileCollection classpath) {
        logDeprecation('setClasspath(path)', 'configurations.weblogicCompile clause')
        project.getConfigurations().getByName(getSourceSet().getCompileConfigurationName()).getDependencies().add(
                new DefaultSelfResolvingDependency(classpath)
        );
    }

    /**
     * @depricated since 1.0
     * @see #sourceSet
     */
    @Deprecated
    private FileCollection weblogicLibraries
    void setWeblogicLibraries(FileCollection classpath) {
        logDeprecation('setWeblogicLibraries(path)', 'configurations.weblogic clause')
        project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_CONFIGURATION_NAME).getDependencies().add(
                new DefaultSelfResolvingDependency(classpath)
        );
    }




    /**
     * @depricated since 1.0
     * @see #sourceSet
     */
    @Deprecated
    private String sourceDir
    void setSourceDir(Object path) {
        logDeprecation('setSourceDir(path)', 'sourceSet.java.srcDir')
        getSourceSet().getJava().srcDir(path)
        //alt:
//        JavaPluginConvention javaPluginConvention = project.getConvention().getPlugins().get("java")
//        javaPluginConvention.getSourceSets().getByName(WeblogicWsWarPlugin.WEBLOGIC_SOURCE_SET_NAME).getJava().srcDir(path)
    }

    /**
     * @depricated since 1.0
     * @see #sourceSet
     */
    @Deprecated
    private String webSourceDir
    void setWebSourceDir(Object path) {
        logDeprecation('setWebSourceDir(path)', 'sourceSet.resources.srcDir')
        getSourceSet().getResources().srcDir(path)
        //alt:
//        JavaPluginConvention javaPluginConvention = project.getConvention().getPlugins().get("java")
//        javaPluginConvention.getSourceSets().getByName(WeblogicWsWarPlugin.WEBLOGIC_SOURCE_SET_NAME).getResources().srcDir(path)
    }

    /**
     * @depricated since 1.0
     * @see #weblogicWsWar(Closure)
     */
    @Deprecated
    def statKartWeblogicWsWar(Closure closure) {
        logDeprecation('statKartWeblogicWsWar(Closure)', 'weblogicWsWar(Closure)')
        return weblogicWsWar(closure)
    }


    public SourceSet getSourceSet() {
        return javaConvention.sourceSets.getByName(WeblogicWsWarPlugin.WEBLOGIC_SOURCE_SET_NAME);
    }

    private static void logDeprecation(String oldSyntax, String newSyntax) {
        println "${oldSyntax} in ${WeblogicWsWarConvention.class.simpleName} is now deprecated \n\t\t-use ${newSyntax} instead!"
    }
}
