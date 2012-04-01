package no.statkart.sktools.gradle.plugins.xjc

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import org.apache.commons.lang.builder.EqualsBuilder

/**
 * Se {@link XjcPluginTest#testDefaultSetting() } for eksempler på konfigurering.
 *
 * ps. bruk av transient felter for å styre hva som ikke skal persisteres ved gradles beregning av up to date ved depends on.
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class XjcConvention implements Serializable {
    private final static long serialVersionUID = 1L;
    final transient Project project

    protected final List<Schema> schema = new ArrayList<Schema>()

    /**
     * Styrer hvilket source set som det skal genereres til.
     */
    protected String sourceSetName = SourceSet.MAIN_SOURCE_SET_NAME;    //defaults to "main"

    /**
     * Hvilket dir det skal legges til
     */
    protected File targetDir



    XjcConvention(Project project) {
        this.project = project
    }

    /**
     * Configuration closure for this plugin
     */
    def xjc(Closure closure) {
        closure.delegate = this
        closure()
    }

    /**
     * @depricated since 1.0 - bruk heller {@link #xjc(Closure)}.
     */
    def statKartXjc(Closure closure) {
        println 'statKartXjc(Closure) is now depricated - use xjc(Closure) instead!'
        return xjc(closure)
    }

    /**
     * @since 1.0
     */
    void schema(Closure shemaConfig) {
        schema.add(new Schema(this).configure(shemaConfig))
    }


    XjcConvention targetDir(Object path) {
        targetDir = project.file(path);
        return this;
    }


    void targetDirectory(File dir) {
        targetDir(dir);
    }

    void sourceSetName(String name) {
        sourceSetName = name;
    }



    //todo: delete deprecated methods

    /**
     * @deprecated since 1.0 - bruk heller {@link #schema(Closure)}
     */
    void schema(dir, String includes, withGrunnbokDoc=null, withListAdapter=null) {
        logDeprecation('schema(dir, includes, grunnbokDoc?, listAdapter?)', 'schema(Closure)')
        Schema newSchema = new Schema(this)
        .dir(dir)
        .includes(includes)
        if (withGrunnbokDoc) {
            newSchema.withGrunnbokDoc()
        }
        if (withListAdapter) {
            newSchema.withListAdapter()
        }
        schema.add(newSchema)
    }


    private static void logDeprecation(String oldSyntax, String newSyntax) {
        println "${oldSyntax} in ${XjcConvention.class.simpleName} is now deprecated \n\t\t-use ${newSyntax} instead!"
    }

    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}

/**
 * Holder konfigurasjon for en logisk samling schema filer.
 *
 * @since 1.0
 * @author Leif Lislegård
 */
class Schema implements Serializable {
    private final static long serialVersionUID = 1L;

    /*
     * For lovlige parametere se {@link com.sun.tools.xjc.addon.statkart.ListGenPlugin#parseArgument(com.sun.tools.xjc.Options, String[], int)
     */
    static final String LIST_ADAPTER = 'list_adapter';
    /*
     * For lovlige parametere se {@link com.sun.tools.xjc.addon.statkart.GrunnbokDocPlugin#parseArgument(com.sun.tools.xjc.Options, String[], int)
     */
    static final String GRUNNBOK_DOC = 'grunnbok_doc';

    final transient Project project

    protected File dir;
    protected Collection<String> includes;

    protected def xjcOptions = [][] as HashMap

    Schema(XjcConvention convention) {
        this.project = convention.project
    }


    Schema configure(Closure closure) {
        closure.setDelegate(this)
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
        return this
    }

    Schema includes(String... patterns) {
        if (includes == null) {
            includes = new ArrayList<String>();
        }
        includes.addAll(patterns);
        return this
    }


    //todo: endre default fqn i en versjon etter 1.0?
    //metode for deklarativ konfigurasjon
    def getWithListAdapter() {
        listAdapter([baseClass:'no.statkart.grunnbok.skif.util.ListIterable']);
    }


    void withListAdapter(String fqn, String getterMethodName=null) {
        listAdapter([baseClass:fqn])
        if (getterMethodName != null) {
            listAdapter([method:getterMethodName])
        }
    }

    //ikke eksponert
    private Map listAdapter(Map params) {
        Map map = xjcOptions.get(LIST_ADAPTER);
        if (map == null) {
            xjcOptions.put(LIST_ADAPTER, map = new HashMap(params))
        } else {
            map.putAll(params);
        }
        return map;
    }

    //metode for deklarativ konfigurasjon
    def getWithGrunnbokDoc() {
        grunnbokDoc([:]);
    }

    //ikke eksponert
    private Map grunnbokDoc(Map params) {
        Map map = xjcOptions.get(GRUNNBOK_DOC);
        if (map == null) {
            xjcOptions.put(GRUNNBOK_DOC, map = new HashMap(params))
        } else {
            map.putAll(params);
        }
        return map;
    }




    Schema path(Object path) {
        dir = project.file(path);
        return this;
    }


    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
