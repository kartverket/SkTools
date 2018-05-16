package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.process.JavaExecSpec

/**
 * Task for deploy
 *
 * @since 1.2
 * @author Leif Lislegård
 */
class WeblogicDeployTask extends AbstractWeblogicDeployTask {
    protected static final Logger logger = Logging.getLogger(WeblogicDeployTask.class)

    @InputFiles
    Object file

    @Input
    boolean upload = true

    @Input
    boolean library = false

    @Override
    protected void buildCommandLine(JavaExecSpec execSpec) {
        logger.quiet("Deployment av ${getDeploymentName()} til Weblogic paa ${getUrl()}")

        checkSourceExists()

        execSpec.args('-deploy')

        if (isUpload()) {
            execSpec.args('-upload')
        }

        execSpec.args('-source', project.files(getFile()).singleFile)

        if (isLibrary()) {
            execSpec.args('-library')
        }
    }

    public Logger getLogger() {
        return logger
    }

    void checkSourceExists() {
        FileCollection fileCollection = project.files(getFile())
        if (fileCollection.isEmpty()) {
            logger.error ''
            logger.error 'ERROR: Ingen fil aa deploye. \nHar du konfigurert opp denne tasken riktigt?'
            logger.error ''
            throw new RuntimeException("Source for task $name er ikke konfigurert!")
        }
        if (!fileCollection.singleFile.exists()) {
            logger.error ''
            logger.error 'ERROR: Ingen fil aa deploye. \nHar du husket assemble?'
            logger.error ''
            throw new FileNotFoundException(String.valueOf(getFile()))
        }
    }
}