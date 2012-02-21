package no.statkart.sktools.gradle.plugins.weblogic.wswar.util

import org.gradle.api.GradleException

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

}