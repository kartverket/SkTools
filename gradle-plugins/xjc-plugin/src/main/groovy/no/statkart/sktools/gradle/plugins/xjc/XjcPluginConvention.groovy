package no.statkart.sktools.gradle.plugins.xjc

import org.gradle.api.Project
import no.statkart.sktools.gradle.plugins.xjc.util.FileUtil

/**
 * todo: dokumentasjon
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class XjcPluginConvention {
    File targetDirectory
    List<Schema> schemas = new ArrayList<Schema>()

    XjcPluginConvention(Project project) {
        targetDirectory = FileUtil.append(project.getBuildDir(), 'generated', 'main', 'java')
    }

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
        schemas.add(new Schema(shemaConfig))
    }

    /**
     * @deprecated since 1.0 - bruk heller {@link #schema(Closure)}
     */
    void schema(dir, includes, withGrunnbokDoc, withListAdapter) {
        schemas.add(new Schema(dir: dir, includes: includes, withGrunnbokDoc: withGrunnbokDoc, withListAdapter: withListAdapter))
    }

    /**
     * @deprecated since 1.0 - bruk heller {@link #schema(Closure)}
     */
    void schema(dir, includes, withGrunnbokDoc) {
        schemas.add(new Schema(dir: dir, includes: includes, withGrunnbokDoc: withGrunnbokDoc))
    }

    /**
     * @deprecated since 1.0 - bruk heller {@link #schema(Closure)}
     */
    void schema(dir, includes) {
        schemas.add(new Schema(dir: dir, includes: includes))
    }

}

class   Schema {
    String dir;
    String includes;
    String withGrunnbokDoc = null;
    boolean withListAdapter = false;

    Schema(Closure closure) {
        closure.setDelegate(this)
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }
}