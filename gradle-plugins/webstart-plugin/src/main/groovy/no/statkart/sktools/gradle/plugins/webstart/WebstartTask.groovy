package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.plugins.webstart.util.ArtifactMatcher
import no.statkart.sktools.gradle.plugins.webstart.util.JarSigner
import org.apache.commons.io.FileUtils
import org.apache.commons.io.comparator.SizeFileComparator
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.file.BaseDirFileResolver
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import no.statkart.sktools.gradle.plugins.webstart.util.FileHashIdent
import org.gradle.api.internal.ConventionTask

/**
 *
 * @author Leif Lislegård
 */
class WebstartTask extends ConventionTask {
    protected static Logger log = Logging.getLogger(WebstartTask.class);

    @Input
    Collection<WebstartClientConfiguration> clients = null

    @InputFile
    File keystoreFile = null

    @Input
    String alias = null

    String password = null


    /**
     * Ressurss-filer blir omdøpt og kopiert ut til lib kataloger.
     * Dersom det blir bygget SNAPSHOT versjoner, vil versjons-delen av filnavnet inneholde et timestamp for bygget.
     * Det er også tatt hensyn til skifte av sertifikat. For detaljer, se {@link #createFileNameForJar(File, ResourcesConfiguration, String) }
     * <p>
     * <p>
     *
     */
    @TaskAction
    def generate() {

        final Configuration configuration = project.getConfigurations().getByName(WebstartPlugin.CONFIGURATION_NAME)

        //genererer alle klienter
        getClients().each { WebstartClientConfiguration clientConfiguration ->

            String digest = "-unsigned"

            //henter ut jarfiler ifra deklarerte dependencies
            Map<ResourcesConfiguration, Collection<File>> jarsForResources = clientConfiguration.jnlp.resources.collectEntries() { ResourcesConfiguration resources ->
                List<Dependency> dependencies = resources.jarDependencies.getDependencies()
                Dependency[] dependencyArray = dependencies.toArray(new Dependency[dependencies.size()])
                return new MapEntry(resources, configuration.files(dependencyArray))
            }

            //signerer jarfiler
            if (clientConfiguration.signJars) {
                jarsForResources = signFiles(jarsForResources, clientConfiguration);
                digest = "-" + FileHashIdent.createChecksum(getKeystoreFile(), getAlias());
            }

            //kopierer inn jar-filer
            //renamer filnavn ihht konvensjon
            jarsForResources = copyFiles(jarsForResources, clientConfiguration, digest);

            //genererer og skriver ned jnlp-fil
            createJnlp(clientConfiguration, jarsForResources);

            //generering av version.xml
            appendVersionXml(clientConfiguration, jarsForResources);

        }

    }

    /**
     * signerer jar filer til cacheDir
     */
    protected Map<ResourcesConfiguration, Collection<File>> signFiles(Map<ResourcesConfiguration, Collection<File>> jarsForResources, WebstartClientConfiguration clientConfiguration) {
        JarSigner signer = new JarSigner(getProject().getRootProject().file('.cache/webstart/signed'));
        signer.setAnt(getAnt());
        signer.setCertificateFile(getKeystoreFile());
        signer.setAlias(getAlias());
        signer.setPassword(getPassword())

        return (Map<ResourcesConfiguration, Collection<File>>) jarsForResources.collectEntries { key, value ->
            signer.setJarfilesToSign(value)

            Set<File> signedJarFiles = signer.signJars().values()
            return new MapEntry(key, signedJarFiles)
        }
    }

    /**
     * Kopierer alle jarfiler dit de skal. <br />
     * <p>
     * Returnert map inneholder <code>value</code> collection av filer som er sortert etter synkende filstørrelse (størst først)
     * <p>
     * Se {@link #createFileNameForJar(File, ResourcesConfiguration, String) for mapping av filnavn
     */
    protected Map<ResourcesConfiguration, Collection<File>> copyFiles(Map<ResourcesConfiguration, Collection<File>> jarsForResources, WebstartClientConfiguration clientConfiguration, String digest) {

        //vi har en mange til mange relasjon mellom kilde og destinasjon for jar filer.
        HashMap<File, HashMap<ResourcesConfiguration, File>> destinationFilesForSourceFiles = new HashMap<File, HashMap<ResourcesConfiguration, File>>();
        jarsForResources.each() { ResourcesConfiguration resourcesConfiguration, Set<File> files ->
            if (!files.isEmpty()) {
                resourcesConfiguration.libDir.mkdirs()
                files.each { File file ->
                    HashMap<ResourcesConfiguration, File> destinationFiles = destinationFilesForSourceFiles.get(file)
                    if (destinationFiles == null) {
                        destinationFiles = new HashMap<ResourcesConfiguration, File>()
                        destinationFilesForSourceFiles.put(file, destinationFiles)
                    }
                    //beregner nytt navn for jar-filer
                    String newFileName = createFileNameForJar(file, resourcesConfiguration, digest)
                    destinationFiles.put(resourcesConfiguration, new File(resourcesConfiguration.libDir, newFileName))
                }
            }
        }

        //kopierer ut filer
        destinationFilesForSourceFiles.each() { File sourceFile, Map<ResourcesConfiguration, File> destinations ->
            //samler her alle destinasjonsfiler til et sett for å unngå unødvendig filkopiering (anntar at jar avhengighet er unike innen samme rc)
            HashSet<File> destinationFiles = new HashSet<File>()
            destinations.collect(destinationFiles) {key, value -> value}.each { File destinationFile ->
                log.info("... copying {}", destinationFile)
                FileUtils.copyURLToFile(sourceFile.toURI().toURL(), destinationFile);
            }
        }

        //generer map for kopierte filer
        HashMap<ResourcesConfiguration, Collection<File>> mappedJarsForResources = new HashMap<ResourcesConfiguration, Collection<File>>()
        jarsForResources.collectEntries(mappedJarsForResources) { ResourcesConfiguration resourcesConfiguration, Set<File> sourceFiles ->
            List<File> mappedFiles = new ArrayList<File>(sourceFiles.size())
            sourceFiles.collect(mappedFiles) { destinationFilesForSourceFiles.get(it).get(resourcesConfiguration) }
            Collections.sort(mappedFiles, SizeFileComparator.SIZE_REVERSE)
            log.lifecycle("Copied {} files to {}", mappedFiles.size(), resourcesConfiguration.libDir)
            new MapEntry(resourcesConfiguration, mappedFiles)
        }

        return mappedJarsForResources
    }

    private static String TIMESTAMP = null;
    protected static String getTimestamp() {
        if (TIMESTAMP == null) {
            TIMESTAMP = new Date().format('yyyy_MM_dd_HHmmss')
        }
        return TIMESTAMP;
    }


    /**
     * Steg for mapping av navn for ressurs-filer til lib katalog
     * <p>
     * Dersom jar filen inneholder 'SNAPSHOT' vil version få lagt til en timestamp-verdi. <br>
     * Versjonsfelt vil også få tillagt parameterisert {@code digest}. <br>
     */
    protected static String createFileNameForJar(File file, ResourcesConfiguration resourcesConfiguration, String digest) {
        String name = null
        String version = null
        String ext = ArtifactMatcher.getArtifactType(file)
        try {
            name = ArtifactMatcher.getArtifactName(file)
        } catch (Exception e) {
                name = file.getName()
        }
        try {
            version = ArtifactMatcher.getArtifactVersion(file)
        } catch (Exception e) {
            log.warn("Filename not parsable, parsing manifest for version {}", file)
            version = ArtifactMatcher.findImplementationVersionInManifest(file)
        }

        if (version == null) {
            log.error("Could not calculate version info from file {}", file)
            version = "unknown"
        }

        if (version.contains("SNAPSHOT") || name.contains("SNAPSHOT")) {
            if (!version.contains("SNAPSHOT")) {
                version += 'snapshot'
            }
            version += getTimestamp();
        }

        //legger til digest
        return "${name}__V${version}${digest}.${ext}"
    }

    /**
     * Gir deg ressursnavn til filer som er blit renamet via {@link #createFileNameForJar(File, ResourcesConfiguration, String)}
     */
    protected static String findJnlpResourceName(File file) {
        String value = file.getName()
        int idx = value.indexOf("__")
        if (idx != -1) {
            value = value.substring(0, idx)
        }
        String append = file.getName()
        idx = append.lastIndexOf('.')
        if (idx != -1) {
            value += append.substring(idx)    //legger til feks '.jar'
        }
        return value
    }

    /**
     *
     * Dersom version inneholder {@code 'SNAPSHOT'} legges et timestamp til versionen.
     */
    protected static String findJnlpResourceVersion(File file) {
        String value = file.getName()
        value = value.substring(value.indexOf("__V") + 3)
        int idx = value.indexOf("__")
        if (idx == -1) {
            idx = value.lastIndexOf('.')
        }
        value = value.substring(0, idx)
        return value
    }

    /**
     * "__O"
     * @return tom liste dersom ikke funnet.
     */
    protected static List<String> findJnlpResourceOS(File file) {
        return Collections.EMPTY_LIST;  //todo
    }
    /**
     * "__A"
     * @return tom liste dersom ikke funnet.
     */
    protected static List<String> findJnlpResourceArch(File file) {
        return Collections.EMPTY_LIST;  //todo
    }
    /**
     * "__L"
     * @return tom liste dersom ikke funnet.
     */
    protected static List<String> findJnlpResourceLocale(File file) {
        return Collections.EMPTY_LIST;  //todo
    }

    /**
     * Genererer <code>jnlp</code>fil.
     *
     * Filen blir delvis basert på template og delvis bygget opp i koden via {@link groovy.util.Node}.
     */
    static protected File createJnlp(WebstartClientConfiguration client, Map<ResourcesConfiguration, Collection<File>> jarsForResources) {
        File jnlpFile = client.getJnlpFile()
        JnlpConfiguration jnlp = client.jnlp

        jnlpFile.parentFile.mkdirs()
        BaseDirFileResolver filePathResolver = new BaseDirFileResolver(client.outputDir)

        Node jnlpNode = new XmlParser().parse(WebstartTask.class.getResourceAsStream('template.jnlp'))

        jnlpNode.attributes().put('href', client.jnlpFilePath)
        jnlpNode.attributes().put('version', '1.1')

        Node informationNode = jnlpNode.information[0]
        informationNode.title[0].value = jnlp.title
        informationNode.vendor[0].value = jnlp.vendor
        informationNode.description[0].value = jnlp.description

        if (jnlp.homepage != null) {
            informationNode.homepage[0].attributes().put('href', jnlp.homepage)
        } else {
            informationNode.remove(informationNode.homepage[0]) //homepage must have an href if set
        }

        jnlp.resources.each { ResourcesConfiguration resources ->
            Node resourcesNode = new Node(jnlpNode, 'resources')

            resources.runtimes?.each { JavaRuntimeConfiguration javaRuntime ->
                Node javaNode = resourcesNode.appendNode('j2se', [version: javaRuntime.version])
                if (javaRuntime.href != null) {
                    javaNode.attributes().put('href', javaRuntime.href)
                }
                if (javaRuntime.xms != null) {
                    javaNode.attributes().put('initial-heap-size', javaRuntime.xms)
                }
                if (javaRuntime.xmx != null) {
                    javaNode.attributes().put('max-heap-size', javaRuntime.xmx)
                }
                if (javaRuntime.vmArgs != null) {
                    javaNode.attributes().put('java-vm-args', javaRuntime.vmArgs)
                }
            }

            jarsForResources.get(resources)?.each { File file ->
                String jarPath = filePathResolver.resolveAsRelativePath(file.getParentFile()).replace('\\'.toCharacter(), '/'.toCharacter())
                jarPath += "/" + findJnlpResourceName(file)
                String version = findJnlpResourceVersion(file)
                Node jarNode = resourcesNode.appendNode('jar', [href: jarPath, version: version])
                long size = file.length()   //0 if for some reasons the file cant be found
                if (size > 0L) {
                    jarNode.attributes().put('size', size)
                }

                //todo: calculate main attribute
            }


            resources.systemProperties?.each { key, value ->
                resourcesNode.appendNode('property', [name: key, value: value])
            }

        }//end <resources>


        if (jnlp.hasApplication()) {
            jnlpNode.appendNode('application-desc', ['main-class': jnlp.application.mainClass])
        }


        writeXml(jnlpFile, jnlpNode)

        return jnlpFile
    }

    /**
     * Appender jar versjons informasjon (evt oppretter ny fil om nødvendig).
     *
     * <p>
     * <p>
     * Merk at jnlp og jnlp servlet ikke forholder seg til hva {@code versjon-id} er annet enn at den er unik.
     * Med andre ord kan en fint gå ann for en westart klient å hente ned en jar ressurs med lavere {@code versjon-id}.
     * Eneste krav er at {@code versjon-id} er unik.
     *
     */
    protected void appendVersionXml(WebstartClientConfiguration clientConfiguration, Map<ResourcesConfiguration, Collection<File>> jarsForResources) {

        jarsForResources.each { ResourcesConfiguration resourcesConfiguration, Collection<File> files ->
            if (!files.isEmpty()) {
                File versionFile = new File(resourcesConfiguration.libDir, 'version.xml')
                Node versionsNode = null
                if (versionFile.exists()) {
                    versionsNode = new XmlParser().parse(versionFile)
                } else {
                    versionsNode = new Node(null, 'jnlp-versions')
                }

                files.each { File file ->

                    versionsNode.children().findAll {it.file.text().trim() == file.getName()}.each {
                        log.debug("replacing version info for jar {}", file)
                        assert versionsNode.remove(it)
                    }

                    Node resourceNode = versionsNode.appendNode('resource')
                    Node patternNode = resourceNode.appendNode('pattern')
                    patternNode.appendNode('name', findJnlpResourceName(file))
                    patternNode.appendNode('version-id', findJnlpResourceVersion(file))
                    resourceNode.appendNode('file', file.getName())
                }


                writeXml(versionFile, versionsNode)
            }
        }
    }

    /**
     * For debug - writes xml to String
     */
    static protected String writeXmlToString(Node xml) {
        StringWriter writer = new StringWriter()
        writeXml(writer, xml)
        writer.toString()
    }

    /**
     * Writes xml to file
     */
    static protected void writeXml(File file, Node xml) {
        if (!file.exists()) {
            log.info("...creating {}", file)
        }

        Writer writer = new OutputStreamWriter(new FileOutputStream(file), 'UTF-8')
        writeXml(writer, xml)
        writer.close()

        if (log.isDebugEnabled()) {
            log.debug("...writing xml to {} :\n{}", file, writeXmlToString(xml))
        }

    }

    static protected void writeXml(Writer writer, Node xml) {
        XmlNodePrinter printer = new XmlNodePrinter(new PrintWriter(writer))
        printer.setPreserveWhitespace(true)
        printer.print(xml)
    }

}
