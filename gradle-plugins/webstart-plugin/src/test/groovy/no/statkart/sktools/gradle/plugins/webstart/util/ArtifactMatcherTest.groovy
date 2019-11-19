package no.statkart.sktools.gradle.plugins.webstart.util


import no.statkart.sktools.gradle.testutils.SampleJarTestutil
import org.testng.Assert
import org.testng.annotations.Test

import java.nio.file.Files

/**
 * Test av {@link ArtifactMatcher}
 *
 * @author Leif Lislegård
 * @author Tor Egil R. Strand
 */
class ArtifactMatcherTest {

    /**
     * Tester standard navn
     */
    @Test
    void testFilname() {

        ArtifactMatcher matcher = new ArtifactMatcher(new File('weblogic-wswar-plugin-1.1.jar'))

        Assert.assertEquals(matcher.getName(), 'weblogic-wswar-plugin')
        Assert.assertEquals(matcher.getVersion(), '1.1')
    }

    /**
     * Tester SNAPSHOT navngivning (version felt)
     */
    @Test
    void testFilnameSNAPSHOT() {

        ArtifactMatcher matcher = new ArtifactMatcher(new File('weblogic-wswar-plugin-1.1SNAPSHOT.jar'))

        Assert.assertEquals(matcher.getName(), 'weblogic-wswar-plugin')
        Assert.assertEquals(matcher.getVersion(), '1.1SNAPSHOT')
    }

    /**
     * Tester SNAPSHOT navngivning i classifier
     */
    @Test
    void testFilnameSNAPSHOT_Classifier() {

        ArtifactMatcher matcher = new ArtifactMatcher(new File('weblogic-wswar-plugin-1.1-SNAPSHOT.jar'))

        Assert.assertEquals(matcher.getName(), 'weblogic-wswar-plugin')
        Assert.assertEquals(matcher.getVersion(), '1.1-SNAPSHOT')
    }

    /**
     * Tester SNAPSHOT navngivning i classifier
     */
    @Test
    void testFilnameBetaSNAPSHOT_Classifier() {

        ArtifactMatcher matcher = new ArtifactMatcher(new File('weblogic-wswar-plugin-1.1beta-SNAPSHOT.jar'))

        Assert.assertEquals(matcher.getName(), 'weblogic-wswar-plugin')
        Assert.assertEquals(matcher.getVersion(), '1.1beta-SNAPSHOT')
    }

    /**
     * Tester RC navngivning
     */
    @Test
    void testFilnameRC() {

        ArtifactMatcher matcher = new ArtifactMatcher(new File('weblogic-wswar-plugin-1.1RC1.jar'))

        Assert.assertEquals(matcher.getName(), 'weblogic-wswar-plugin')
        Assert.assertEquals(matcher.getVersion(), '1.1RC1')
    }

    /**
     * Tester RC navngivning adskilt med bindestrek
     */
    @Test
    void testFilnameRC2() {

        ArtifactMatcher matcher = new ArtifactMatcher(new File('weblogic-wswar-plugin-1.1-RC1.jar'))

        Assert.assertEquals(matcher.getName(), 'weblogic-wswar-plugin')
        Assert.assertEquals(matcher.getVersion(), '1.1-RC1')
    }

    /**
     * Tester classifier navngivning
     */
    @Test
    void testFilnameClassifier() {

        ArtifactMatcher matcher = new ArtifactMatcher(new File('weblogic-wswar-plugin-1.1-someclassifier.jar'))

        Assert.assertEquals(matcher.getName(), 'weblogic-wswar-plugin')
        Assert.assertEquals(matcher.getVersion(), '1.1-someclassifier')
    }

    /**
     * Tester RC navngivning + classifier
     */
    @Test
    void testFilnameClassifierRC() {

        ArtifactMatcher matcher = new ArtifactMatcher(new File('weblogic-wswar-plugin-1.1RC1-someclassifier.jar'))

        Assert.assertEquals(matcher.getName(), 'weblogic-wswar-plugin')
        Assert.assertEquals(matcher.getVersion(), '1.1RC1-someclassifier')
    }

    /**
     * Tester gradle milestone navngivning
     */
    @Test
    void testFilenameGradleStyle() {
        ArtifactMatcher matcher = new ArtifactMatcher(new File('gradle-cli-1.0-milestone-7.jar'))

        Assert.assertEquals(matcher.getName(), 'gradle-cli')
        Assert.assertEquals(matcher.getVersion(), '1.0-milestone-7')
    }

    /**
     * Tester alternativ henting av version
     */
    @Test
    void testVersionViaManifest() {
        File simpleJarFile = Files.createTempFile("simple", ".jar").toFile()
        SampleJarTestutil.writeSampleJar(simpleJarFile)

        final String versionInManifest = ArtifactMatcher.findImplementationVersionInManifest(simpleJarFile)
        Assert.assertEquals(versionInManifest, SampleJarTestutil.VERSION_simple_jar)

        simpleJarFile.delete()
    }

    /**
     * Tester utviklingsversjonering av SKIF.
     */
    @Test
    void testSkifVersion() {
        ArtifactMatcher matcher = new ArtifactMatcher(new File('skif-placeholder-trunk-build42.jar'))

        Assert.assertEquals(matcher.getName(), 'skif-placeholder')
        Assert.assertEquals(matcher.getVersion(), 'trunk-build42')
        Assert.assertEquals(matcher.getType(), 'jar')
    }

    /**
     * Tester utviklingsversjonering av SKIF.
     */
    @Test
    void testDevVersion() {
        ArtifactMatcher matcher = new ArtifactMatcher(new File('matrikkelspif-dev2.10.jar'))

        Assert.assertEquals(matcher.getName(), 'matrikkelspif')
        Assert.assertEquals(matcher.getVersion(), 'dev2.10')
        Assert.assertEquals(matcher.getType(), 'jar')
    }

    /**
     * Tester filnavnet til javax inject.
     */
    @Test
    void testJavaxInject() {
        ArtifactMatcher matcher = new ArtifactMatcher(new File('javax.inject-1.jar'))

        Assert.assertEquals(matcher.getName(), 'javax.inject')
        Assert.assertEquals(matcher.getVersion(), '1')
        Assert.assertEquals(matcher.getType(), 'jar')
    }
}