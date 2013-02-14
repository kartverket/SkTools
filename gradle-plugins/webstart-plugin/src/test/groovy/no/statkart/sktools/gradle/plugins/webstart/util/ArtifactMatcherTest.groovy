package no.statkart.sktools.gradle.plugins.webstart.util

import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency
import org.gradle.api.tasks.util.PatternSet
import org.testng.annotations.Test

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

        File file = new File('weblogic-wswar-plugin-1.1.jar')

        assert ArtifactMatcher.getArtifactName(file) == 'weblogic-wswar-plugin'
        assert ArtifactMatcher.getArtifactVersion(file) == '1.1'
    }

    /**
     * Tester SNAPSHOT navngivning (version felt)
     */
    @Test
    void testFilnameSNAPSHOT() {

        File file = new File('weblogic-wswar-plugin-1.1SNAPSHOT.jar')

        assert ArtifactMatcher.getArtifactName(file) == 'weblogic-wswar-plugin'
        assert ArtifactMatcher.getArtifactVersion(file) == '1.1SNAPSHOT'
    }

    /**
     * Tester SNAPSHOT navngivning i classifier
     */
    @Test
    void testFilnameSNAPSHOT_Classifier() {

        File file = new File('weblogic-wswar-plugin-1.1-SNAPSHOT.jar')

        assert ArtifactMatcher.getArtifactName(file) == 'weblogic-wswar-plugin'
        assert ArtifactMatcher.getArtifactVersion(file) == '1.1-SNAPSHOT'
    }

    /**
     * Tester SNAPSHOT navngivning i classifier
     */
    @Test
    void testFilnameBetaSNAPSHOT_Classifier() {

        File file = new File('weblogic-wswar-plugin-1.1beta-SNAPSHOT.jar')

        assert ArtifactMatcher.getArtifactName(file) == 'weblogic-wswar-plugin'
        assert ArtifactMatcher.getArtifactVersion(file) == '1.1beta-SNAPSHOT'
    }

    /**
     * Tester RC navngivning
     */
    @Test
    void testFilnameRC() {

        File file = new File('weblogic-wswar-plugin-1.1RC1.jar')

        assert ArtifactMatcher.getArtifactName(file) == 'weblogic-wswar-plugin'
        assert ArtifactMatcher.getArtifactVersion(file) == '1.1RC1'
    }

    /**
     * Tester RC navngivning adskilt med bindestrek
     */
    @Test
    void testFilnameRC2() {

        File file = new File('weblogic-wswar-plugin-1.1-RC1.jar')

        assert ArtifactMatcher.getArtifactName(file) == 'weblogic-wswar-plugin'
        assert ArtifactMatcher.getArtifactVersion(file) == '1.1-RC1'
    }

    /**
     * Tester classifier navngivning
     */
    @Test
    void testFilnameClassifier() {

        File file = new File('weblogic-wswar-plugin-1.1-someclassifier.jar')

        assert ArtifactMatcher.getArtifactName(file) == 'weblogic-wswar-plugin'
        assert ArtifactMatcher.getArtifactVersion(file) == '1.1-someclassifier'
    }

    /**
     * Tester RC navngivning + classifier
     */
    @Test
    void testFilnameClassifierRC() {

        File file = new File('weblogic-wswar-plugin-1.1RC1-someclassifier.jar')

        assert ArtifactMatcher.getArtifactName(file) == 'weblogic-wswar-plugin'
        assert ArtifactMatcher.getArtifactVersion(file) == '1.1RC1-someclassifier'
    }

    /**
     * Tester gradle milestone navngivning
     */
    @Test
    void testFilenameGradleStyle() {
        File file = new File('gradle-cli-1.0-milestone-7.jar')

        assert ArtifactMatcher.getArtifactName(file) == 'gradle-cli'
        assert ArtifactMatcher.getArtifactVersion(file) == '1.0-milestone-7'
    }

    /**
     * Tester alternativ henting av version
     */
    @Test
    void testVersionViaManifest() {

        ProjectHelper projectHelper = GradleProjectBuilder.builder().build()

        File gradleJarFile = ((DefaultSelfResolvingDependency) projectHelper.project.getDependencies().gradleApi()).getSource().getAsFileTree().matching(new PatternSet(includes: ['**/*gradle-core*.jar'])).files.iterator().next()

        assert gradleJarFile


        assert ArtifactMatcher.findImplementationVersionInManifest(gradleJarFile) == projectHelper.project.gradle.getGradleVersion()

    }

    /**
     * Tester utviklingsversjonering av SKIF.
     */
    @Test
    void testSkifVersion() {
        File file = new File('skif-placeholder-trunk-build42.jar')

        assert ArtifactMatcher.getArtifactName(file) == 'skif-placeholder'
        assert ArtifactMatcher.getArtifactVersion(file) == 'trunk-build42'
        assert ArtifactMatcher.getArtifactType(file) == 'jar'
    }

    /**
     * Tester utviklingsversjonering av SKIF.
     */
    @Test
    void testDevVersion() {
        File file = new File('matrikkelspif-dev2.10.jar')

        assert ArtifactMatcher.getArtifactName(file) == 'matrikkelspif'
        assert ArtifactMatcher.getArtifactVersion(file) == 'dev2.10'
        assert ArtifactMatcher.getArtifactType(file) == 'jar'
    }

    /**
     * Tester filnavnet til javax inject.
     */
    @Test
    void testJavaxInject() {
        File file = new File('javax.inject-1.jar')

        assert ArtifactMatcher.getArtifactName(file) == 'javax.inject'
        assert ArtifactMatcher.getArtifactVersion(file) == '1'
        assert ArtifactMatcher.getArtifactType(file) == 'jar'
    }
}