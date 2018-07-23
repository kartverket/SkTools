package no.statkart.sktools.gradle.plugins.xjc

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSet
import org.gradle.util.ConfigureUtil

/**
 * Konfigurasjon for en xjc eksekvering.
 * <br />
 *
 * Dersom en ønsker å legge ved kildekode for custom implementasjon så benyttes {@link SourceSet#java}
 *
 * @author Leif Lislegård
 */
class XjcConfig {
    /**
     * For lovlige parametere se {@link com.sun.tools.xjc.addon.statkart.ListGenPlugin#parseArgument(com.sun.tools.xjc.Options, String[], int)
     */
    static final String LIST_ADAPTER = 'list_adapter';
    /**
     * For lovlige parametere se {@link com.sun.tools.xjc.addon.statkart.GrunnbokDocPlugin#parseArgument(com.sun.tools.xjc.Options, String[], int)
     */
    static final String GRUNNBOK_DOC = 'grunnbok_doc';

    public final String name;

    public String genOutputPath //SKTOOLS-10: mulighet for konfigurering av path
    public transient String genTaskName  //SKTOOLS-10: mulighet for konfigurering av navn

    public final transient ConfigurableFileCollection source;


    //SKTOOLS-128: annoterer properties som er input felter
    @Input
    public Map<String, Map> xjcOptions = [:] as HashMap


    XjcConfig(SourceSet sourceSet, String name, ConfigurableFileCollection sourceFiles) {
        this.name = name;

        this.source = sourceFiles
        this.genOutputPath = String.format("build/xjc/%s/%s", sourceSet.getName(), name)

        this.genTaskName = sourceSet.getTaskName("gen", name); //SKTOOLS-10: mulighet for konfigurering av navn
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

    //ikke eksponert
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

    XjcConfig configure(Closure closure) {
        return ConfigureUtil.configure(closure, this);
    }

    public FileCollection srcDir(Object srcDir) {
        return source.from(srcDir);
    }

    public FileCollection srcDirs(Object... srcDirs) {
        return source.from(srcDirs);
    }

    public void setSrcDirs(Iterable<?> srcPaths) {
        source.setFrom(srcPaths);
    }

    public void setSrcDirs(Object... srcPaths) {
        source.setFrom(srcPaths);
    }

    public FileCollection source(FileTree src) {
        return source.from(src);
    }

}