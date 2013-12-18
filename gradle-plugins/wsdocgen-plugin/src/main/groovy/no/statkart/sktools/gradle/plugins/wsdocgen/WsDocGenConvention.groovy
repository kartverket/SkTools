package no.statkart.sktools.gradle.plugins.wsdocgen

import org.gradle.api.Project

import org.apache.commons.lang.builder.EqualsBuilder

/**
 * Kan konfigureres oppt til å dokumentere valgfritt sourceSet via {@link WsDocGenConvention#sourceSetName}
 *
 * Det legges opp til at man kan genere dokumentasjon for flere samlinger av servicer, disse grupperes og må da selv tildeles targetPaths.
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class WsDocGenConvention implements Serializable {
    private final static long serialVersionUID = 1L;
    final static protected transient String GEN_TASK_NAME_PATTERN = "gen%s%sWsDoc"
    final transient Project project

    protected final List<Group> groups = new ArrayList<Group>()

    /**
     * Styrer hvilket source set som det skal genereres ifra.
     */
    protected String sourceSetName;    //defaults to "main"


    //blir dynamisk satt av Plugin
    //todo: faktorere denne bort
    protected String genDocTaskName

    WsDocGenConvention(Project project) {
        this.project = project
    }

    /**
     * Config closure
     * @since 1.0
     */
    def wsDoc(Closure closure) {
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.delegate = this
        closure()
    }


    /**
     * @since 1.1
     */
    void sourceSet(String sourceSetName) {
        this.sourceSetName = sourceSetName
    }

    /**
     * @since 1.1
     */
    void docGroup(Closure groupConfig) {
        groups.add(new Group(this).configure(groupConfig))
    }




    /**
     * @depricated since 1.0 - bruk heller {@link #wsDoc(Closure)}.
     */
    def wsdlDoc(Closure closure) {
        println 'wsdlDoc(Closure) is now depricated - use wsDoc(Closure) instead!'
        return wsdlDoc(closure)
    }


    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}

/**
 * Dokumentasjon for en logisk samling webservices.
 *
 * @since 1.1
 */
class Group implements Serializable {
    private final static long serialVersionUID = 1L;
    final transient Project project
    final transient WsDocGenConvention convention

    /**
     * Navn for gruppe - blir automatisk tildelt dersom ikke spesifisert
     * @since 1.3
     */
    protected name;

    protected Collection<String> includes;

    /**
     * Hvilket dir det skal legges til
     */
    protected File targetDir

    protected String lookupPath

    protected def serviceXsltPath
    protected def indexXsltPath


    protected Group(WsDocGenConvention convention) {
        this.convention = convention
        this.project = convention.project
    }

    protected Group configure(Closure closure) {
        closure.setDelegate(this)
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
        return this
    }

    /**
     * @since 1.1
     */
    Group include(String... patterns) {
        if (includes == null) {
            includes = new ArrayList<String>();
        }
        includes.addAll(patterns);
        return this
    }


    /**
     * @since 1.1
     */
    Group targetPath(Object path) {
        targetDir = project.file(path);
        return this;
    }

    /**
     * @since 1.1
     */
    Group lookupPath(String relativePath) {
        lookupPath = relativePath;
        return this;
    }

    /**
     * @since 1.3
     */
    Group xslt(Object path) {
        serviceXsltPath = path;
        return this;
    }

    /**
     * SKTOOLS-105
     * @see #xslt(java.lang.Object)
     * @since 1.3
     */
    Group serviceXslt(Object path) {
        return xslt(path);
    }

    /**
     * SKTOOLS-105
     * @since 1.3
     */
    Group indexXslt(Object path) {
        indexXsltPath = path;
        return this;
    }


    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}
