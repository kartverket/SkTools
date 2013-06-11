package no.statkart.sktools.gradle.plugins.webstart;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

/**
 * Beregner path til jar-filer for deploying av jnlp servlet.
 *
 * Bruk denne dersom for å hente ut jarfiler via {@code getOutputs().getFiles()}
 *
 * @author Leif Lislegård
 */
public class JnlpServletWarTask extends DefaultTask {
    private static Logger logger = Logging.getLogger(JnlpServletWarTask.class);

    public JnlpServletWarTask() {
    }


    @Optional
    @Input
    String getJavaHome() {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null) {
            logger.info("JAVA_HOME not set.. trying java.home");
            javaHome = System.getProperty("java.home") + "/.."; //anntar at jre og jdk ligger sammen på std plassering.
        }
        return javaHome;
    }



    @TaskAction
    public void findJars() {
        String javaHome = getJavaHome();
        getOutputs().file(javaHome + "/sample/jnlp/servlet/jnlp-servlet.jar");
        getOutputs().file(javaHome + "/sample/jnlp/servlet/jardiff.jar");

        if (getOutputs().getFiles().isEmpty()) {
            throw new GradleException("Could not find sample files from jdk. Check JAVA_HOME. Tried " + getOutputs().getFiles().getAsPath());
        }

    }
}
