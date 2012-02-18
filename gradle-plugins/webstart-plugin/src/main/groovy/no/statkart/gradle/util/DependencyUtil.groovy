package no.statkart.gradle.util

import org.gradle.api.GradleException

class DependencyUtil {
    /**
     * Find the java tools library file
     *
     * TODO: Hack until http://issues.gradle.org/browse/GRADLE-1477
     */
    static File getJavaTools() {
        String javaHome = System.properties['java.home']
        if (System.properties['os.name'].toLowerCase().contains('mac os'))
            new File(javaHome, '../classes/classes.jar')
        else
            new File(javaHome, '../lib/tools.jar')
    }

    static List<File> getJnlpJars() {
        String JNLP_JAR_DIR = new File(System.getenv('JAVA_HOME'), 'sample/jnlp/servlet/')
        List<File> jnlpFiles = [new File(JNLP_JAR_DIR, 'jnlp.jar'), new File(JNLP_JAR_DIR, 'jnlp-servlet.jar'), new File(JNLP_JAR_DIR, 'jardiff.jar')]
        jnlpFiles.each { if (!it.exists()) throw new GradleException("File $it.path doesn't exist") }
        jnlpFiles
    }

    static String artifactMatcher = /(.*)-([\d][^-]*((-[\dA-Z]+)+|-v[\d]+)?)(-[a-zA-Z]+)?\.[a-zA-Z_][^\.]*/

    static String getArtifactVersion(File file) {
        checkArtifactMatcher(file)
        file.name.replaceAll(artifactMatcher, '$2')
    }

    private static def checkArtifactMatcher(File file) {
        if (!(file.name ==~ artifactMatcher)) {
            throw new GradleException("Unable to find version of file $file.path")
        }
    }

    static String getArtifactName(File file) {
        checkArtifactMatcher(file)
        file.name.replaceAll(artifactMatcher, '$1')
    }

}
