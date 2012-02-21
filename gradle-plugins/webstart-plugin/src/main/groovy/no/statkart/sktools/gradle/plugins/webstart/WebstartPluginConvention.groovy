package no.statkart.sktools.gradle.plugins.webstart

/**
 * todo: dokumentasjon
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class WebstartPluginConvention {
    String targetDir
    Closure jnlpTranslation
    String jnlpFileName
    boolean includeSelfJar = false

    def webstart(Closure closure) {
        closure.delegate = this
        closure()
    }
}