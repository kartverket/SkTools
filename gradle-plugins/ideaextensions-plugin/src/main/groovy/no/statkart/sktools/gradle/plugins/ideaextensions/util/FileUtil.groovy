package no.statkart.sktools.gradle.plugins.ideaextensions.util

import org.gradle.api.GradleException
import groovy.util.slurpersupport.GPathResult
import groovy.xml.XmlUtil
import groovy.xml.StreamingMarkupBuilder

class FileUtil {
    static String relativeTo(File to, File from) {
        String toPath = to.getAbsolutePath()
        String fromPath = from.getAbsolutePath()
        if (!fromPath.startsWith(toPath)) {
            throw new GradleException("Unable to find relative path of $fromPath to $toPath")
        }
        String rel = fromPath.substring(toPath.length())
        while (rel.startsWith('\\') || rel.startsWith('/'))
            rel = rel.substring(1)
        return rel
    }

    /**
     * @since 1.1
     */
    static def modifyXmlFile(File file, Closure modifications) {
        GPathResult xml = new XmlSlurper().parse(file)

        modifications.call(xml)

        def writer = new OutputStreamWriter(new FileOutputStream(file), 'UTF-8')
        XmlUtil.serialize(new StreamingMarkupBuilder().bind {
            mkp.yield xml
        }, writer)
        writer.close()
    }
}