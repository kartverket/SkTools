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