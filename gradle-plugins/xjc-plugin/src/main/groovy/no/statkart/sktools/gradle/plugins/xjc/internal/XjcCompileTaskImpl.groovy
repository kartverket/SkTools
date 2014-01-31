package no.statkart.sktools.gradle.plugins.xjc.internal

import org.gradle.api.tasks.compile.JavaCompile
import no.statkart.sktools.gradle.plugins.xjc.XjcCompile

/**
 * Task som kompilerer java kildekoden. Se {@link no.statkart.sktools.gradle.plugins.xjc.XjcCompile}
 *
 * @author Leif Lislegård
 * @since 1.2
 */
public class XjcCompileTaskImpl extends JavaCompile implements XjcCompile {
    public XjcCompileTaskImpl() {
    }
}
