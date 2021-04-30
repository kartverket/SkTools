package no.statkart.sktools.gradle.plugins.xjc

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.gradle.util.ConfigureUtil

import java.nio.file.Paths

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

    public Property<File> genOutputPath;
    public final String genTaskName;

    public final transient ConfigurableFileCollection source;


    //SKTOOLS-128: annoterer properties som er input felter
    @Input
    Map<String, Map> xjcOptions = [:] as HashMap

    private final Project project


    XjcConfig(String name, SourceSet sourceSet, Project project) {
        this.project = project;
        this.name = name;

        this.source = project.files();

        this.genOutputPath = project.getObjects().property(File);
        this.genOutputPath.set(defaultOutputPath(sourceSet));

        this.genTaskName = sourceSet.getTaskName("gen", name);
    }

    Provider<File> defaultOutputPath(SourceSet sourceSet) {
        def callable = { project.file(Paths.get(project.getBuildDir() as String, "xjc", sourceSet.getName(), name)) }
        return project.provider(callable)
    }


    //todo: endre default fqn i en versjon etter 1.0?
    //metode for deklarativ konfigurasjon
    @Internal
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
    @Internal
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


    @Internal
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

    void setGenOutputPath(String genOutputPath) {
        this.genOutputPath.set(project.file(genOutputPath))
    }
}