package no.statkart.sktools.gradle.plugins.ideaextensions

/**
 * todo: dokumentasjon
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class IdeaExtensionsConvention {
    Collection<String> masks = ['*.iws', '*.ipr', '*.iml', '*.log']

    /**
     * Konfigurasjon-closure av plugin.
     */
    def ideaExtensions(Closure closure) {
        closure.delegate = this
        closure()
    }
}
