package no.statkart.sktools.gradle.plugins.wsdocgen

import org.gradle.api.file.FileCollection

/**
 * todo: dokumentasjon
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class WsDocGenPluginConvention {
    String sourceDir
    String lookupPath
    FileCollection classpath
    String includePattern

    def wsDoc(Closure closure) {
        closure.delegate = this
        closure()
    }

    /**
     * @depricated since 1.0 - bruk heller {@link #wsDoc(Closure)}.
     */
    def wsdlDoc(Closure closure) {
        println 'wsdlDoc(Closure) is now depricated - use wsDoc(Closure) instead!'
        return wsdlDoc(closure)
    }
}
