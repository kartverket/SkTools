package no.statkart.sktools.gradle.plugins.webstart.util

import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException

class FileUtil {

    static File copyOutResourceTemporary(Class<?> clazz, String resourceName) {
        File temp = File.createTempFile('temporary-', 'tmp')
        FileUtils.copyURLToFile(clazz.getResource(resourceName), temp)
        temp.deleteOnExit()
        return temp
    }
}