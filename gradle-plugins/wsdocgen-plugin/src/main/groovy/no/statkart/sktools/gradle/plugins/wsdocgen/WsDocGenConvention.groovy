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
    private static final long serialVersionUID = 1L;
    protected final transient Project project

    public final static String GEN_TASK_NAME_PATTERN = "gen%s%sWSDoc"

    protected final List<Group> groups = new ArrayList<Group>()

    /**
     * Styrer hvilket source set som det skal genereres ifra.
     */
    protected String sourceSetName;    //defaults to "main"



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
    private static final long serialVersionUID = 1L;

    protected final transient Project project
    protected final transient WsDocGenConvention convention

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
     * @see #xslt(Object)
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

//    @InputFile //not up to date when change in file
    public File getServiceXsltFile() {
        if (serviceXsltPath) {
            return project.file(serviceXsltPath)
        } else {
            project.logger.warn("WARNING: no xslt file specified - using template for TESTING purposes..")
            return generateTestFile(new File(project.buildDir, "Transform.xsl")) //can't write to output dir because it gets wiped when not up to date...
        }
    }

    private File generateTestFile(File testFile) {
        if (testFile.exists()) return testFile;

        testFile.getParentFile().mkdirs()
        testFile.createNewFile()

        testFile.withWriter { def writer ->
            this.getClass().getResourceAsStream("tasks/DefaultTransform.xsl").withReader() {
                it.readLines().each { writer.write(it); writer.write("\n") }
            }
            writer.flush()
        }
        return testFile
    }


//    @Optional
//    @InputFile //not up to date when change in file
    File getIndexXsltFile() {
        if (indexXsltPath) {
            return project.file(indexXsltPath)
        } else {
            return null
        }
    }


}
