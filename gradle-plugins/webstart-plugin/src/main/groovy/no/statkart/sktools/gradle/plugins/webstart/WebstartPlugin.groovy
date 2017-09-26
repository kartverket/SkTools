package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.plugins.webstart.util.ArtifactMatcher
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.bundling.War
import org.gradle.util.GUtil

import java.util.concurrent.Callable

/**
 * For generering av webstart klienter og distribusjoner. <br />
 * Har funksjonalitet for generering av jnlp-filer, jar-ressurser, signering og enkel war distribuering.
 *
 * @since 1.2
 * @author Tor Egil R. Strand
 */
class WebstartPlugin implements Plugin<Project> {
    protected static Logger logger = Logging.getLogger(WebstartPlugin.class)

    public static final String WEBSTART_CONVENTION_NAME = 'webstart';


    @Override
    void apply(Project project) {
        project.plugins.apply(WarPlugin)

        WebstartConvention convention = new WebstartConvention(project)
        project.convention.plugins.put(WEBSTART_CONVENTION_NAME, convention)

        convention.getWebstart().all(new Action<ClientConfiguration>() {
            @Override
            void execute(ClientConfiguration clientConfiguration) {
                JarSigner jarSigner = configureJarSigner(project, clientConfiguration)
                WebstartTask genJnlp = configureGenJnlp(project, clientConfiguration, jarSigner)
                configureWar(project, clientConfiguration, jarSigner, genJnlp)
            }
        });

    }


    private static JarSigner configureJarSigner(Project project, ClientConfiguration clientConfiguration) {
        HashMap<String, Object> args = new HashMap<String, Object>();
        args.put(Task.TASK_TYPE, JarSigner.class);
        args.put(Task.TASK_OVERWRITE, "false");

        final JarSigner jarSigner = (JarSigner) project.task(args, makeTaskName('sign', clientConfiguration.name, null));

        jarSigner.jarFilesToSign = clientConfiguration.jarDependencies
        jarSigner.conventionMapping.certificateFile = { clientConfiguration?.signingConfiguration?.keystore }
        jarSigner.conventionMapping.alias = { clientConfiguration?.signingConfiguration?.alias }
        jarSigner.conventionMapping.password = { clientConfiguration?.signingConfiguration?.password }
        jarSigner.conventionMapping.digestAlgorithm = { clientConfiguration?.signingConfiguration?.digestAlgorithm }
        jarSigner.conventionMapping.manifestAttributes = { clientConfiguration.manifestAttributes }

        return jarSigner
    }

    private static WebstartTask configureGenJnlp(Project project, ClientConfiguration clientConfiguration, JarSigner jarSigner) {
        HashMap<String, Object> args = new HashMap<String, Object>();
        args.put(Task.TASK_TYPE, WebstartTask.class);
        args.put(Task.TASK_OVERWRITE, "false");

        final WebstartTask webstartTask = (WebstartTask) project.task(args, makeTaskName('gen', clientConfiguration.name, 'Jnlp'));

        webstartTask.jarResources(new Callable<Object>() {
            @Override
            Object call() throws Exception {
                if (jarSigner.getCertificateFile() != null) { //dersom signering
                    return jarSigner.getJarFiles();
                } else {
                    return clientConfiguration.getJarDependencies();
                }
            }
        });

        webstartTask.setJnlpConfigurations(clientConfiguration.jnlpConfigurations)

        //late bindings since configuration is applied later...
        webstartTask.conventionMapping.libDir = { clientConfiguration.libDir }
        webstartTask.conventionMapping.map('mainJar', new Callable<Object>() {
            @Override
            Object call() throws Exception {
                FileCollection jarFiles = webstartTask.getJarResources();
                return jarFiles.filter(clientConfiguration.mainJarFilter)
            }
        })
        webstartTask.conventionMapping.map('digest', new Callable<String>() {
            @Override
            String call() throws Exception {
                return clientConfiguration.signingConfiguration?.createDigest()
            }
        })
        return webstartTask
    }

    private static void configureWar(Project project, ClientConfiguration clientConfiguration, JarSigner jarSigner, WebstartTask webstartTask) {
        War war = project.tasks.getByName(WarPlugin.WAR_TASK_NAME) as War

        war.from { clientConfiguration.warJnlps ? webstartTask : [] }
        war.with(jnlpCopySpec(project, { clientConfiguration.libDir }, jarSigner, { clientConfiguration.signingConfiguration?.createDigest() }))
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
            duplicatesStrategy 'exclude'

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
