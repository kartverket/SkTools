package no.statkart.sktools.gradle.plugins.webstart

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.*

import java.nio.charset.StandardCharsets

/**
 *
 * @author Leif Lislegård
 * @author Tor Egil R. Strand
 */
class WebstartTask extends ConventionTask {
    protected static final Logger logger = Logging.getLogger(WebstartTask.class);

    private List<JnlpConfiguration> jnlpConfigurations = new ArrayList<JnlpConfiguration>();
    private final ConfigurableFileCollection jarResources;
    private final ConfigurableFileCollection mainJar;

    private File destinationDir;

    String libDir = "lib";

    private String digest = null;

    WebstartTask() {
        jarResources = project.files()
        mainJar = project.files()
    }

    public void jnlp(Closure config) {
        def jnlpConfiguration = new JnlpConfiguration(project)
        jnlpConfiguration.configure(config)
        getJnlpConfigurations().add(jnlpConfiguration)
    }

    @InputFiles
    FileCollection getJarResources() {
        return jarResources
    }

    void jarResources(Object... files) {
        jarResources.from(files)
    }

    @InputFiles
    FileCollection getMainJar() {
        return mainJar
    }

    void mainJar(Object... files) {
        mainJar.from(files)
    }

    @Nested
    List<JnlpConfiguration> getJnlpConfigurations() {
        return jnlpConfigurations
    }

    void setJnlpConfigurations(List<JnlpConfiguration> jnlpConfigurations) {
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
            getJnlpConfigurations().collect {
                new File(getDestinationDir(), it.jnlpFilename)
            }
        })
    }

    @TaskAction
    public void generate() {
        getJnlpConfigurations().each {
            createJnlp(it)
        }
    }

    /**
     * Genererer <code>jnlp</code>fil.
     *
     * Filen blir delvis basert på template og delvis bygget opp i koden via {@link groovy.util.Node}.
     */
    public void createJnlp(JnlpConfiguration jnlp) {
        this.getClass().getResource('template.jnlp').withInputStream { jnlpTemplateStream ->  //groovy way of handling streams
            Node jnlpNode = new XmlParser().parse(jnlpTemplateStream)
            jnlpNode.attributes().put('xmlns:jfx', 'http://javafx.com')

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

            jnlp.resources.each { ResourcesConfiguration resources ->
                jnlpNode.append(JnlpSyntaxUtil.createResourcesElement(resources))
            }

            if (jnlp.resources.isEmpty()) {
                logger.info "Adding empty resources element to jnlp ..."
                jnlpNode.append(JnlpSyntaxUtil.createResourcesElement(null))
            }

            //adds all jars for dependencies to very first <resources> element in jnlp
            JnlpSyntaxUtil.appendJarElementForAllDependencies(jnlpNode.resources[0], getMainJar().files, getJarResources().files, getLibDir(), getDigest() != null ? ('-' + getDigest()) : '')


            if (jnlp.hasApplication()) {
                def applicationDescNode = jnlpNode.appendNode('application-desc', ['main-class': jnlp.application.mainClass])
                for (String arg : jnlp.application.args) {
                    applicationDescNode.appendNode('argument', arg)
                }
            }

            writeXml(new File(getDestinationDir(), jnlp.jnlpFilename), jnlpNode, jnlp.withXml)
        }
    }

    /**
     * Writes xml to file, applying withXml transform if set
     */
    static protected void writeXml(File file, Node xml, Closure withXml) {
        file.getParentFile().mkdirs()
        def writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file)), StandardCharsets.UTF_8)
        writeXml(writer, xml, withXml)
        writer.close()
    }

    static protected void writeXml(Writer writer, Node xml, Closure withXml) {
        Object transformer = findXmlTransformer()
        if (withXml != null) {
            transformer.addAction(withXml)
        }
        transformer.transform(xml, writer)
    }

    /**
     * Finner XmlTransformer, som ligger i forskjellige pakker avhengig av Gradle-versjon
     */
    static private Object findXmlTransformer() {
        Class transformerClass;
        try {
            transformerClass = Class.forName('org.gradle.internal.xml.XmlTransformer') //gradle 2.2
        } catch (ClassNotFoundException ignored) {
            transformerClass = Class.forName('org.gradle.api.internal.xml.XmlTransformer') //gradle 2.1
        }

        return transformerClass.getConstructor(new Class[0]).newInstance()
    }

    public Logger getLogger() {
        return logger;
    }
}
