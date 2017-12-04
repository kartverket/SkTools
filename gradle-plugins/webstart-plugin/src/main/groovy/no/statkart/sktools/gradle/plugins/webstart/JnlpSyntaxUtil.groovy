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

            long size = file.length()   //0 if for some reason the file can not be found
            if (size > 0L) {
                jarNode.attributes().put('size', size)
            }

            if (mainJarFiles.contains(file)) {
                if (mainJarFound) {
                    throw new GradleException('SKTOOLS-118: There can only be one main jar; was: ' + mainJarFiles)
                }
                mainJarFound = true
                jarNode.attributes().put('main', true)
            }

        }

        return resourcesNode
    }

    static def Node createResourcesElement(ResourcesConfiguration resources) {
        Node resourcesNode = new Node(null, 'resources')

        if (resources != null) {
            resources.runtimes.each { RuntimeConfiguration runtimeConfiguration ->
                resourcesNode.append(createRuntimeElement(runtimeConfiguration))
            }

            resources.systemProperties?.each { key, value ->
                resourcesNode.appendNode('property', [name: key, value: value])
            }
        }

        return resourcesNode
    }

    static def Node createRuntimeElement(RuntimeConfiguration runtimeConfiguration) {
        if (runtimeConfiguration instanceof JavaRuntimeConfiguration) {
            return createJavaRuntimeElement(runtimeConfiguration)
        } else if (runtimeConfiguration instanceof JavaFxRuntimeConfiguration) {
            return createJavaFxRuntimeElement(runtimeConfiguration)
        } else {
            throw new GradleException("Configuration class not supported! ${runtimeConfiguration.class}")
        }
    }

    static def Node createJavaFxRuntimeElement(JavaFxRuntimeConfiguration configuration) {
        Node javaNode = new Node(null, 'jfx:javafx-runtime', [version: configuration.version])
        if (configuration.href != null) {
            javaNode.attributes().put('href', configuration.href)
        }
        return javaNode
    }

    static def Node createJavaRuntimeElement(JavaRuntimeConfiguration configuration) {
        Node javaNode = new Node(null, 'j2se', [version: configuration.version])
        if (configuration.href != null) {
            javaNode.attributes().put('href', configuration.href)
        }
        if (configuration.xms != null) {
            javaNode.attributes().put('initial-heap-size', configuration.xms)
        }
        if (configuration.xmx != null) {
            javaNode.attributes().put('max-heap-size', configuration.xmx)
        }
        if (configuration.vmArgs != null) {
            javaNode.attributes().put('java-vm-args', configuration.vmArgs)
        }
        return javaNode
    }
}
