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
 * @author Tor Egil R. Strand
 */
class IdeaExtensionsPluginExtension {
    final Project project

    Set<String> ignoreMasks = ['*.iws', '*.ipr', '*.iml', '*.log']
    Set<File> ignorePaths = []

    /**
     * Gyldige verdier og VCS systemer som intellij kjenner til som default er:
     *   'Perforce', 'CVS', 'SourceSafe', 'Subversion', 'TFS'
     *
     * @since 1.1
     */
    Map<String, String> vcsDirectoryMappings = new LinkedHashMap<String,String>(1)

    /**
     * Angir om plugin-et skal opprette alle source-kataloger ved kjøring av ideaModule.
     * Standardverdi er <code>true</code> for bakoverkompatibilitet.
     *
     * @since 1.2
     */
    boolean createAllSourceDirs = true

    /**
     * Angir en valgfri fil som inneholder inspection-instillinger eksportert fra IntelliJ.
     *
     * @since 1.2
     */
    File inspectionsFile = null

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
