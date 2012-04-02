package no.statkart.sktools.gradle.plugins.webstart.util

import java.util.jar.JarFile
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import no.statkart.sktools.gradle.testutils.filewriter.WebstartTestutilFilewriter
import org.apache.commons.io.FileUtils
import org.testng.Assert
import org.testng.annotations.Test
import org.gradle.api.Project

/**
 * Test av {@link JarSigner}
 *
 * @author Leif Lislegård
 */
class JarSignerTest {

    /**
     * Instansierer opp en standard JarSigner med sertifikatfil, passord og alias.
     */
    private JarSigner buildDefaultJarSigner(ProjectHelper projectHelper, File cacheDir) {

        //jks certificate
        use(WebstartTestutilFilewriter) {
            projectHelper.writeKodesignerinSertifikat('.')
        }
        File certificateFile = projectHelper.project.file('kodesignering.jks')
        assert certificateFile.exists()

        //configures the jar signer
        JarSigner jarSigner = new JarSigner(cacheDir)
        jarSigner.setAnt(projectHelper.project.getAnt());
        jarSigner.setCertificateFile(certificateFile)
        jarSigner.setPassword('SagZ45_p1')
        jarSigner.setAlias('statenskartverk')
        return jarSigner
    }

    /**
     * Tester caching av signerte jar filer
     */
    @Test
    void testSignJarsCache() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = GradleProjectBuilder.builder().withName('root').build()
        Project project = projectHelper.project

        Set<File> jarFilesToSign = [
                projectHelper.gradleJars[0],
        ]

        JarSigner jarSigner1 = buildDefaultJarSigner(projectHelper, project.file('cache'))
        jarSigner1.setJarfilesToSign(jarFilesToSign)

        assert jarSigner1.signedArtifactsForCertificates.size() == 0   //forventer tomt cache

        //testing signed file
        Map<File, File> signing1Map = jarSigner1.signJars()
        long modified1 = signing1Map[projectHelper.gradleJars[0]].lastModified()
        jarSigner1.with { JarSigner jarSigner ->
            assert jarSigner.signedArtifactsForCertificates.size() == 1
            assert jarSigner.signedArtifactsForCertificates.values().asList()[0].size() == 1
            assert jarSigner.signedArtifactsForCertificates.values().asList()[0].values().collect {it.file}.size() == 1
            assert jarSigner.signedArtifactsForCertificates.values().asList()[0].values().collect {it.file}.containsAll(signing1Map.values())
        }

        JarSigner jarSigner2 = buildDefaultJarSigner(projectHelper, project.file('cache'))
        //forventer at ny instans konstruerer samme cache..
        jarSigner2.with { JarSigner jarSigner ->
            assert jarSigner.signedArtifactsForCertificates.size() == 1
            assert jarSigner.signedArtifactsForCertificates.values().asList()[0].size() == 1
            assert jarSigner.signedArtifactsForCertificates.values().asList()[0].values().collect {it.file}.size() == 1
            assert jarSigner.signedArtifactsForCertificates.values().asList()[0].values().collect {it.file}.containsAll(signing1Map.values())
        }

        Thread.sleep(1000) //venter ett sekund for evt ulik timestamp

        jarSigner2.setJarfilesToSign(jarFilesToSign)
        Map<File, File> signing2Map = jarSigner2.signJars();
        long modified2 = signing2Map[projectHelper.gradleJars[0]].lastModified()


        assert signing1Map == signing2Map   //forventer samme sett av filer
        assert modified1 == modified2   //forventer at cached fil er urørt

        def debug = 0

    }


    /**
     * Tester signering av jar-filer
     */
    @Test
    void testSignJars() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = GradleProjectBuilder.builder().withName('root').build()
        Project project = projectHelper.project

        Set<File> jarFilesToSign = [
                projectHelper.gradleJars[0],
        ]

        JarSigner jarSigner = buildDefaultJarSigner(projectHelper, project.file('cache'))
        jarSigner.setJarfilesToSign(jarFilesToSign)

        //testing signed file
        Map<File, File> signing1Map = jarSigner.signJars().each {File unsignedFile, File signedFile ->

            assert jarFilesToSign.contains(unsignedFile)
            assertSignedJar(signedFile)

            String md5 = new File("${signedFile.path}.md5").text
            assertMd5(unsignedFile, md5)
        }

        def debug = 0

    }

    /**
     * Verifiserer at filer blir re-signert ved oppdatering.
     */
    @Test
    void testSignJar() {

        ProjectHelper rootProjectHelper = GradleProjectBuilder.builder("rootProject").build()

        //forks a new project in a temp folder
        ProjectHelper java1ProjectHelper = GradleProjectBuilder.builder().withName("java1").applyJavaPlugin().withParent(rootProjectHelper).build()
        ProjectHelper java2ProjectHelper = GradleProjectBuilder.builder().withName("java2").applyJavaPlugin().withParent(rootProjectHelper).build()

        //some dummy code making up two jars
        use(WebstartTestutilFilewriter) {
            java1ProjectHelper.writeDynamicMethodsClassWithNMethods('src/main/java', 1, 1)
            java2ProjectHelper.writeDynamicMethodsClassWithNMethods('src/main/java', 1, 2)
        }

        java1ProjectHelper.executeTask('build')
        java2ProjectHelper.executeTask('build')

        File java1JarFile = java1ProjectHelper.project.file('build/libs/java1.jar')
        File java2JarFile = java2ProjectHelper.project.file('build/libs/java2.jar')

        File destinationDir = rootProjectHelper.project.file('gen/signed')
        Collection<File> jarFilesToSign = Collections.singleton(java1JarFile)




        JarSigner jarSigner = buildDefaultJarSigner(rootProjectHelper, destinationDir)
        jarSigner.setJarfilesToSign(jarFilesToSign)

        //testing signed file
        Map<File, File> signing1Map = jarSigner.signJars().each {File unsignedFile, File signedFile ->

            assert jarFilesToSign.contains(unsignedFile)
            assertSignedJar(signedFile)
            assertJarFileContainsAllEntries(signedFile, unsignedFile)

            String md5 = new File(signedFile.getPath()+'.md5').text
            assertMd5(unsignedFile, md5)
        }

        //updating 'java1' project jar by swapping it with jar produced by 'java2'
        File oldFile = new File(java1JarFile.parentFile, java1JarFile.getName() + ".old")
        FileUtils.copyFile(java1JarFile, oldFile)
        FileUtils.copyFile(java2JarFile, java1JarFile)
        assert java1JarFile.exists()

        //testing signed file - java1 should now ble updated
        Map<File, File> signing2Map = jarSigner.signJars().each {File unsignedFile, File signedFile ->

            assert jarFilesToSign.contains(unsignedFile)
            assertSignedJar(signedFile)
            assertJarFileContainsAllEntries(signedFile, unsignedFile)

            String md5 = new File("${signedFile.path}.md5").text
            assertMd5(unsignedFile, md5)
        }

    }

    /**
     * Beregner md5 hash verdi utifra filens innhold og asserter den mot forventet verdi
     */
    public static void assertMd5(File file, String expected) {
        Assert.assertEquals(FileHashIdent.createChecksum(file), expected, "Forventet hash verdi (MD5)")
    }

    /**
     * Asserts that the file has the jar extension, and that all the contents are signed.
     */
    public static void assertSignedJar(File file) {
        Assert.assertTrue(file.getName().endsWith('.jar'), "Jar fil skal ende på '.jar")
        JarFile jarFile = new JarFile(file, true);
    }

    /**
     * Asserts that all jar entries in <code>intersect</code> are contained in code>base</code>.
     * <br/>
     * This so that <code>'base' ? 'intersect' = 'intersect'</code>
     */
    public static void assertJarFileContainsAllEntries(File base, File intersect) {
        List<String> baseEntries = new JarFile(base).entries().collect() {it.name}
        List<String> intersectEntries = new JarFile(intersect).entries().collect() {it.name}

        intersectEntries.removeAll(baseEntries)
        if (!intersectEntries.isEmpty()) {
            Assert.fail("Forventet at filen ${base} inneholder aller entries ifra ${intersect}. Overflytende entries: ${intersectEntries}")
        }

    }
}