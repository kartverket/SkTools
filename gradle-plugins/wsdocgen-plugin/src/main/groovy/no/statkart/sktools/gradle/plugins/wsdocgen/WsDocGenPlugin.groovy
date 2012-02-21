package no.statkart.sktools.gradle.plugins.wsdocgen

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar

/**
 * todo: dokumentasjon
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 */
class WsDocGenPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        WsDocGenPluginConvention convention = new WsDocGenPluginConvention()
        project.convention.plugins.wsdlDoc = convention
        project.plugins.apply(JavaPlugin.class);
        String configurationName = 'docWsdlDoc'
        File wsdlDocDir = project.file('build/docs/wsdldoc')
        Task generateWsdlDocTask = project.task('generateWsdlDoc') {
            project.afterEvaluate {
                if (it.state.failure == null) {
                    inputs.dir convention.sourceDir
                }
            }
            outputs.dir wsdlDocDir
            doLast {
                wsdlDocDir.mkdirs()
                ant.apt(factory: 'no.statkart.grunnbok.tools.docgen.ws.WebserviceAnnotationProcessorFactory',
                        srcdir: convention.sourceDir,
                        destdir: wsdlDocDir,
                        compile: false,
                        debug: true,
                        classpath: convention.classpath.asPath) {
                    option(name: 'LookupPath', value: convention.lookupPath)
                    include(name: convention.includePattern)
                }
            }
        }
        Task packWsdlDocTask = project.task('packWsdldoc', type: Jar, dependsOn: generateWsdlDocTask) {
            classifier = configurationName
            from wsdlDocDir
        }
        project.configurations.add(configurationName).setTransitive(false).setDescription('Javadoc artifact').addArtifact(new ArchivePublishArtifact(packWsdlDocTask))
    }


}
