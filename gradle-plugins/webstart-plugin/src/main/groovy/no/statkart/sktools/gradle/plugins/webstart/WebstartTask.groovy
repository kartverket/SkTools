package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.plugins.webstart.util.ArtifactMatcher
import org.gradle.api.GradleException
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
        jnlpConfigurations.add(jnlpConfiguration)
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
        String digest = getDigest() != null ? ('-' + getDigest()) : ''

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

        Set<File> mainJarFiles = mainJar.files // Disse er potensielt usignert, og kan derfor ikke brukes direkte
        Set<File> allJarFiles = jarResources.files // Dette er de jar-filene som skal brukes, både main og de andre
        Set<File> nonMainJarFiles // Dette er alle jar-filer som ikke skal merkes som main

        if (mainJarFiles.size() > 1) {
            throw new GradleException('There can only be one main jar. ' + mainJarFiles)
        } else if (mainJarFiles.size() > 0) {
            File unsignedFile = mainJarFiles.iterator().next()

            File mainJarFile = allJarFiles.find { it.name == unsignedFile.name }
            nonMainJarFiles = allJarFiles - mainJarFiles

            ArtifactMatcher artifactMatcher = new ArtifactMatcher(mainJarFile)

            String jarPath = getLibDir() + '/' + artifactMatcher.name + '.' + artifactMatcher.type

            Node jarNode = resourcesNode.appendNode('jar', [href: jarPath, version: artifactMatcher.version + digest, main: 'true'])
            long size = mainJarFile.length()   //0 if for some reasons the file cant be found
            if (size > 0L) {
                jarNode.attributes().put('size', size)
            }
        } else {
            nonMainJarFiles = allJarFiles
        }

        nonMainJarFiles.each {File file ->
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


        writeXml(new File(getDestinationDir(), jnlp.jnlpFilename), jnlpNode, jnlp.withXml)
    }

    /**
     * Writes xml to file, applying withXml transform if set
     */
    static protected void writeXml(File file, Node xml, Closure withXml) {
        writeXml(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file)), 'UTF-8'), xml, withXml)
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
     * @return
     */
    static private Object findXmlTransformer() {
        Class transformerClass;
        try {
            transformerClass = Class.forName('org.gradle.api.internal.xml.XmlTransformer')
        } catch (ClassNotFoundException ignored) {
            transformerClass = Class.forName('org.gradle.api.internal.XmlTransformer')
        }

        return transformerClass.newInstance()
    }
}
