package no.statkart.sktools.gradle.plugins.dbtools.testutils

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


    static File createTempFile(FILE_TYPE type = FILE_TYPE.Default, File dir = null, String... texts) {
        File file = File.createTempFile(type.name(), type.filetype, dir)
        file.deleteOnExit()
        createFile(file, texts)
    }

    static File createFile(File file, String... texts) {
        file.parentFile.mkdirs()
        file.createNewFile()
        file.withPrintWriter { def writer ->
            for (String text : texts) {
                writer.println text
            }
            writer.flush()
            return null
        }
        return file
    }


}
