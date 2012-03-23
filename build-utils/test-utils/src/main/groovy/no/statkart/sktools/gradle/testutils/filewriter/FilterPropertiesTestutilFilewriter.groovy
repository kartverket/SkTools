package no.statkart.sktools.gradle.testutils.filewriter

import no.statkart.sktools.gradle.testutils.ProjectHelper

/**
 * Statiske understøttende hjelpemetoder for generering av kildekode for bruk i testing.
 *
 * @author Leif Lislegård
 */
class FilterPropertiesTestutilFilewriter {

    /**
     * Oppretter to ressursfiler med navn <code>simpleResource1.txt</code> og <code>simpleResource2.txt</code>
     *
     * <p><code>simpleResource1.txt</code> inneholder
     * <ul>
     *     <li>name=@name@
     *     <li>version=@version@
     *     <li>myProperty1=@myProperty1@
     * </ul>
     *
     * <p><code>simpleResource2.txt</code> inneholder
     * <ul>
     *     <li>myProperty1=@myProperty1@
     *     <li>myProperty2=@myProperty2@
     *     <li>myEmail=@myEmail@@statkart.no
     * </ul>
     *
     */
    public static Collection<File> writeTwoSimpleResources(ProjectHelper projectHelper, String targetPath) {
        ArrayList<File> generatedFiles = new ArrayList<File>()

        generatedFiles.add projectHelper.project.file(targetPath + '/simpleResource1.txt').with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.println("name=@name@")
                writer.println("version=@version@")
                writer.println("myProperty1=@myProperty1@")
            }
            return file
        }
        generatedFiles.add projectHelper.project.file(targetPath + '/simpleResource2.txt').with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.println("myProperty1=@myProperty1@")
                writer.println("myProperty2=@myProperty2@")
                writer.println("myEmail=@myEmail@@statkart.no")
            }
            return file
        }

        return generatedFiles
    }
}
