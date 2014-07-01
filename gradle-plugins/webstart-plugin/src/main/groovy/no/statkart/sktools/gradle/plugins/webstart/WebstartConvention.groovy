package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.plugins.webstart.util.FileHashIdent
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection

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

    protected ResourcesConfiguration resources;

    private transient Closure withXml;

    JnlpConfiguration(Project project) {
        this.project = project
        jnlpFilename = project.name + '.jnlp'
        resources = new ResourcesConfiguration(this)
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

    public ResourcesConfiguration resources(Closure config) {
        return resources.configure(config)
    }

    public ResourcesConfiguration getResources() {
        return resources
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
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        JnlpConfiguration that = (JnlpConfiguration) o

        if (application != that.application) return false
        if (description != that.description) return false
        if (homepage != that.homepage) return false
        if (jnlpFilename != that.jnlpFilename) return false
        if (resources != that.resources) return false
        if (title != that.title) return false
        if (vendor != that.vendor) return false
        if (version != that.version) return false

        return true
    }

    int hashCode() {
        int result
        result = (jnlpFilename != null ? jnlpFilename.hashCode() : 0)
        result = 31 * result + (title != null ? title.hashCode() : 0)
        result = 31 * result + (vendor != null ? vendor.hashCode() : 0)
        result = 31 * result + (description != null ? description.hashCode() : 0)
        result = 31 * result + (homepage != null ? homepage.hashCode() : 0)
        result = 31 * result + (version != null ? version.hashCode() : 0)
        result = 31 * result + (application != null ? application.hashCode() : 0)
        result = 31 * result + (resources != null ? resources.hashCode() : 0)
        return result
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
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ApplicationConfiguration that = (ApplicationConfiguration) o

        if (mainClass != that.mainClass) return false

        return true
    }

    int hashCode() {
        return (mainClass != null ? mainClass.hashCode() : 0)
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
        runtime().version(version).href(href).xms(xms).xmx(xmx);
        return this;
    }

    /**
     * Adds a runtime declaration.
     * <p>
     * A jnlp file can list several runtimes the distribution can run on. The list should list the runtimes in preferred order with the most desired runtime first.
     */
    public JavaRuntimeConfiguration runtime(Closure config = null) {
        JavaRuntimeConfiguration javaRuntime = new JavaRuntimeConfiguration(this)
        runtimes.add(javaRuntime);
        if (config != null) {
            javaRuntime.configure(config)
        }
        return javaRuntime;
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
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ResourcesConfiguration that = (ResourcesConfiguration) o

        if (runtimes != that.runtimes) return false
        if (systemProperties != that.systemProperties) return false

        return true
    }

    int hashCode() {
        int result
        result = systemProperties.hashCode()
        result = 31 * result + runtimes.hashCode()
        return result
    }
}

/**
 * Represents the {@code <java> or <j2se>} elements in a jnlp file
 */
class JavaRuntimeConfiguration implements Serializable {
    private final static long serialVersionUID = 1L;
    protected final transient ResourcesConfiguration resources;

    String version;
    String href = null;   //optional
    /** initial-heap-size   */
    String xms = null;    //optional
    /** max-heap-size   */
    String xmx = null;    //optional
    String vmArgs = null; //optional


    protected JavaRuntimeConfiguration(ResourcesConfiguration resources) {
        this.resources = resources;
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

    /**
     * Config clause for a runtime declaration.
     */
    protected JavaRuntimeConfiguration configure(Closure closure) {
        closure.setDelegate(this);
        closure.resolveStrategy = Closure.DELEGATE_FIRST;
        closure.call();
        return this;
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        JavaRuntimeConfiguration that = (JavaRuntimeConfiguration) o

        if (href != that.href) return false
        if (version != that.version) return false
        if (vmArgs != that.vmArgs) return false
        if (xms != that.xms) return false
        if (xmx != that.xmx) return false

        return true
    }

    int hashCode() {
        int result
        result = (version != null ? version.hashCode() : 0)
        result = 31 * result + (href != null ? href.hashCode() : 0)
        result = 31 * result + (xms != null ? xms.hashCode() : 0)
        result = 31 * result + (xmx != null ? xmx.hashCode() : 0)
        result = 31 * result + (vmArgs != null ? vmArgs.hashCode() : 0)
        return result
    }
}