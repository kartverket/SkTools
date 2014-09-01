package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.plugins.webstart.util.FileHashIdent
import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.util.ConfigureUtil

/**
 * Extension for webstart plugin.
 *
 * @author Tor Egil R. Strand
 */
class WebstartConvention {
    private final Project project
    private final NamedDomainObjectContainer<ClientConfiguration> clientContainer

    WebstartConvention(Project project) {
        this.project = project
        clientContainer = project.container(ClientConfiguration, new NamedDomainObjectFactory<ClientConfiguration>() {
            @Override
            ClientConfiguration create(String name) {
                return new ClientConfiguration(project, name)
            }
        })
    }

    void webstart(Closure config) {
        clientContainer.configure config
    }

    NamedDomainObjectContainer<ClientConfiguration> getWebstart() {
        return clientContainer
    }
}

class ClientConfiguration {
    private final Project project;
    private final String name;

    private SigningConfiguration signingConfiguration
    private Map<String, String> manifestAttributes = new LinkedHashMap<String, String>();
    private List<JnlpConfiguration> jnlpConfigurations = new ArrayList<JnlpConfiguration>();
    private ConfigurableFileCollection jarDependencies
    private ConfigurableFileCollection mainDependency

    /**
     * Angir relativ URL til jar-filer i forholdt til jnlp-filene.
     */
    String libDir = 'lib'

    /**
     * Angir om jnlp-filene skal legges rett til war-en. Sett til <code>false</code> dersom jnlp-filene skal prosesseres mer på ett eller annet vis.
     */
    boolean warJnlps = true

    ClientConfiguration(Project project, String name) {
        this.project = project
        this.name = name

        jarDependencies = project.files()
        mainDependency = project.files()
    }

    String getName() {
        return name
    }

    public void sign(File keystore, String alias, String password) {
        signingConfiguration = new SigningConfiguration(keystore, alias, password)
    }

    public void manifestAttribute(String name, String value) {
        manifestAttributes.put(name, value);
    }

    public void jnlp(Closure config) {
        JnlpConfiguration jnlpConfiguration = new JnlpConfiguration(project)
        jnlpConfiguration.configure config
        jnlpConfigurations.add(jnlpConfiguration)
    }

    public void libDir(String libDir) {
        this.libDir = libDir
    }

    public void jarDependencies(Object... files) {
        jarDependencies.from(files)
    }

    public void mainDependency(Object... files) {
        mainDependency.from(files)
    }

    SigningConfiguration getSigningConfiguration() {
        return signingConfiguration
    }

    Map<String, String> getManifestAttributes() {
        return manifestAttributes
    }

    List<JnlpConfiguration> getJnlpConfigurations() {
        return jnlpConfigurations
    }

    FileCollection getJarDependencies() {
        return jarDependencies
    }

    FileCollection getMainDependency() {
        return mainDependency
    }
}

class SigningConfiguration {
    private final File keystore
    private final String alias
    private final String password

    SigningConfiguration(File keystore, String alias, String password) {
        this.keystore = keystore
        this.alias = alias
        this.password = password
    }

    File getKeystore() {
        return keystore
    }

    String getAlias() {
        return alias
    }

    String getPassword() {
        return password
    }

    public String getDigest() throws Exception {
        return FileHashIdent.createChecksum(keystore, alias);
    }
}

class JnlpConfiguration implements Serializable {
    private final static long serialVersionUID = 1L;

    private transient Project project

    String jnlpFilename;

    String title;
    String vendor;
    String description;

    String homepage = null; //optional

    String version = null; //optional
    protected ApplicationConfiguration application = null;  //might be null

    protected final List<ResourcesConfiguration> resources = new ArrayList<ResourcesConfiguration>();

    private transient Closure withXml;

    JnlpConfiguration(Project project) {
        this.project = project
        jnlpFilename = project.name + '.jnlp'
    }

    Project getProject() {
        return project
    }

    public JnlpConfiguration title(String title) {
        this.title = title;
        return this;
    }

    public JnlpConfiguration jnlpFilename(String jnlpFilename) {
        this.jnlpFilename = jnlpFilename;
        return this;
    }

    public JnlpConfiguration vendor(String vendor) {
        this.vendor = vendor;
        return this;
    }

    public JnlpConfiguration description(String description) {
        this.description = description;
        return this;
    }

    public JnlpConfiguration homepage(String homepage) {
        this.homepage = homepage;
        return this;
    }

    /**
     * Version for the jnlp file/application.
     */
    public JnlpConfiguration version(Object version) {
        this.version = version.toString();
        return this;
    }

    public JnlpConfiguration applicationMainClass(String fqn) {
        getApplication().mainClass(fqn);
        return this;
    }

    boolean hasApplication() {
        return application != null;
    }

    public ApplicationConfiguration application(Closure config = null) {
        def app = getApplication()
        if (config != null) {
            app.configure(config)
        }
        return app
    }

    public ApplicationConfiguration getApplication() {
        if (application == null) {
            application = new ApplicationConfiguration(this);
        }
        return application;
    }

    /**
     * Adds a group of resources
     */
    public ResourcesConfiguration resources(Closure config = null) {
        ResourcesConfiguration resourcesConfiguration = new ResourcesConfiguration(this)
        resources.add(resourcesConfiguration);
        if (config != null) {
            resourcesConfiguration.configure(config)
        }
        return resourcesConfiguration;
    }

    protected List<ResourcesConfiguration> getResources() {
        return resources;
    }

    public void withXml(Closure closure) {
        withXml = closure
    }

    protected Closure getWithXml() {
        return withXml
    }

    /**
     * Config clause for a jnlp application.
     */
    protected JnlpConfiguration configure(Closure closure) {
        closure.setDelegate(this);
        closure.resolveStrategy = Closure.DELEGATE_FIRST;
        closure.call();
        return this;
    }

    boolean equals(o) {
        return EqualsBuilder.reflectionEquals(this, o);

    }

    int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}

class ApplicationConfiguration implements Serializable {
    private final static long serialVersionUID = 1L;
    protected final transient JnlpConfiguration jnlp;

    protected String mainClass;


    ApplicationConfiguration(JnlpConfiguration jnlp) {
        this.jnlp = jnlp;
    }

    public ApplicationConfiguration mainClass(String fqn) {
        mainClass = fqn;
        return this;
    }

    protected ApplicationConfiguration configure(Closure closure) {
        closure.setDelegate(this);
        closure.resolveStrategy = Closure.DELEGATE_FIRST;
        closure.call();
        return this;
    }

    boolean equals(o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}

/**
 * Represents the {@code <resource>} elements in a jnlp file.
 */
class ResourcesConfiguration implements Serializable {
    private final static long serialVersionUID = 1L;
    protected final transient JnlpConfiguration jnlp;

    protected final Map<String, Object> systemProperties = new LinkedHashMap();
    protected final List<JavaRuntimeConfiguration> runtimes = new ArrayList<JavaRuntimeConfiguration>();

    ResourcesConfiguration(JnlpConfiguration jnlp) {
        this.jnlp = jnlp;
    }

    private Project getProject() {
        return jnlp.getProject()
    }

    /**
     * Optional. Use this to pass on system properties to a webstart client.
     */
    public ResourcesConfiguration systemProperties(Map<String, Object> props) {
        systemProperties.putAll(props);
        return this;
    }

    /**
     * Convenience method for adding a java runtime.
     * <p>
     * Defaults <code>href</code> to official sun binaries as well as <code>xmx</code> to 128m
     */
    public ResourcesConfiguration javaRuntime(String version, String xms = null, String xmx = '128m', String href = 'http://java.sun.com/products/autodl/j2se') {
        javaRuntime().version(version).href(href).xms(xms).xmx(xmx);
        return this;
    }

    /**
     * Adds a java runtime declaration.
     * <p>
     * A jnlp file can list several runtimes the distribution can run on. The list should list the runtimes in preferred order with the most desired runtime first.
     */
    public JavaRuntimeConfiguration javaRuntime(Closure config = null) {
        return javaRuntime([:], config)
    }
    public JavaRuntimeConfiguration javaRuntime(Map properties, Closure config = null) {
        JavaRuntimeConfiguration javaRuntime = new JavaRuntimeConfiguration(this)
        runtimes.add(javaRuntime);
        if (properties != null) {
            ConfigureUtil.configureByMap(properties, javaRuntime)
        }
        if (config != null) {
            ConfigureUtil.configure(config, javaRuntime, false)
        }
        return javaRuntime;
    }

    /**
     * Adds a java-fx runtime declaration.
     */
    public JavaFxRuntimeConfiguration javaFxRuntime(Closure config = null) {
        return javaFxRuntime([:], config)
    }
    public JavaFxRuntimeConfiguration javaFxRuntime(Map properties, Closure config = null) {
        JavaFxRuntimeConfiguration runtimeConfiguration = new JavaFxRuntimeConfiguration(this)
        runtimes.add(runtimeConfiguration);
        if (properties != null) {
            ConfigureUtil.configureByMap(properties, runtimeConfiguration)
        }
        if (config != null) {
            ConfigureUtil.configure(config, runtimeConfiguration, false)
        }
        return runtimeConfiguration;
    }

    /**
     * Config clause for a collection of resources .
     */
    protected ResourcesConfiguration configure(Closure closure) {
        closure.setDelegate(this);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
        return this;
    }

    boolean equals(o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}

abstract class RuntimeConfiguration {
    private final static long serialVersionUID = 1L;
    def final transient ResourcesConfiguration resources;

    RuntimeConfiguration(ResourcesConfiguration resources) {
        this.resources = resources
    }
}

/**
 * Represents the {@code <jfx:javafx-runtime>} elements in a jnlp file
 */
class JavaFxRuntimeConfiguration extends RuntimeConfiguration implements Serializable {
    private final static long serialVersionUID = 1L;
    String version;
    String href = null;   //optional



    def JavaFxRuntimeConfiguration(ResourcesConfiguration resources) {
        super(resources)
    }

    public JavaFxRuntimeConfiguration version(String value) {
        version = value;
        return this;
    }

    public JavaFxRuntimeConfiguration href(String value) {
        href = value;
        return this;
    }


    boolean equals(o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}

/**
 * Represents the {@code <j2se>} elements in a jnlp file
 */
class JavaRuntimeConfiguration extends RuntimeConfiguration implements Serializable {
    private final static long serialVersionUID = 1L;
    String version;
    String href = null;   //optional
    /** initial-heap-size   */
    String xms = null;    //optional
    /** max-heap-size   */
    String xmx = null;    //optional
    String vmArgs = null; //optional



    JavaRuntimeConfiguration(ResourcesConfiguration resources) {
        super(resources)
    }

    public JavaRuntimeConfiguration version(String value) {
        version = value;
        return this;
    }

    public JavaRuntimeConfiguration href(String value) {
        href = value;
        return this;
    }

    public JavaRuntimeConfiguration xms(String value) {
        xms = value;
        return this;
    }

    public JavaRuntimeConfiguration xmx(String value) {
        xmx = value;
        return this;
    }

    public JavaRuntimeConfiguration vmArgs(String value) {
        vmArgs = value;
        return this;
    }

    boolean equals(o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}