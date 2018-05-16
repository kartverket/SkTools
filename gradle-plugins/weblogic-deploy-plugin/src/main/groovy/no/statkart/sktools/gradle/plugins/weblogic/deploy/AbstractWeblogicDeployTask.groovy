package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec

import java.util.jar.JarFile
import java.util.jar.Manifest

/**
 * Alt som er felles for deploying og undeploying.
 *
 * @author Tor Egil R. Strand
 * @since 1.2
 */
abstract class AbstractWeblogicDeployTask extends ConventionTask {

    @InputFiles
    FileCollection classpath


    @Input
    String deploymentName

    @Optional
    @Input
    String getTargets() {
        targets == null || targets.isAllWhitespace() ? null : targets
    }
    String targets

    @Input
    String url
    @Input
    String username
    @Input
    String password


    @Input
    @Optional
    String getTimeout() {
        if (timeout != null) {
            return isTimeoutInMilliseconds() ? timeout + "000" : timeout;
        }
        return null;
    }
    String timeout

    @Input
    boolean failOnError = false
    @Input
    boolean verbose = true


    AbstractWeblogicDeployTask() {
        super()
        group = 'Deployment'
        outputs.upToDateWhen { false }
    }

    @TaskAction
    final void exec() {
        def cp = getClasspath()
        def result = project.javaexec {
            main = 'weblogic.Deployer'
            classpath = cp

            systemProperty('weblogic.security.SSL.hostnameVerifier', 'weblogic.security.utils.SSLWLSWildcardHostnameVerifier')

            args('-adminurl', getUrl())
            args('-username', getUsername())
            args('-password', getPassword())

            buildCommandLine(it)

            args('-targets', getTargets())
            args('-name', getDeploymentName())

            if (isVerbose()) {
                args('-verbose')
            }

            if (getTimeout() != null) {
                args('-timeout', getTimeout())
            }
        }

        if (isFailOnError()) {
            result.assertNormalExitValue()
        }
    }

    protected abstract void buildCommandLine(JavaExecSpec spec)


    public abstract Logger getLogger();

    protected boolean isTimeoutInMilliseconds() {
        String version = findWeblogicVersion()
        if (version != null && version.startsWith("12.1.3.")) {
            return true;
        }
        return false;
    }

    protected String findWeblogicVersion() {
        final File file = getClasspath().getAsFileTree().filter { "weblogic.jar" == it.name }.getSingleFile();
        final Manifest manifest = new JarFile(file, false).getManifest();
        if (manifest != null) {
            return manifest.getMainAttributes().getValue("Implementation-Version");
        }
        return null;
    }

}
