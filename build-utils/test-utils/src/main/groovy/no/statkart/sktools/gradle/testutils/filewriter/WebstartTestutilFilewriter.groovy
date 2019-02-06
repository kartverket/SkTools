package no.statkart.sktools.gradle.testutils.filewriter

import no.statkart.sktools.gradle.testutils.ProjectHelper

import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Statiske understøttende hjelpemetoder for generering av kildekode for bruk i testing.
 *
 * @author Leif Lislegård
 */
class WebstartTestutilFilewriter extends AbstractTestutilFilewriter {

    private final static KeystorePath = "/keystore/selfsign.jks";
    final static String KeystoreAlias = 'selfsign';
    final static String KeystorePassword = 'meMyselfAndI';

    /**
     * Kopierer ut <code>kodesignering.jks</code> til targetPath.
     */
    public static Collection<File> writeKodesignerinSertifikat(ProjectHelper projectHelper, String targetPath) {
        ArrayList<File> generatedFiles = new ArrayList<File>()

        File certFile = new File(projectHelper.project.mkdir(targetPath), "kodesignering.jks")
        Files.copy(getClass().getResourceAsStream(KeystorePath), certFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        generatedFiles.add(certFile)

        return generatedFiles;
    }

    /**
     * Tom java klasse <code>no.statkart.test.div.Dummy{n}</code> som skrives til <code>div/Dummy{n}.java</code>
     */
    public static Collection<File> writeDummyNClass(ProjectHelper projectHelper, String targetPath, def n = '') {
        ArrayList<File> generatedFiles = new ArrayList<File>()

        generatedFiles.add projectHelper.project.file("${targetPath}/div/Dummy${n}.java").with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.print """
                    package no.statkart.test.div;

                    public class Dummy${n} {

                        public Dummy${n}() {
                        }

                    }
                """
            }
            return file
        }
        return generatedFiles
    }

    /**
     * Tom java klasse <code>no.statkart.test.div.DynamicMethods{n}</code> som skrives til <code>div/DynamicMethods{n}.java</code>
     * som inneholder m antall metoder.
     */
    public static Collection<File> writeDynamicMethodsClassWithNMethods(ProjectHelper projectHelper, String targetPath, def n = 0, def m = 0) {
        ArrayList<File> generatedFiles = new ArrayList<File>()

        generatedFiles.add projectHelper.project.file("${targetPath}/div/DynamicMethods${n}.java").with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.print """
                    package no.statkart.test.div;

                    public class DynamicMethods${n} {

                        public DynamicMethods${n}() {
                        }

                """

                m.times() {
                    writer.print """
                        public void method${it}(){
                        }
                    """
                }

                writer.print """
                    }
                """
            }
            return file
        }
        return generatedFiles
    }

}
