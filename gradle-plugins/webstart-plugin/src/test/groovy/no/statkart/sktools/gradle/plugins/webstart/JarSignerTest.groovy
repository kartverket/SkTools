package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.plugins.webstart.util.FileHashIdent
import org.apache.commons.codec.binary.Hex
import org.assertj.core.api.Assertions

import java.util.jar.JarFile
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import no.statkart.sktools.gradle.testutils.filewriter.WebstartTestutilFilewriter
import org.testng.Assert
import org.testng.annotations.Test
import org.gradle.api.Project

import static org.assertj.core.util.Preconditions.checkState

/**
 * Test av {@link JarSigner}
 *
 * @author Leif Lislegård
 */
class JarSignerTest {

    /**
     * Instansierer opp en standard JarSigner med sertifikatfil, passord og alias.
     */
    private JarSigner buildDefaultJarSigner(ProjectHelper projectHelper, String name) {

        //jks certificate
        use(WebstartTestutilFilewriter) {
            projectHelper.writeKodesignerinSertifikat('.')
        }
        File certificateFile = projectHelper.project.file('kodesignering.jks')
        checkState(certificateFile.exists(), "kodesignering.jks available as test resources");

        //configures the jar signer
        return projectHelper.project.task(name, type: JarSigner) { JarSigner task ->
            task.setCertificateFile(certificateFile)
            task.setPassword(WebstartTestutilFilewriter.KeystorePassword)
            task.setAlias(WebstartTestutilFilewriter.KeystoreAlias)
            task.manifestAttribute('Permissions', 'sandbox')
        } as JarSigner
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

        JarSigner jarSigner1 = buildDefaultJarSigner(projectHelper, 'sign1')
        jarSigner1.setJarFilesToSign(jarFilesToSign)

        Assert.assertEquals(jarSigner1.signedArtifactsForCertificates.size(), 0, 'forventet tomt cache')

        //testing signed file
        jarSigner1.signJars()
        long modified1 = jarSigner1.outputs.files.singleFile.lastModified()
        jarSigner1.with { JarSigner jarSigner ->
            Assert.assertEquals(jarSigner.signedArtifactsForCertificates.size(), 1)
            Assert.assertEquals(jarSigner.signedArtifactsForCertificates.values().asList()[0].size(), 1)
            Assert.assertEquals(jarSigner.signedArtifactsForCertificates.values().asList()[0].values().collect {it.file}.size(), 1)
            Assertions.assertThat(jarSigner.signedArtifactsForCertificates.values().asList()[0].values().collect {it.file.name}).
                    containsAll(jarSigner.outputs.files.collect {it.name})
        }

        JarSigner jarSigner2 = buildDefaultJarSigner(projectHelper, 'sign2')
        //forventer at ny instans konstruerer samme cache..
        jarSigner2.with { JarSigner jarSigner ->
            Assert.assertEquals(jarSigner.signedArtifactsForCertificates.size(), 1)
            Assert.assertEquals(jarSigner.signedArtifactsForCertificates.values().asList()[0].size(), 1)
            Assert.assertEquals(jarSigner.signedArtifactsForCertificates.values().asList()[0].values().collect {it.file}.size(), 1)
            Assertions.assertThat(jarSigner.signedArtifactsForCertificates.values().asList()[0].values().collect {it.file.name}).
                    containsAll(jarSigner.outputs.files.collect {it.name})
        }

        Thread.sleep(1000) //venter ett sekund for evt ulik timestamp

        jarSigner2.setJarFilesToSign(jarFilesToSign)
        jarSigner2.signJars();
        long modified2 = jarSigner2.outputs.files.singleFile.lastModified()


        Assert.assertEquals(jarSigner2.outputs.files.collect {it.name}, jarSigner1.outputs.files.collect {it.name}, 'forventet samme sett av filer')
        Assert.assertEquals(modified2, modified1, 'forventer at cached fil er urørt')
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

        JarSigner jarSigner = buildDefaultJarSigner(projectHelper, 'sign')
        jarSigner.setJarFilesToSign(jarFilesToSign)

        //testing signed file
        jarSigner.signJars()

        def jarNames = jarFilesToSign.collect {it.name}

        jarSigner.outputs.files.each { File signedFile ->

            Assert.assertTrue(jarNames.contains(signedFile.name))
            assertSignedJar(signedFile)

            String md5 = new File(getCertificateCacheDir(jarSigner), signedFile.name+'.md5').text
            File unsignedFile = jarFilesToSign.find {it.name == signedFile.name}
            assertMd5(unsignedFile, md5)
        }
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




        JarSigner jarSigner = buildDefaultJarSigner(rootProjectHelper, 'sign')
        jarSigner.setJarFilesToSign(jarFilesToSign)

        //testing signed file
        jarSigner.signJars()

        File unsignedFile1 = java1JarFile
        File signedFile1 = jarSigner.outputs.files.singleFile

        checkState(jarFilesToSign.contains(unsignedFile1), "jarFilesToSign contains unsigned file");
        assertSignedJar(signedFile1)
        assertJarFileContainsAllEntries(signedFile1, unsignedFile1)

        String md51 = new File(getCertificateCacheDir(jarSigner), signedFile1.getName()+'.md5').text
        assertMd5(unsignedFile1, md51)

        //updating 'java1' project jar by swapping it with jar produced by 'java2'
        File oldFile = new File(java1JarFile.parentFile, java1JarFile.getName() + ".old")
        ProjectHelper.copyFile(java1JarFile, oldFile)
        ProjectHelper.copyFile(java2JarFile, java1JarFile)
        checkState(java1JarFile.exists(), "java1JarFile should be have been copied and exist")

        //testing signed file - java1 should now ble updated
        jarSigner.signJars()

        File unsignedFile2 = java1JarFile
        File signedFile2 = jarSigner.outputs.files.singleFile

        checkState(jarFilesToSign.contains(unsignedFile2), "jarFilesToSign contains unsigned file");
        assertSignedJar(signedFile2)
        assertJarFileContainsAllEntries(signedFile2, unsignedFile2)

        String md52 = new File(getCertificateCacheDir(jarSigner), signedFile2.getName()+'.md5').text
        assertMd5(unsignedFile2, md52)

    }

    /**
     * Beregner md5 hash verdi utifra filens innhold og asserter den mot forventet verdi
     */
    public static void assertMd5(File file, String expected) {
        def digest = FileHashIdent.createDigest(file)
        Assertions.assertThat(FileHashIdent.hexEncoded(digest)).
                as("Forventet hex representasjon").isEqualTo(new String(Hex.encodeHex(digest)))

        Assertions.assertThat(FileHashIdent.createChecksum(file)).
                as("Forventet hash verdi (MD5)").isEqualTo(expected);
    }

    /**
     * Asserts that the file has the jar extension, and that all the contents are signed.
     */
    public static void assertSignedJar(File file) {
        Assert.assertTrue(file.getName().endsWith('.jar'), "Jar fil skal ende på '.jar")
        JarFile jarFile = new JarFile(file, true);
        try {
            Assertions.assertThat(jarFile.manifest.mainAttributes.getValue("Permissions"))
                    .describedAs("Permissions").isEqualTo("sandbox");
        } finally {
            if (jarFile != null) jarFile.close()
        }
    }

    /**
     * Asserts that all jar entries in <code>intersect</code> are contained in code>base</code>.
     * <br/>
     * This so that <code>'base' ? 'intersect' = 'intersect'</code>
     */
    public static void assertJarFileContainsAllEntries(File base, File intersect) {
        JarFile jarFileBase, jarFileIntersect;
        try {
            jarFileBase = new JarFile(base)
            jarFileIntersect = new JarFile(intersect)

            List<String> baseEntries = jarFileBase.entries().collect() {it.name}
            List<String> intersectEntries = jarFileIntersect.entries().collect() {it.name}

            Assertions.assertThat(baseEntries).containsAll(intersectEntries)
        } finally {
            if (jarFileBase != null) {
                jarFileBase.close()
            }
            if (jarFileIntersect != null) {
                jarFileIntersect.close()
            }
        }

    }

    static File getCertificateCacheDir(JarSigner jarSigner) {
        jarSigner.getCertificateCacheDir(jarSigner.getCertificateHashIdent());
    }
}