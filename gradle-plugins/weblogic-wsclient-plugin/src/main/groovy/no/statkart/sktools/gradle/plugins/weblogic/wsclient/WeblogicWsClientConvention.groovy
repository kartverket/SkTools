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
 *
 * ps. bruk av transient felter for å styre hva som ikke skal persisteres ved gradles beregning av up to date ved depends on.
 *
 * @since 1.1
 * @author Leif Lislegård
 */
class WeblogicWsClientConvention {
    protected final transient Project project
    protected final transient SourceSet sourceSet

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
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
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
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.delegate = sourceSet
        closure()
    }

    /**
     * @depricated since 1.0 - bruk heller {@link #weblogicWsClient(Closure)}.
     */
    @Deprecated
    def wsClient(Closure closure) {
        println 'wsClient(Closure) is now depricated - use weblogicWsClient(Closure) instead!'
        return weblogicWsClient(closure)
    }

}

/**
 * @since 1.1
 */
class WebServiceConfig implements Serializable {
    private final static long serialVersionUID = 1L;
    private final WeblogicWsClientConvention convention;

    protected String name;
    protected FileCollection schemaFiles;
    protected transient Dependency baseWar;
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
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
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
     * Alternativ spesifisering av wsdl filer dersom en ønsker overstyring av default verdier.
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
     * Alternativ spesifisering av wsdl filer dersom en ønsker overstyring av default verdier.
     */
    public WebServiceConfig schemaFiles(Closure closure) {
        closure.setDelegate(convention) //eksponerer med dette bla convention sin 'project' property
        return schemaFiles(closure())
    }



    /**
     * Bestemmer hvor wsdl schema befinner seg. Benytt denne evt sammen med {@link #matching} for filtrerinv av schema-filer.
     *
     * Legger til en baseWar der {@code dependencyNotation} er på formen beskrevet i {@link org.gradle.api.artifacts.dsl.DependencyHandler}
     */
    public WebServiceConfig baseWar(Closure dependencyNotatonClosure) {
        dependencyNotatonClosure.setResolveStrategy(Closure.DELEGATE_FIRST);
        dependencyNotatonClosure.delegate = convention.project.getDependencies()
        baseWar(dependencyNotatonClosure())
        return this
    }


    public void baseWar(Object notation) {
        //legger baseWar til 'default' configuration - dette for at man senere kan lese ut innhold i war fil..
        baseWar = convention.project.dependencies.add(Dependency.DEFAULT_CONFIGURATION, notation)
    }

    /**
     * @deprecated since 1.2
     */
    @Deprecated
    public WebServiceConfig dependency(Closure dependencyNotatonClosure) {
        println 'dependency(*) is now depricated - use baseWar(*) instead!'
        return baseWar(dependencyNotatonClosure)
    }

    /**
     * @deprecated since 1.2
     */
    @Deprecated
    public void dependency(Object notation) {
        println 'dependency(*) is now depricated - use baseWar(*) instead!'
        baseWar(notation)
    }


    /**
     * Optional samling av exception til felles pakke
     */
    public WebServiceConfig exceptionReusePackage(String packageOrPath) {
        getOrCreateException().packageOrPathString = packageOrPath;
        return this
    }


    private ExceptionConfig getOrCreateException() {
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
    private final static long serialVersionUID = 1L;
    private final WebServiceConfig convention;

    protected String packageOrPathString
    protected PatternSet exceptionFilePatternSet = new PatternSet(includes: ['**/*Exception.java', '**/*FaultInfo.java']) //disse henger sammen og blir relokalisert/slått sammen



    protected ExceptionConfig(WebServiceConfig convention) {
        this.convention = convention
    }



    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}