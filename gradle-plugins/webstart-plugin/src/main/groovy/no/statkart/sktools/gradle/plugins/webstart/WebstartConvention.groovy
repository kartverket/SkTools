package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.plugins.webstart.util.DependencyHelper
import org.apache.commons.lang.builder.EqualsBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

/**
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class WebstartConvention implements Serializable {
    private final static long serialVersionUID = 1L;
    protected final transient Project project

    protected final Collection<WebstartClientConfiguration> clients = new ArrayList<WebstartClientConfiguration>()

    /**
     * Deklarer alle navn for {@link org.gradle.api.tasks.bundling.War war tasks}.
     * Tasks deklarert her vil få standard jarfiler for jnlp servlet bli kopiert ut.
     * <p>
     * Dersom listen er tom og {@code not null}, vil alle tasker av type {@link org.gradle.api.tasks.bundling.War} få med disse filene.
     *
     * @see WebstartPlugin#JNLP_SERVLET_JARS_TASK_NAME
     * @see #warTask(String...)
     * @see #getWarTasks()
     */
    protected Set<String> warTasks = null



    WebstartConvention(Project project) {
        this.project = project
    }

    /**
     * Config closure.
     * <ul>
     *     <li>{@link #client(Closure)} - definerer webstart klient
     * </ul>
     */
    def webstart(Closure closure) {
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.delegate = this
        closure.run()
    }

    /**
     * @since 1.1
     */
    public void client(Closure clientConfig) {
        clients.add(new WebstartClientConfiguration(this).configure(clientConfig))
    }

    /**
     * @since 1.1
     */
    public HashSet<String> getWarTasks() {
        if (warTasks == null) {
            warTasks = new HashSet<String>();
        }
        return warTasks
    }

    /**
     * @since 1.1
     */
    public void warTask(String... name) {
        name.each {
            getWarTasks().add(it)
        }
    }

    protected boolean hasWarTasks() {
        return warTasks != null;
    }


    boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}

class WebstartClientConfiguration implements Serializable {
    private final static long serialVersionUID = 1L;
    protected final transient WebstartConvention convention;
    protected final transient Project project;

    protected File outputDir   //optional
    protected String jnlpFilePath   //optional - relativ til outputDir

    protected boolean signJars = true //optional
    protected JnlpConfiguration jnlp = new JnlpConfiguration(this);



    protected WebstartClientConfiguration(WebstartConvention convention) {
        this.convention = convention
        this.project = convention.project

    }

    /**
     * Config clause for a webstart client.
     */
    protected WebstartClientConfiguration configure(Closure closure) {
        closure.setDelegate(this);
        closure.resolveStrategy = Closure.DELEGATE_FIRST;
        closure.call();
        return this;
    }


    public JnlpConfiguration jnlp(Closure config) {
        return jnlp.configure(config);
    }

    public JnlpConfiguration jnlp() {
        return jnlp;
    }


    public WebstartClientConfiguration outputPath(Object path) {
        outputDir = project.file(path);
        return this;
    }

    public WebstartClientConfiguration jnlpFile(def relativePath) {
        jnlpFilePath = relativePath;
        return this;
    }

    protected File getJnlpFile() {
        if (outputDir != null) {
            return project.file(project.relativePath(outputDir) + "/" + jnlpFilePath);
        } else {
            throw new GradleException("outputPath not defined!");
        }
    }



    public WebstartClientConfiguration signJars() {
        return signJars(true);
    }

    public WebstartClientConfiguration signJars(boolean value) {
        signJars = value;
        return this;
    }

    boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}


class JnlpConfiguration implements Serializable {
    private final static long serialVersionUID = 1L;
    protected final transient WebstartClientConfiguration client;

    protected String title;
    protected String vendor;
    protected String description;
    protected String homepage = null; //optional
    protected String version = null; //optional

    protected ApplicationConfiguration application = null;  //might be null
    protected final List<ResourcesConfiguration> resources = new ArrayList<ResourcesConfiguration>();


    JnlpConfiguration(WebstartClientConfiguration client) {
        this.client = client;
    }


    public JnlpConfiguration title(String title) {
        this.title = title;
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

    public ApplicationConfiguration application() {
        return getApplication();
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

    /**
     * Config clause for a jnlp application.
     */
    protected JnlpConfiguration configure(Closure closure) {
        closure.setDelegate(this);
        closure.resolveStrategy = Closure.DELEGATE_FIRST;
        closure.call();
        return this;
    }


    boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}

class ApplicationConfiguration implements Serializable {
    private final static long serialVersionUID = 1L;
    protected final transient JnlpConfiguration client;

    protected String mainClass;


    ApplicationConfiguration(JnlpConfiguration client) {
        this.client = client;
    }

    public ApplicationConfiguration mainClass(String fqn) {
        mainClass = fqn;
        return this;
    }

    boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}

/**
 * Represents the {@code <resource>} elements in a jnlp file.
 */
class ResourcesConfiguration implements Serializable {
    private final static long serialVersionUID = 1L;
    protected final transient JnlpConfiguration jnlp;
    protected final transient Project project;

    protected final Map<String, Object> systemProperties = new LinkedHashMap();
    protected final List<JavaRuntimeConfiguration> runtimes = new ArrayList<JavaRuntimeConfiguration>();
    protected final DependencyHelper jarDependencies;
    protected String libPath;

    ResourcesConfiguration(JnlpConfiguration jnlp) {
        this.jnlp = jnlp;
        this.project = jnlp.client.project;
        this.jarDependencies = new DependencyHelper(this.project);
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
     * Optional spesification of path to lib-directory. <br />
     * The path is relative to {@link WebstartClientConfiguration#outputPath(Object)
     */
    public ResourcesConfiguration libPath(String path) {
        this.libPath = path
        return this;
    }

    protected File getLibDir() {
        if (jnlp.client.outputDir != null) {
            String basePath = project.relativePath(jnlp.client.outputDir);
            return  project.file("${basePath}/${libPath}");
        } else {
            throw new GradleException("outputPath for client not defined! client: ${jnlp.title} (${jnlp.client.jnlpFilePath})");
        }
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

}

/**
 * Represents the {@code <java> or <j2se>} elements in a jnlp file
 */
class JavaRuntimeConfiguration implements Serializable {
    private final static long serialVersionUID = 1L;
    protected final transient ResourcesConfiguration resources;

    protected String version;
    protected String href = null;   //optional
    /** initial-heap-size   */
    protected String xms = null;    //optional
    /** max-heap-size   */
    protected String xmx = null;    //optional
    protected String vmArgs = null; //optional


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

}