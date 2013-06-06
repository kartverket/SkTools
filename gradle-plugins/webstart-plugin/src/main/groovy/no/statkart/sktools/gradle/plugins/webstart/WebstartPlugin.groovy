package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.plugins.webstart.util.ArtifactMatcher
import org.apache.commons.io.FileUtils
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.ConventionTask
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.bundling.War
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

/**
 * For generering av webstart klienter og distribusjoner. <br />
 * Har funksjonalitet for generering av jnlp-filer, jar-ressurser, signering og enkel war distribuering.
 *
 * @since 1.2
 * @author Tor Egil R. Strand
 */
class WebstartPlugin implements Plugin<Project> {
    private static Logger logger = LoggerFactory.getLogger(WebstartPlugin.class)

    public static final String SIGN_TASK_NAME = 'signJars'
    public static final String WEBSTART_TASK_NAME = 'webstart'

    /**
     * Steg for mapping av navn for ressurs-filer til lib katalog
     * <p>
     * Versjonsfelt vil også få tillagt parameterisert {@code digest}. <br>
     */
    public static String createFileNameForJar(ArtifactMatcher artifactMatcher, String digest) {
        return "${artifactMatcher.name}__V${artifactMatcher.version}${digest != null ? digest : ''}.${artifactMatcher.type}"
    }

    @Override
    void apply(Project project) {
        project.plugins.apply(WarPlugin)

        configureSignJarTask(project)
        configureWebstartTask(project)
        configureWar(project)
    }

    JarSigner configureSignJarTask(Project project) {
        JarSigner jarSigner = project.tasks.add(SIGN_TASK_NAME, JarSigner)
        return jarSigner
    }

    WebstartTask configureWebstartTask(Project project) {
        WebstartTask webstartTask = project.tasks.add(WEBSTART_TASK_NAME, WebstartTask)
        webstartTask.getConventionMapping().map('digest') {
            JarSigner jarSigner = project.tasks.getByName(SIGN_TASK_NAME) as JarSigner
            return jarSigner.digest
        }
        return webstartTask
    }

    void configureWar(Project project) {
        War war = project.tasks.getByName(WarPlugin.WAR_TASK_NAME) as War
        war.from({project.tasks.getByName(WEBSTART_TASK_NAME)})

        war.into({
            WebstartTask task = project.tasks.getByName(WEBSTART_TASK_NAME) as WebstartTask
            task.libDir
        }) {
            from {
                WebstartTask task = project.tasks.getByName(WEBSTART_TASK_NAME) as WebstartTask
                task.jarDependencies
            }
            eachFile { FileCopyDetails details ->
                JarSigner jarSigner = project.tasks.getByName(SIGN_TASK_NAME) as JarSigner
                ArtifactMatcher artifactMatcher = new ArtifactMatcher(details.file)
                details.name = createFileNameForJar(artifactMatcher, jarSigner.digest)
            }
        }
    }
}


