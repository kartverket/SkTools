package no.statkart.sktools.gradle.plugins.weblogic.wsclient.internal

import org.gradle.api.tasks.compile.JavaCompile
import no.statkart.sktools.gradle.plugins.weblogic.wsclient.WeblogicWsClientCompileTask

/**
 * Task som kompilerer java kildekoden. Se {@link WeblogicWsClientCompileTask}
 *
 * @author Leif Lislegård
 * @since 1.2
 */
public class WeblogicWsClientCompileTaskImpl extends JavaCompile implements WeblogicWsClientCompileTask {
    public WeblogicWsClientCompileTaskImpl() {
    }
}