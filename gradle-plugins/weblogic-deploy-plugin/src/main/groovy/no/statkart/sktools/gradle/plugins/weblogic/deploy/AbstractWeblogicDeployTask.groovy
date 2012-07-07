package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.internal.ConventionTask
import org.gradle.api.file.FileCollection
import org.gradle.api.AntBuilder
import org.gradle.api.tasks.Input

/**
 * Alt som er felles for deploying og undeploying.
 *
 * @author Tor Egil R. Strand
 * @since 1.2
 */
abstract class AbstractWeblogicDeployTask extends ConventionTask {

    @Input
    FileCollection classpath


    @Input
    String deploymentName
    @Input
    String targets

    @Input
    String url
    @Input
    String username
    @Input
    String password


    @Input
    boolean failOnError = false
    @Input
    boolean verbose = true



    AbstractWeblogicDeployTask() {
        super()
        group = 'Deployment'
        outputs.upToDateWhen { false }
    }


    @Override
    AntBuilder getAnt() {
        AntBuilder ant = super.getAnt()
        ant.taskdef(name: 'wldeploy', classname: 'weblogic.ant.taskdefs.management.WLDeploy', classpath: getClasspath().asPath)
        return ant
    }

}
