package no.statkart.sktools.gradle.plugins.ideaextensions

import org.gradle.api.Project

/**
 * Convention og standard verdier.
 * Se {@code ideaExtensions(Closure)} for konfigurasjon.
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class IdeaExtensionsConvention {
    final Project project

    Collection<String> masks = ['*.iws', '*.ipr', '*.iml', '*.log']

    /**
     * Gyldige verdier og VCS systemer som intellij kjenner til som default er:
     *   'Perforce', 'CVS', 'SourceSafe', 'Subversion', 'TFS'
     *
     * @since 1.1
     */
    Map<String, String> vcsDirectoryMappings = new LinkedHashMap<String,String>(1)


    IdeaExtensionsConvention(Project project) {
        this.project = project

        //anngir Perforce som vcs for hele filstrukturen
        vcs('Perforce')
    }

    /**
     * Konfigurasjon-closure av plugin.
     */
    def ideaExtensions(Closure closure) {
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.delegate = this
        closure()
    }



    //metoder for bruk i konfigurasjon -->

    /**
     * Setter default VCS mapping
     * @since 1.1
     */
    void vcs(String vcsName) {
        vcsMapping('', vcsName)
    }

    /**
     * Legger til VCS mapping
     * @since 1.1
     */
    void vcsMapping(String path, String vcsName) {
        vcsDirectoryMappings[path] = vcsName
    }



}
