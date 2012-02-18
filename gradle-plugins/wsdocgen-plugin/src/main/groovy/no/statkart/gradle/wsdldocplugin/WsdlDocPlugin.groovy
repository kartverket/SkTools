package no.statkart.gradle.wsdldocplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar

class WsdlDocPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        WsdlDocPluginConvention convention = new WsdlDocPluginConvention()
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

class WsdlDocPluginConvention {
    String sourceDir
    String lookupPath
    FileCollection classpath
    String includePattern

    def wsdlDoc(Closure closure) {
        closure.delegate = this
        closure()
    }
}