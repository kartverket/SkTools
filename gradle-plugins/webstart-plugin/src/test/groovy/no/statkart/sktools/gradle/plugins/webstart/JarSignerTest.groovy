package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.plugins.webstart.util.FileHashIdent
import no.statkart.sktools.gradle.testutils.KeystoreTestutil
import no.statkart.sktools.gradle.testutils.SampleJarTestutil
import no.statkart.sktools.gradle.testutils.TestKitBase
import org.apache.commons.codec.binary.Hex
import org.assertj.core.api.Assertions
import org.gradle.api.Project
import org.testng.Assert
import org.testng.annotations.Test

import java.nio.file.Files
import java.util.jar.JarFile

import static no.statkart.sktools.gradle.testutils.KeystoreTestutil.KeystoreType.JKS
import static no.statkart.sktools.gradle.testutils.KeystoreTestutil.KeystoreType.P12
import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.util.Preconditions.checkState

/**
 * Test av {@link JarSigner}
 *
 * @author Leif Lislegård
 */
class JarSignerTest extends TestKitBase {

    /**
     * Tester caching av signerte jar filer
     */
    @Test
    void testSignJarsCache() {
        File certificateFile = file('kodesignering.jks')
        KeystoreTestutil.writeKodesigneringssertifikat(JKS, certificateFile)
        File sampleJarFile = SampleJarTestutil.writeSampleJar(file('lib/sample.jar'))

        //forks a new project in a temp folder
        Project root = projectBuilder().withName('root').build()

        JarSigner jarSigner1 = root.tasks.create('jarSigner1', JarSigner)
        jarSigner1.setCertificateFile(certificateFile)
        jarSigner1.setPassword(KeystoreTestutil.KeystorePassword)
        jarSigner1.setAlias(KeystoreTestutil.KeystoreAlias)
        jarSigner1.setJarFilesToSign(sampleJarFile)

        assertThat(jarSigner1.getSignedArtifactsForCertificates()).as("cache of signed files").isEmpty()

        //testing signed file
        jarSigner1.signJars()

        assertThat(jarSigner1.getSignedArtifactsForCertificates()).as("cache of signed files").hasSize(1)
        assertThat(jarSigner1.getSignedArtifactsForCertificates().values()).as("cache of signed files").hasSize(1)
        assertThat(jarSigner1.getSignedArtifactsForCertificates().values().iterator().next() as Map).containsOnlyKeys("sample.jar")

        assertThat(jarSigner1.outputs.files.getSingleFile()).hasName("sample.jar")
            .hasBinaryContent(Files.readAllBytes(jarSigner1.getSignedArtifactsForCertificates().values().asList()[0].get("sample.jar").file.toPath()))


        final long modified1 = jarSigner1.outputs.files.singleFile.lastModified()

        Project subProject = projectBuilder().withParent(root).build()
        JarSigner jarSigner2 = subProject.tasks.create('jarSigner2', JarSigner)
        jarSigner2.setCertificateFile(certificateFile)
        jarSigner2.setPassword(KeystoreTestutil.KeystorePassword)
        jarSigner2.setAlias(KeystoreTestutil.KeystoreAlias)

        //forventer at ny instans konstruerer cache som inneholder forrige signering..
        assertThat(jarSigner2.getSignedArtifactsForCertificates())
            .hasSize(1).isEqualTo(jarSigner1.getSignedArtifactsForCertificates());

        Thread.sleep(1000) //venter ett sekund for evt ulik timestamp

        jarSigner2.setJarFilesToSign(sampleJarFile)
        jarSigner2.signJars();
        long modified2 = jarSigner2.outputs.files.singleFile.lastModified()


        Assert.assertEquals(jarSigner2.outputs.files.collect {it.name}, jarSigner1.outputs.files.collect {it.name}, 'forventet samme sett av filer')
        assertThat(modified1).as('forventer at cached fil er urørt').isEqualTo(modified2)

        assertThat(jarSigner1.didSignJarFile).isTrue()
        assertThat(jarSigner2.didSignJarFile)
            .as("Jar signer eksekvert nr 2 skal kun ha brukt cache [SKTOOLS-184]").isFalse()
    }


    /**
     * Tester signering av jar-filer
     */
    @Test
    void testSignJars() {
        File certificateFile = file('kodesignering.jks')
        KeystoreTestutil.writeKodesigneringssertifikat(JKS, certificateFile)
        File sampleJarFile = SampleJarTestutil.writeSampleJar(file('lib/sample.jar'))

        //forks a new project in a temp folder
        Project root = projectBuilder().withName('root').build()

        JarSigner jarSigner = root.tasks.create('jarSigner1', JarSigner)
        jarSigner.setCertificateFile(certificateFile)
        jarSigner.setPassword(KeystoreTestutil.KeystorePassword)
        jarSigner.setAlias(KeystoreTestutil.KeystoreAlias)
        jarSigner.manifestAttribute('Permissions', 'sandbox')
        jarSigner.setJarFilesToSign(sampleJarFile)


        //testing signed file
        jarSigner.signJars()

        jarSigner.outputs.files.singleFile.with { File signedFile ->

            assertThat(signedFile).hasName("sample.jar")
            assertSignedJar(signedFile)

            String md5 = new File(getCertificateCacheDir(jarSigner), signedFile.name+'.md5').text
            assertMd5(sampleJarFile, md5)
        }
    }

    /**
     * Tester signering med P12 sertifikat
     */
    @Test
    void testSigningWithP12() {
        File certificateFile = file('kodesignering.p12')
        KeystoreTestutil.writeKodesigneringssertifikat(P12, certificateFile)
        File sampleJarFile = SampleJarTestutil.writeSampleJar(file('lib/sample.jar'))

        //forks a new project in a temp folder
        Project root = projectBuilder().withName('root').build()

        JarSigner jarSigner = root.tasks.create('jarSigner1', JarSigner)
        jarSigner.setCertificateFile(certificateFile)
        jarSigner.setPassword(KeystoreTestutil.KeystorePassword)
        jarSigner.setAlias(KeystoreTestutil.KeystoreAlias)
        jarSigner.manifestAttribute('Permissions', 'sandbox')
        jarSigner.setJarFilesToSign(sampleJarFile)


        //testing signed file
        jarSigner.signJars()

        jarSigner.outputs.files.singleFile.with { File signedFile ->

            assertThat(signedFile).hasName("sample.jar")
            assertSignedJar(signedFile)

            String md5 = new File(getCertificateCacheDir(jarSigner), signedFile.name+'.md5').text
            assertMd5(sampleJarFile, md5)
        }
    }

    /**
     * Verifiserer at filer blir re-signert ved oppdatering.
     */
    @Test
    void testSignJar() {
        File certificateFile = file('kodesignering.jks')
        KeystoreTestutil.writeKodesigneringssertifikat(JKS, certificateFile)

        Project root = projectBuilder().withName('root').build()

        File java1JarFile = SampleJarTestutil.writeSampleJar(file('unsigned/sample.jar'))

        JarSigner jarSigner = root.tasks.create('jarSigner1', JarSigner)
        jarSigner.setCertificateFile(certificateFile)
        jarSigner.setPassword(KeystoreTestutil.KeystorePassword)
        jarSigner.setAlias(KeystoreTestutil.KeystoreAlias)
        jarSigner.manifestAttribute('Permissions', 'sandbox')
        jarSigner.setJarFilesToSign(java1JarFile)


        //testing signed file
        jarSigner.signJars()

        File unsignedFile1 = java1JarFile
        File signedFile1 = jarSigner.outputs.files.singleFile

        assertSignedJar(signedFile1)
        assertJarFileContainsAllEntries(signedFile1, unsignedFile1)

        String md51 = new File(getCertificateCacheDir(jarSigner), signedFile1.getName()+'.md5').text
        assertMd5(unsignedFile1, md51)

        //updating 'java1' project jar by swapping it with jar produced by 'java2'
        SampleJarTestutil.writeSample2Jar(unsignedFile1)

        //testing signed file - java1 should now ble updated
        jarSigner.signJars()

        File unsignedFile2 = java1JarFile
        File signedFile2 = jarSigner.outputs.files.singleFile

        checkState(jarSigner.getJarFilesToSign().contains(unsignedFile2), "jarFilesToSign contains unsigned file");
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
