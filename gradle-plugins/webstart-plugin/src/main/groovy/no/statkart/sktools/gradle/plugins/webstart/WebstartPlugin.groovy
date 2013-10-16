package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.plugins.webstart.util.ArtifactMatcher
import org.apache.commons.lang.StringUtils
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.bundling.War
import org.gradle.util.GUtil
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

    public static final String WEBSTART_CONVENTION_NAME = 'webstart'
    public static final String SIGN_TASK_PREFIX = 'sign'
    public static final String WEBSTART_TASK_PREFIX = 'gen'
    public static final String WEBSTART_TASK_POSTFIX = 'Jnlp'

    @Override
    void apply(Project project) {
        project.plugins.apply(WarPlugin)

        WebstartConvention convention = new WebstartConvention(project)
        project.convention.plugins.put(WEBSTART_CONVENTION_NAME, convention)

        convention.webstart.all(new Action<ClientConfiguration>() {
            @Override
            void execute(ClientConfiguration clientConfiguration) {
                configureClient(project, clientConfiguration)
            }
        })
    }

    private static void configureClient(Project project, ClientConfiguration clientConfiguration) {
        JarSigner jarSigner = configureJarSigner(project, clientConfiguration)
        WebstartTask genJnlp = configureGenJnlp(project, clientConfiguration, jarSigner)
        configureWar(project, clientConfiguration, jarSigner, genJnlp)
    }

    private static JarSigner configureJarSigner(Project project, ClientConfiguration clientConfiguration) {

        final JarSigner jarSigner;
        if (project.getGradle().getGradleVersion().compareTo("1.5") > 0 ) {
            jarSigner = project.tasks.replace(makeTaskName(SIGN_TASK_PREFIX, clientConfiguration.name, null), JarSigner.class)  //todo: endre bruk av replace() til create()
        } else {
            jarSigner = project.tasks.add(makeTaskName(SIGN_TASK_PREFIX, clientConfiguration.name, null), JarSigner.class) //todo: remove backward compability with Gradle 1.5
        }

        jarSigner.jarFilesToSign = clientConfiguration.jarDependencies
        jarSigner.conventionMapping.certificateFile = { clientConfiguration?.signingConfiguration?.keystore }
        jarSigner.conventionMapping.alias = { clientConfiguration?.signingConfiguration?.alias }
        jarSigner.conventionMapping.password = { clientConfiguration?.signingConfiguration?.password }
        jarSigner.manifestAttributes = clientConfiguration.manifestAttributes

        return jarSigner
    }

    private static WebstartTask configureGenJnlp(Project project, ClientConfiguration clientConfiguration, JarSigner jarSigner) {

        final WebstartTask webstartTask;
        if (project.getGradle().getGradleVersion().compareTo("1.5") > 0 ) {
            webstartTask = project.tasks.replace(makeTaskName(WEBSTART_TASK_PREFIX, clientConfiguration.name, WEBSTART_TASK_POSTFIX), WebstartTask.class)  //todo: endre bruk av replace() til create()
        } else {
            webstartTask = project.tasks.add(makeTaskName(WEBSTART_TASK_PREFIX, clientConfiguration.name, WEBSTART_TASK_POSTFIX), WebstartTask.class) //todo: remove backward compability with Gradle 1.5
        }

        webstartTask.setJnlpConfigurations(clientConfiguration.jnlpConfigurations)
        webstartTask.conventionMapping.libDir = { clientConfiguration.libDir }
        webstartTask.jarResources jarSigner
        webstartTask.conventionMapping.map('digest', new Callable<String>() {
            @Override
            String call() throws Exception {
                return clientConfiguration.signingConfiguration?.digest
            }
        })
        return webstartTask
    }

    private static void configureWar(Project project, ClientConfiguration clientConfiguration, JarSigner jarSigner, WebstartTask webstartTask) {
        War war = project.tasks.getByName(WarPlugin.WAR_TASK_NAME) as War

        war.from { clientConfiguration.warJnlps ? webstartTask : [] }
        war.with(jnlpCopySpec(project, { clientConfiguration.libDir }, jarSigner, { clientConfiguration.signingConfiguration?.digest }))
    }

    private static String makeTaskName(String verb, String client, String postfix) {
        if (postfix != null) {
            return StringUtils.uncapitalize(String.format("%s%s%s", verb, GUtil.toCamelCase(client), StringUtils.capitalize(postfix)));
        } else {
            return StringUtils.uncapitalize(String.format("%s%s", verb, GUtil.toCamelCase(client)));
        }
    }

    public static CopySpec jnlpCopySpec(Project project, Object libDir, Object libs, Callable<String> digestProvider) {
        return project.copySpec {
            into libDir
            from libs
            eachFile(new Action<FileCopyDetails>() {
                @Override
                void execute(FileCopyDetails t) {
                    ArtifactMatcher artifactMatcher = new ArtifactMatcher(t.file)
                    String digest = digestProvider.call()
                    t.name = "${artifactMatcher.name}__V${artifactMatcher.version}${digest != null ? '-' + digest : ''}.${artifactMatcher.type}"
                }
            })
        }
    }
}
