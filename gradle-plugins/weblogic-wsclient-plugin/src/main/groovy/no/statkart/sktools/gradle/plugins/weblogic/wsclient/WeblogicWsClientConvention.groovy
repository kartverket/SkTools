package no.statkart.sktools.gradle.plugins.weblogic.wsclient

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.util.PatternSet

/**
 *
 *
 * ps. bruk av transient felter for å styre hva som ikke skal persisteres ved gradles beregning av up to date ved depends on.
 *
 * @since 1.1
 * @author Leif Lislegård
 * @author Tor Egil R. Strand
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class WeblogicWsClientConvention {
    protected final transient Project project

    protected final NamedDomainObjectContainer<WebServiceConfig> webService;
    protected def genDir

    WeblogicWsClientConvention(Project project) {
        this.project = project
        webService = project.container(WebServiceConfig) { String name ->
            new WebServiceConfig(this, name)
        }
    }

    /**
     * Config closure
     * @since 1.0
     */
    def weblogicWsClient(Closure closure) {
        webService.configure closure
    }

    /**
     * Bestemmer katalog for genererte filer.
     * @since 1.1
     */
    def WeblogicWsClientConvention genDir(Object path) {
        genDir = path
        this
    }

    def getGenDir() {
        return genDir
    }
}

/**
 * @since 1.1
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class WebServiceConfig {
    private final WeblogicWsClientConvention convention;
    protected final String name;

    protected final FileCollection schemaFiles;
    protected final List schemaFilesSpecs = [];
    protected final Configuration baseWars;

    protected ExceptionConfig exception;

    protected String lastWsdl = null;


    WebServiceConfig(WeblogicWsClientConvention convention, String name) {
        this.name = name
        this.convention = convention
        this.baseWars = convention.project.configurations.detachedConfiguration()
        this.schemaFiles = convention.project.files(schemaFilesSpecs)
    }

    /**
     * Kun for testing
     */
    WebServiceConfig(Project project) {
        this.name = null
        this.convention = new WeblogicWsClientConvention(project)
        this.baseWars = convention.project.configurations.detachedConfiguration()
        this.schemaFiles = convention.project.files(schemaFilesSpecs)
    }

    protected WebServiceConfig configure(Closure closure) {
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.delegate = this
        closure()
        return this
    }

    String getName() {
        return name
    }
/**
     * Alternativ spesifisering av wsdl filer dersom en ønsker overstyring av default verdier.
     *
     * @see Project#files(Object... paths)
     */
    public WebServiceConfig schemaFiles(Object... paths) {
        schemaFilesSpecs.addAll paths
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
     * Bestemmer hvor wsdl schema befinner seg.
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
        def baseWar = convention.project.dependencies.create(notation)
        baseWars.dependencies.add(baseWar)
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

    public void lastWsdl(String wsdl) {
        lastWsdl = wsdl;
    }

    String getLastWsdl() {
        return lastWsdl
    }

    ExceptionConfig getException() {
        return exception
    }

    public String toString() {
        return getClass().getSimpleName() + ": " + name;
    }

}

/**
 * Det kan virke som om at exceptions er forbedret i webservicer generert med weblogic 10.3.5 ?
 *
 * @since 1.1
 */
class ExceptionConfig {
    private final WebServiceConfig convention;

    protected String packageOrPathString
    protected PatternSet exceptionFilePatternSet = new PatternSet(includes: ['**/*Exception.java'])



    protected ExceptionConfig(WebServiceConfig convention) {
        this.convention = convention
    }

    String getPackageString() {
        return packageOrPathString?.replace('/', '.')?.replace('\\', '.')
    }

    PatternSet getExceptionFilePatternSet() {
        return exceptionFilePatternSet
    }

}