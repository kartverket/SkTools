package no.statkart.sktools.gradle.plugins.dbtools.testutils

import no.statkart.sktools.utils.databasepatcher.SqlExecutor

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * @since 1.3
 * @author Leif Lislegård
 */
class DbToolsTestContext<T extends DbToolsTestContext> {
    static enum FILE_TYPE {
        Patch('.sql'),
        SQL('.sql'),
        HSQL('.hsql'),
        Text('.txt'),
        Default('.txt')

        final String filetype
        FILE_TYPE(String filetype) {
            this.filetype = filetype
        }
    }

    Charset encoding = SqlExecutor.sqlFileEncoding


    File createTempFile(File dir = null, CharSequence text) {
        return createTempFile(FILE_TYPE.Default, dir, text, encoding)
    }

    File createTempFile(FILE_TYPE type, CharSequence text) {
        return createTempFile(type, null, text, encoding)
    }

    static File createTempFile(FILE_TYPE type, File dir, CharSequence text, Charset encoding) {
        File file = File.createTempFile(type.name(), type.filetype, dir)
        file.deleteOnExit()
        createFile(file, Collections.singleton(text), encoding)
    }

    static File createFile(File file, Iterable<? extends CharSequence> texts, Charset encoding) {
        file.parentFile.mkdirs()
        file.createNewFile()
        Files.write(file.toPath()
            , texts
            , encoding
            , StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        return file
    }


}
