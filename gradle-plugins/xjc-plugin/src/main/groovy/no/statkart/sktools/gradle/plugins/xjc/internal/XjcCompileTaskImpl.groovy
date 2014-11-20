package no.statkart.sktools.gradle.plugins.xjc.internal

import no.statkart.sktools.gradle.plugins.xjc.XjcCompile
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Wrapper-task for kompilering av java kildekode. Se {@link no.statkart.sktools.gradle.plugins.xjc.XjcCompile}
 *
 * @author Leif Lislegård
 * @since 1.2
 */
public class XjcCompileTaskImpl extends DefaultTask implements XjcCompile {
    protected static final Logger logger = Logging.getLogger(XjcCompile.class);

    public XjcCompileTaskImpl() {
    }

    public Logger getLogger() {
        return logger;
    }
}
