package no.statkart.sktools.gradle.plugins.xjc.internal

import no.statkart.sktools.gradle.plugins.xjc.XjcCompile
import org.gradle.api.DefaultTask

/**
 * Wrapper-task for kompilering av java kildekode. Se {@link no.statkart.sktools.gradle.plugins.xjc.XjcCompile}
 *
 * @author Leif Lislegård
 * @since 1.2
 */
public class XjcCompileTaskImpl extends DefaultTask implements XjcCompile {
    public XjcCompileTaskImpl() {
    }
}
