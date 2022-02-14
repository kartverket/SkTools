package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec

/**
 * Alt som er felles for deploying og undeploying.
 * <br>
 * <p>
 * Dokumentasjon for {@code weblogic.Deployer}:
 * <ul>
 * <li> <a href="https://docs.oracle.com/middleware/12213/wls/DEPGD/wldeployer.htm">Oracle® Fusion Middleware Deploying Applications to Oracle WebLogic Server 12.2.1.3</a> </li>
 * <li> <a href="https://docs.oracle.com/middleware/1213/wls/DEPGD/wldeployer.htm">Fusion Middleware Deploying Applications to Oracle WebLogic Server 12.1.3</a> </li>
 * <li> <a href="https://docs.oracle.com/middleware/11119/wls/DEPGD/wldeployer.htm">Fusion Middleware Deploying Applications to Oracle WebLogic Server 10.3.6</a> </li>
 * <ul>
 *
 * @author Tor Egil R. Strand
 * @since 1.2
 */
abstract class AbstractWeblogicDeployTask extends ConventionTask {

    /**
     * Classpath for WebLogic
     */
    @InputFiles
    FileCollection classpath


    /**
     * Navn på deployment
     */
    @Input
    String deploymentName

    /**
     * Targets det skal deployes til (navn på servere eller clustere)
     */
    @Optional
    @Input
    String getTargets() {
        targets == null || targets.isAllWhitespace() ? null : targets
    }
    String targets

    /**
     * URL til AdminServer
     */
    @Input
    String url
    /**
     * Administratorbrukernavn for WebLogic
     */
    @Input
    String username
    /**
     * Administratorpassord for WebLogic
     */
    @Input
    String password

    /**
     * Timeout for WebLogic-verktøyet.
     * PS: En har en navnekonflikt her med nyere versjoner av Gradle.
     *
     */
    @Input
    @Optional
    String timeout

    /**
     * Om tasken skal feile bygget om den feiler. Standardverdi er false.
     */
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
        project.javaexec {
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

            setIgnoreExitValue(!isFailOnError())
        }
    }

    protected abstract void buildCommandLine(JavaExecSpec spec)


    public abstract Logger getLogger();

}
