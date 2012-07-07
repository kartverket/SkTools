package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.internal.ConventionTask
import no.statkart.sktools.gradle.plugins.weblogic.WeblogicTaskInterface
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction
import org.gradle.api.AntBuilder

/**
 * Alt som er felles for deploying og undeploying.
 *
 * @author Tor Egil R. Strand
 * @since 1.2
 */
abstract class AbstractWeblogicDeployTask extends ConventionTask implements WeblogicTaskInterface {
    private FileCollection weblogicClasspath;
    private WeblogicDeployConfiguration weblogicServerConfiguration;

    @Override
    FileCollection getWeblogicClasspath() {
        return weblogicClasspath
    }

    @Override
    void setWeblogicClasspath(FileCollection weblogicClasspath) {
        this.weblogicClasspath = weblogicClasspath
    }

    void setWeblogicServerConfiguration(WeblogicDeployConfiguration weblogicServerConfiguration) {
        this.weblogicServerConfiguration = weblogicServerConfiguration
    }

    protected WeblogicDeployConfiguration getWeblogicServerConfiguration() {
        return weblogicServerConfiguration
    }

    @Override
    AntBuilder getAnt() {
        AntBuilder ant = super.getAnt()
        ant.taskdef(name: 'wldeploy', classname: 'weblogic.ant.taskdefs.management.WLDeploy', classpath: getWeblogicClasspath().asPath)
        return ant
    }

}
