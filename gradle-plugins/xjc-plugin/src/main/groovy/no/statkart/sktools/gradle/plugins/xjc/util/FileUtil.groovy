package no.statkart.sktools.gradle.plugins.xjc.util

import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException

class FileUtil {
    static File append(File file, String... dirs) {
        if (dirs.length == 0)
            file
        else
            append(new File(file, dirs[0]), dirs.tail())
    }
}