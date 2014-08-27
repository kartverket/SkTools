package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.plugins.webstart.util.ArtifactMatcher
import org.gradle.api.GradleException

/**
 *
 */
class JnlpSyntaxUtil {

    /**
     * @param mainJarFiles Disse er potensielt usignert, og kan derfor ikke brukes direkte
     * @param allJarFiles Dette er de jar-filene som skal brukes, både main og de andre
     * @return
     */
    static def Node appendJarElementForAllDependencies(Node resourcesNode, Set<File> mainJarFiles, Set<File> allJarFiles, String libPath, String digest) {
        final Set<File> nonMainJarFiles // Dette er alle jar-filer som ikke skal merkes som main

        boolean mainJarFound = false

        allJarFiles.each { File file ->
            ArtifactMatcher artifactMatcher = new ArtifactMatcher(file)
            String jarPath = libPath + '/' + artifactMatcher.name + '.' + artifactMatcher.type

            Node jarNode = new Node(resourcesNode, 'jar', [href: jarPath, version: artifactMatcher.version + digest])

            if (mainJarFiles.contains(file)) {
                if (mainJarFound) {
                    throw new GradleException('There can only be one main jar. ' + mainJarFiles)
                }
                mainJarFound = true
                jarNode.attributes().put('main', true)
            }

            long size = file.length()   //0 if for some reason the file can not be found
            if (size > 0L) {
                jarNode.attributes().put('size', size)
            }
        }

        return resourcesNode
    }

    static def Node createResourcesElement(ResourcesConfiguration resources) {
        Node resourcesNode = new Node(null, 'resources')

        if (resources != null) {
            resources.runtimes.each { JavaRuntimeConfiguration javaRuntime ->
                resourcesNode.append(createJavaRuntimeElement(javaRuntime))
            }

            resources.systemProperties?.each { key, value ->
                resourcesNode.appendNode('property', [name: key, value: value])
            }
        }

        return resourcesNode
    }

    static def Node createJavaRuntimeElement(JavaRuntimeConfiguration javaRuntime) {
        Node javaNode = new Node(null, 'j2se', [version: javaRuntime.version])
        if (javaRuntime.href != null) {
            javaNode.attributes().put('href', javaRuntime.href)
        }
        if (javaRuntime.xms != null) {
            javaNode.attributes().put('initial-heap-size', javaRuntime.xms)
        }
        if (javaRuntime.xmx != null) {
            javaNode.attributes().put('max-heap-size', javaRuntime.xmx)
        }
        if (javaRuntime.vmArgs != null) {
            javaNode.attributes().put('java-vm-args', javaRuntime.vmArgs)
        }
        return javaNode
    }
}
