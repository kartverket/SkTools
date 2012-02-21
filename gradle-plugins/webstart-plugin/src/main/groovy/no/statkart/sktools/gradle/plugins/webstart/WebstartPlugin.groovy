package no.statkart.sktools.gradle.plugins.webstart

import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import no.statkart.sktools.gradle.plugins.webstart.util.DependencyUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin

import org.gradle.api.logging.LogLevel
import no.statkart.sktools.gradle.plugins.webstart.util.FileUtil

/**
 * todo: dokumentasjon
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 */
class WebstartPlugin implements Plugin<Project> {

    final static CONVENTION_NAME = 'webstart'
    final static CONFIGURATION_NAME = 'webstart'

    @Override
    void apply(Project project) {
        WebstartPluginConvention convention = new WebstartPluginConvention()
        project.convention.plugins.put(CONVENTION_NAME, convention)
        project.plugins.apply(JavaPlugin.class);
        Configuration moduleConfiguration = project.configurations.add(CONFIGURATION_NAME).setDescription("Classpath for jars to be included in webstart application");
        project.task('webstart') {
            project.afterEvaluate {
                project.configurations.webstart.each { inputs.file it }
                if (it.state.failure == null) {
                    String dir = convention.targetDir
                    new File(dir).mkdirs()
                    outputs.dir(dir)
                }
            }
            doLast {
                def files = project.configurations.webstart
                if (convention.includeSelfJar) {
                    files = files.files + project.jar.archivePath
                }
                signAllJars(project, new File(convention.targetDir, 'lib'), files)
                createJnlp(new File(convention.targetDir, convention.jnlpFileName), convention.jnlpTranslation, files)
                createVersionXml(new File(convention.targetDir, 'lib/version.xml'), files)
            }
        }.dependsOn(moduleConfiguration).dependsOn(JavaPlugin.JAR_TASK_NAME)
    }

    private def signAllJars(Project project, File signedJarsOutputDir, def files) {
        File keystoreFile = FileUtil.copyOutResourceTemporary(getClass(), 'kodesignering.jks')
        String keystorePassword = 'SagZ45_p1'
        String alias = 'statenskartverk'

        files.each { unsignedJarFileName ->
            File signedJarFileName = new File(signedJarsOutputDir, unsignedJarFileName.name)
            project.logger.info("Signing ${signedJarFileName}...")
            project.ant.copy(file: unsignedJarFileName, tofile: signedJarFileName)
            project.ant.exec(executable: 'jarsigner', failonerror: true) {
                if (project.logger.isEnabled(LogLevel.INFO)) {
                    arg(value: '-verbose')
                }
                arg(value: '-keystore')
                arg(value: keystoreFile)
                arg(value: '-storepass')
                arg(value: keystorePassword)
                arg(value: signedJarFileName)
                arg(value: alias)
            }
        }
    }


    private def createJnlp(File jnlpFile, Closure translation, def files) {
        jnlpFile.parentFile.mkdirs()
        GPathResult xml = new XmlSlurper().parse(getClass().getResourceAsStream('template.jnlp'))
        xml.resources.appendNode {
            files.each { dep ->
                jar(href: "lib/$dep.name", version: DependencyUtil.getArtifactVersion(dep))
            }
        }
        // Reslurping to see changes
        xml = new XmlSlurper().parseText(writeXmlToString(xml))
        translation(xml)
        writeXml(jnlpFile, xml)
    }

    private String writeXmlToString(GPathResult xml) {
        StringWriter writer = new StringWriter()
        writeXml(writer, xml)
        writer.toString()
    }

    private def writeXml(File jnlpFile, GPathResult xml) {
        def writer = new OutputStreamWriter(new FileOutputStream(jnlpFile), 'UTF-8')
        writeXml(writer, xml)
        writer.close()
    }

    private def writeXml(Writer writer, GPathResult xml) {
        XmlUtil.serialize(new StreamingMarkupBuilder().bind {
            mkp.yield xml
        }, writer)
    }

    private def createVersionXml(File versionXmlFile, def files) {
        versionXmlFile.parentFile.mkdirs()
        FileWriter writer = new FileWriter(versionXmlFile)
        def xml = new MarkupBuilder(writer)
        xml.'jnlp-versions' {
            files.each { dep ->
                resource {
                    pattern {
                        name(dep.name)
                        'version-id'(DependencyUtil.getArtifactVersion(dep))
                    }
                    xml.file(dep.name)
                }
            }
        }
        writer.close()
    }


}


