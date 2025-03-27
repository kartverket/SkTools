package no.statkart.sktools.gradle.plugins.wsdocgen

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet

/**
 * Dokumentasjon av en logisk samling webservices.
 *
 * @since 1.1
 */
class WsDocGroup {
    private final Project project
    private final SourceSet sourceSet

    /**
     * Navn for gruppe - blir automatisk tildelt dersom ikke spesifisert
     * @since 1.3
     */
    protected String name

    /**
     * Hvilket dir det skal legges til
     */
    final Property<File> targetPath

    final Property<String> lookupPath

    final Property<String> encoding

    final Property<File> serviceXsltPath
    final Property<File> indexXsltPath


    WsDocGroup(Project project, String name, SourceSet sourceSet) {
        this.name = name
        this.project = project
        this.sourceSet = sourceSet
        this.targetPath = project.getObjects().property(File)
        this.lookupPath = project.getObjects().property(String)
        this.encoding = project.getObjects().property(String)
        this.serviceXsltPath = project.getObjects().property(File)
        this.indexXsltPath = project.getObjects().property(File)
    }

    protected WsDocGroup configure(Closure closure) {
        closure.setDelegate(this)
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
        return this
    }

    /**
     * @since 1.1
     */
    WsDocGroup targetPath(Object path) {
        targetPath.set(project.file(path))
        return this
    }

    /**
     * @since 1.1
     */
    WsDocGroup lookupPath(String relativePath) {
        lookupPath.set(relativePath)
        return this
    }

    /**
     * @since 1.3
     */
    WsDocGroup xslt(Object path) {
        serviceXsltPath.set(project.file(path))
        return this
    }

    /**
     * @see #xslt(Object)
     * @since 1.3
     */
    WsDocGroup serviceXslt(Object path) {
        return xslt(path)
    }

    /**
     * @since 1.3
     */
    WsDocGroup indexXslt(Object path) {
        indexXsltPath.set(project.file(path))
        return this
    }

    WsDocGroup encoding(String encoding) {
        this.encoding.set(encoding)
        return this
    }

    /**
     * Compatibility with syntax pre SKTOOLS-213
     */
    @Deprecated //kan fjernes i sktools 8
    WsDocGroup group(Closure<?> closure) {
        project.getLogger().warn("Deprecated syntax: wsdoc.group - see SKTOOLS-213 for details");
        project.configure(this, closure)
        return this
    }
}
