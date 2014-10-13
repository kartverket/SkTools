package no.statkart.sktools.gradle.plugins.xjc

import org.apache.commons.lang.builder.EqualsBuilder

/**
 * Konfigurasjon for en xjc eksekvering.
 * <br />
 *
 * Dersom en ønsker å legge ved kildekode for custom implementasjon så benyttes {@link XjcSourceDirectorySet#getJava() }
 *
 * @author Leif Lislegård
 */
class XjcConfig implements Serializable {
    private final static long serialVersionUID = 1L;

    /*
     * For lovlige parametere se {@link com.sun.tools.xjc.addon.statkart.ListGenPlugin#parseArgument(com.sun.tools.xjc.Options, String[], int)
     */
    static final String LIST_ADAPTER = 'list_adapter';
    /*
     * For lovlige parametere se {@link com.sun.tools.xjc.addon.statkart.GrunnbokDocPlugin#parseArgument(com.sun.tools.xjc.Options, String[], int)
     */
    static final String GRUNNBOK_DOC = 'grunnbok_doc';


    protected transient final XjcSourceDirectorySet source;

    protected Collection<String> includes;
    protected def xjcOptions = [][] as HashMap


    XjcConfig(XjcSourceDirectorySet schema) {
        this.source = schema
    }



    XjcSourceDirectorySet includes(String... patterns) {
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


    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }


    XjcSourceDirectorySet configure(Closure closure) {
        closure.setDelegate(this)
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
        return this
    }

}