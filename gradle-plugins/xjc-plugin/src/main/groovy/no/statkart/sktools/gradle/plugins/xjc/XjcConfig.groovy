package no.statkart.sktools.gradle.plugins.xjc

import org.apache.commons.lang.builder.EqualsBuilder
import org.gradle.api.tasks.SourceSet

/**
 * Konfigurasjon for en xjc eksekvering.
 * <br />
 *
 * Dersom en ønsker å legge ved kildekode for custom implementasjon så benyttes {@link XjcSourceDirectorySet#getJava() }
 *
 * @author Leif Lislegård
 */
class XjcConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /*
     * For lovlige parametere se {@link com.sun.tools.xjc.addon.statkart.ListGenPlugin#parseArgument(com.sun.tools.xjc.Options, String[], int)
     */
    static final String LIST_ADAPTER = 'list_adapter';
    /*
     * For lovlige parametere se {@link com.sun.tools.xjc.addon.statkart.GrunnbokDocPlugin#parseArgument(com.sun.tools.xjc.Options, String[], int)
     */
    static final String GRUNNBOK_DOC = 'grunnbok_doc';


    public String genOutputPath //SKTOOLS-10: mulighet for konfigurering av path
    public transient String genTaskName, compileTaskName, hookTaskName  //SKTOOLS-10: mulighet for konfigurering av navn

    protected transient final XjcSourceDirectorySet source;

    protected Collection<String> includes;
    protected Map<String, Map> xjcOptions = [:] as HashMap


    XjcConfig(XjcSourceDirectorySet schema, SourceSet sourceSet) {
        this.source = schema
        this.genOutputPath = String.format("gen/%s/xjc/%s", sourceSet.getName(), schema.getName())

        this.genTaskName = sourceSet.getTaskName("gen", schema.getName()); //SKTOOLS-10: mulighet for konfigurering av navn
        this.compileTaskName = sourceSet.getTaskName("compile", schema.getName()); //SKTOOLS-10: mulighet for konfigurering av navn
        this.hookTaskName = sourceSet.getTaskName("hookCompile", schema.getName());
    }



    XjcConfig includes(String... patterns) {
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
            xjcOptions.put(GRUNNBOK_DOC, map = new LinkedHashMap(params))
        } else {
            map.putAll(params);
        }
        return map;
    }


    def getWithSkDoc() {
        skDoc([:]);
    }

    def getWithSkDoc(Map params) {
        skDoc(params);
    }

    //ikke eskponert
    private Map skDoc(Map params) {

        // Gjennbruker grunnbokDoc-implementasjon her inntill annen dokumentasjonsgenerering er på plass
        // see {@link #GRUNNBOK_DOC}

        HashMap grunnbokDocParams = new LinkedHashMap();
        if (params.containsKey('from')) {
            grunnbokDocParams.put('from', params.get('from'));
            grunnbokDocParams.put('to', params.get('to'));
        }
        grunnbokDoc(grunnbokDocParams);
    }

    int hashCode() {
        int result
        result = (genOutputPath != null ? genOutputPath.hashCode() : 0)
        result = 31 * result + (includes != null ? includes.hashCode() : 0)
        result = 31 * result + (xjcOptions != null ? xjcOptions.hashCode() : 0)
        return result
    }

    boolean equals(Object o) {
        if (this.is(o)) return true

        // Sammenlikningen 'getClass() != o.class' feiler når gradle-daemon er aktiv. Forstår ikke helt hvorfor, men o er ikke altid av type XjcConfig
        // selv om o.getClass().toString() returnerer riktig klasse. Det går heller ikke an å caste til XjcConfig men det trengs ikke i Groovy så det er
        // ikke noe problem.
        // Hvis man caster kan man få følgende feil:
        // Cannot cast object 'no.statkart.sktools.gradle.plugins.xjc.XjcConfig@4f5924d0' with
        // class 'no.statkart.sktools.gradle.plugins.xjc.XjcConfig' to class 'no.statkart.sktools.gradle.plugins.xjc.XjcConfig'
        if (!getClass().toString().equals(o.getClass().toString())) return false
        if (genOutputPath != o.genOutputPath) return false
        if (includes != o.includes) return false
        if (xjcOptions != o.xjcOptions) return false

        return true
    }

    XjcConfig configure(Closure closure) {
        closure.setDelegate(this)
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.run()
        return this
    }

}