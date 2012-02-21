package no.statkart.sktools.gradle.plugins.weblogic.wsclient

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

/**
 * todo: dokumentasjon
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class WsClientPluginConvention {
    FileCollection weblogicLibraries
    List<Map<String, String>> fixExceptionsFor;

    File wsTargetDir
    File wsResourcesDir

    WsClientPluginConvention(Project project) {
        wsTargetDir = project.file('build/generated/main/java')
        wsResourcesDir = project.file('build/generated/main/resources')
    }

    def weblogicWsClient(Closure closure) {
        closure.delegate = this
        closure()
    }


    /**
     * @depricated since 1.0 - bruk heller {@link #weblogicWsClient(Closure)}.
     */
    def wsClient(Closure closure) {
        println 'wsClient(Closure) is now depricated - use weblogicWsClient(Closure) instead!'
        return weblogicWsClient(closure)
    }

}
