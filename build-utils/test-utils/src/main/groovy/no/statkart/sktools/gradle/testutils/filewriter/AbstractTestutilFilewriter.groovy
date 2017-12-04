package no.statkart.sktools.gradle.testutils.filewriter

import no.statkart.sktools.gradle.testutils.ProjectHelper

/**
 *
 *
 * @since 1.3 - ny grunnbok sprint 30
 * @author Leif Lislegård
 */
abstract class AbstractTestutilFilewriter {

    /**
     * Oppretter fil og skriver evaluert innhold til fil
     */
    public static File writeCustomFile(ProjectHelper projectHelper, String targetPath, Closure text) {
        ArrayList<File> generatedFiles = new ArrayList<File>()

        generatedFiles.add projectHelper.project.file(targetPath).with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.print(text.call())
            }
            return file
        }

        return generatedFiles.iterator().next();
    }

}
