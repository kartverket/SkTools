package no.statkart.gradle.util

import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException

class FileUtil {
    static File append(File file, String... dirs) {
        if (dirs.length == 0)
            file
        else
            append(new File(file, dirs[0]), dirs.tail())
    }
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

    static File copyOutResourceTemporary(Class<?> clazz, String resourceName) {
        File temp = File.createTempFile('temporary-', 'tmp')
        FileUtils.copyURLToFile(clazz.getResource(resourceName), temp)
        temp.deleteOnExit()
        return temp
    }
}