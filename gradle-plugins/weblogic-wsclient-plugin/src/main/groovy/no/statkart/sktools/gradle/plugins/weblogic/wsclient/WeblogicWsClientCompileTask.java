package no.statkart.sktools.gradle.plugins.weblogic.wsclient;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.compile.JavaCompile;

/**
 * Task for kompilerings-steg.
 *
 * Dette interfacet kan benyttes for å huke inn xjc funksjonalitet slik at:
 *
 * <pre><code>
 task('gen').description = "Genererte ressurser"


 //integrasjon med plugin
 afterEvaluate {
   if (project.plugins.hasPlugin('sktools-weblogic-wsclient-plugin')) {
     project.tasks.withType(no.statkart.sktools.gradle.plugins.weblogic.wsclient.WeblogicWsClientCompileTask.class) {
       project.tasks.gen.dependsOn it
     }
   }
 }

 * </code></pre>
 *
 * }
 *
 *
 * @since 1.2
 * @author Leif Lislegård
 */
public class WeblogicWsClientCompileTask extends JavaCompile {
    protected static final Logger logger = Logging.getLogger(WeblogicWsClientCompileTask.class);

    @Override
    public Logger getLogger() {
        return logger;
    }
}
