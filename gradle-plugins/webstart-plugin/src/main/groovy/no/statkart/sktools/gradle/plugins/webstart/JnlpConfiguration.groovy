package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.plugins.webstart.util.DependencyHelper
import org.apache.commons.lang.builder.EqualsBuilder
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

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
    protected final DependencyHelper jarDependencies;

    ResourcesConfiguration(JnlpConfiguration jnlp) {
        this.jnlp = jnlp;
        this.jarDependencies = new DependencyHelper(this.project);
    }

    private Project getProject() {
        return jnlp.getProject()
    }

    public DependencyHelper jars(Closure closure) {
        return jarDependencies.configure(closure);
    }

    /**
     * @see DependencyHelper#library(Object, Closure)
     * @since 1.2
     */
    public Dependency jars(Object notation, Closure notationConfigClosure) {
        return jarDependencies.library(notation, notationConfigClosure);
    }

    /**
     * @see DependencyHelper#library(Object...)
     * @since 1.2
     */
    public Dependency jars(Object... notations) {
        return jarDependencies.library(notations);
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

    // Fordi equals() brukes til up-to-date evaluering, så er ikke jarDependencies med i equals siden filavhengigheter
    // må/bør registreres for seg selv som Gradle-input. Dessuten er ikke en serialisert og deseralisert
    // DependencyHelper lik seg selv lenger.
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