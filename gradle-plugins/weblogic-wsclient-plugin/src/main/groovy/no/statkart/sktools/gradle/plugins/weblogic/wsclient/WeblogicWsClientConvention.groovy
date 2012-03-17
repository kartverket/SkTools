package no.statkart.sktools.gradle.plugins.weblogic.wsclient

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.artifacts.Dependency

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.tasks.util.PatternSet
import org.apache.commons.lang.builder.EqualsBuilder

/**
 *
 * @since 1.1
 * @author Leif Lislegård
 */
class WeblogicWsClientConvention {
    protected final Project project
    protected final SourceSet sourceSet

    protected final Collection<WebServiceConfig> webService = new ArrayList<WebServiceConfig>()
    protected File genDir

    WeblogicWsClientConvention(Project project, SourceSet sourceSet) {
        this.project = project
        this.sourceSet = sourceSet
        genDir('gen/weblogic/wsclient')
    }

    /**
     * Config closure
     * @since 1.0
     */
    def weblogicWsClient(Closure closure) {
        closure.delegate = this
        closure()
    }

    /**
     * Legger til konfigurasjon for en service.
     * @since 1.1
     */
    def webService(Closure closure) {
        webService.add(
                new WebServiceConfig(this).name("webService Closure#${webService.size() + 1}").configure(closure)
        )
    }

    /**
     * Bestemmer katalog for genererte filer.
     * @since 1.1
     */
    def File genDir(Object path) {
        genDir = project.file(path)
    }

    public File getGenDir() {
        return genDir
    }

    /**
     * Konfigurerer source set for plugin
     * @since 1.1
     */
    def sourceSet(Closure closure) {
        closure.delegate = sourceSet
        closure()
    }

    /**
     * @depricated since 1.0 - bruk heller {@link #weblogicWsClient(Closure)}.
     */
    def wsClient(Closure closure) {
        println 'wsClient(Closure) is now depricated - use weblogicWsClient(Closure) instead!'
        return weblogicWsClient(closure)
    }

}

/**
 * @since 1.1
 */
class WebServiceConfig implements Serializable {
    private final WeblogicWsClientConvention convention;

    protected String name;
    protected FileCollection schemaFiles;
    protected Dependency dependency;
    protected PatternSet matching;
    protected ExceptionConfig exception;


    WebServiceConfig(WeblogicWsClientConvention convention) {
        this.convention = convention
    }


    private void setDefaults() {
        if (schemaFiles == null) {
            if (matching == null) {
                matching = new PatternSet(includes: ['**/*.wsdl', '**/*.xsd'], caseSensitive: false)
            }
        }
    }

    protected WebServiceConfig configure(Closure closure) {
        closure.delegate = this
        closure()
        setDefaults()
        return this
    }

    /**
     * Optionalt navn for identifikasjon.
     */
    public WebServiceConfig name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Optional spesifisering av wsdl filer dersom en ønsker overstyring av default verdier.
     *
     * @see Project#files(Object... paths)
     */
    public WebServiceConfig schemaFiles(Object... paths) {
        FileCollection fileCollection = convention.project.files(paths)
        if (schemaFiles == null) {
            schemaFiles = new UnionFileCollection(fileCollection)
        } else {
            schemaFiles.add(fileCollection)
        }
        return this
    }

    /**
     * Optional spesifisering av wsdl filer dersom en ønsker overstyring av default verdier.
     */
    public WebServiceConfig schemaFiles(Closure closure) {
        closure.setDelegate(convention) //eksponerer med dette bla convention sin 'project' property
        return schemaFiles(closure())
    }



    /**
     * Legger til en dependency der {@code dependencyNotation} er på formen beskrevet i {@link org.gradle.api.artifacts.dsl.DependencyHandler}
     */
    public WebServiceConfig dependency(Closure dependencyNotatonClosure) {
        dependencyNotatonClosure.delegate = convention.project.getDependencies()
        dependency(dependencyNotatonClosure())
        return this
    }


    public void dependency(Object notation) {
        //legger til som default dependency - dette for at man senere kan lese ut war fil..
        dependency = convention.project.dependencies.add(Dependency.DEFAULT_CONFIGURATION, notation)
    }

    /**
     * Optional samling av exception til felles pakke
     */
    public WebServiceConfig exceptionReusePackage(String packageOrPath) {
        getException().packageOrPathString = packageOrPath;
        return this
    }


    private ExceptionConfig getException() {
        if (exception == null) {
            exception = new ExceptionConfig(this);
        }
        return exception
    }

    public String toString() {
        return (name != null) ? name : getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }


    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}

/**
 * Component
 * @since 1.1
 */
class ExceptionConfig implements Serializable {
    private final WebServiceConfig convention;

    protected String packageOrPathString
    protected PatternSet exceptionFilePatternSet = new PatternSet(includes: ['**/*Exception.java'])



    protected ExceptionConfig(WebServiceConfig convention) {
        this.convention = convention
    }



    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}