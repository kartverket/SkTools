package no.statkart.sktools.gradle.plugins.weblogic.wswar

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
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
 * @deprecated since 1.2 - da denne er blitt overflødig
 */
class WeblogicWsWarConvention {

    final protected Project project


    WeblogicWsWarConvention(JavaPluginConvention javaConvention) {
        this.project = javaConvention.project;
    }

    /**
     * Konfigurasjon av convention skjer her.
     * @since 1.0
     */
    def weblogicWsWar(Closure closure) {
        logDeprecation('weblogicWsWar(closure)', 'warWeblogic task')
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.delegate = this
        closure()
    }

    /**
     * Konfigurerer source set for plugin
     * @since 1.1
     * @deprecated since 1.2
     */
    private def sourceSet(Closure closure) {
        logDeprecation('sourceSet(closure)', 'weblogic sourceSet')
        ConfigureUtil.configure(closure, getSourceSet());
        return this
    }



    private SourceSet getSourceSet() {
        JavaPluginConvention javaConvention = project.getConvention().getPlugins().get("java");
        return javaConvention.sourceSets.getByName(WeblogicWsWarPlugin.WEBLOGIC_SOURCE_SET_NAME);
    }

    private static void logDeprecation(String oldSyntax, String newSyntax) {
        println "${oldSyntax} in ${WeblogicWsWarConvention.class.simpleName} is now deprecated \n\t\t-use ${newSyntax} instead!"
    }
}
