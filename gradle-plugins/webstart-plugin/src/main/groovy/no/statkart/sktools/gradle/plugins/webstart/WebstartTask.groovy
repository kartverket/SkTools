package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.plugins.webstart.util.ArtifactMatcher
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.internal.ConventionTask

/**
 *
 * @author Leif Lislegård
 * @author Tor Egil R. Strand
 */
class WebstartTask extends ConventionTask {
    protected static Logger log = Logging.getLogger(WebstartTask.class);

    private List<JnlpConfiguration> jnlpConfigurations = new ArrayList<JnlpConfiguration>();
    private final ConfigurableFileCollection jarResources;

    private File destinationDir;

    String libDir = "lib";

    private String digest = null;

    WebstartTask() {
        jarResources = project.files()
    }

    public void jnlp(Closure config) {
        def jnlpConfiguration = new JnlpConfiguration(project)
        jnlpConfiguration.configure(config)
        jnlpConfigurations.add(jnlpConfiguration)
    }

    @InputFiles
    FileCollection getJarResources() {
        return jarResources
    }

    void jarResources(Object... files) {
        jarResources.from(files)
    }

    @Input
    List<JnlpConfiguration> getJnlpConfigurations() {
        return jnlpConfigurations
    }

    protected void setJnlpConfigurations(List<JnlpConfiguration> jnlpConfigurations) {
        this.jnlpConfigurations = jnlpConfigurations
    }

    @Input
    @Optional
    String getDigest() {
        return digest
    }

    File getDestinationDir() {
        return destinationDir != null ? destinationDir : new File(project.buildDir, 'webstart')
    }

    void setDigest(String digest) {
        this.digest = digest
    }

    void setDestinationDir(File destinationDir) {
        this.destinationDir = destinationDir
    }

    @OutputFiles
    FileCollection getJnlpFiles() {
        project.files({
            jnlpConfigurations.collect {
                new File(getDestinationDir(), it.jnlpFilename)
            }
        })
    }

    @TaskAction
    public void generate() {
        jnlpConfigurations.each {
            createJnlp(it)
        }
    }

    /**
     * Genererer <code>jnlp</code>fil.
     *
     * Filen blir delvis basert på template og delvis bygget opp i koden via {@link groovy.util.Node}.
     */
    public void createJnlp(JnlpConfiguration jnlp) {
        Node jnlpNode = new XmlParser().parse(getClass().getResourceAsStream('template.jnlp'))
        String digest = digest != null ? ('-' + digest) : ''

        jnlpNode.attributes().put('href', jnlp.jnlpFilename)
        jnlpNode.attributes().put('version', jnlp.version)

        Node informationNode = jnlpNode.information[0]
        informationNode.title[0].value = jnlp.title
        informationNode.vendor[0].value = jnlp.vendor
        informationNode.description[0].value = jnlp.description

        if (jnlp.homepage != null) {
            informationNode.homepage[0].attributes().put('href', jnlp.homepage)
        } else {
            informationNode.remove(informationNode.homepage[0]) //homepage must have an href if set
        }

        Node resourcesNode = new Node(jnlpNode, 'resources')

        jnlp.resources.runtimes?.each { JavaRuntimeConfiguration javaRuntime ->
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

        jarResources.files.each {File file ->
            ArtifactMatcher artifactMatcher = new ArtifactMatcher(file)

            String jarPath = getLibDir() + '/' + artifactMatcher.name + '.' + artifactMatcher.type

            Node jarNode = resourcesNode.appendNode('jar', [href: jarPath, version: artifactMatcher.version + digest])
            long size = file.length()   //0 if for some reasons the file cant be found
            if (size > 0L) {
                jarNode.attributes().put('size', size)
            }
        }

        jnlp.resources.systemProperties?.each { key, value ->
            resourcesNode.appendNode('property', [name: key, value: value])
        }

        if (jnlp.hasApplication()) {
            jnlpNode.appendNode('application-desc', ['main-class': jnlp.application.mainClass])
        }


        writeXml(new File(getDestinationDir(), jnlp.jnlpFilename), jnlpNode)
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
