package no.statkart.sktools.gradle.plugins.weblogic.wswar

import org.gradle.api.file.FileCollection

/**
 * todo: dokumentasjon
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class WeblogicWsWarPluginConvention {
    String sourceDir
    String webSourceDir
    FileCollection classpath
    FileCollection weblogicLibraries

    def weblogicWsWar(Closure closure) {
        closure.delegate = this
        closure()
    }

    /**
     * @depricated since 1.0 - bruk heller {@link #weblogicWsWar(Closure)}.
     */
    def statKartWeblogicWsWar(Closure closure) {
        println 'statKartWeblogicWsWar(Closure) is now depricated - use weblogicWsWar(Closure) instead!'
        return weblogicWsWar(closure)
    }
}
