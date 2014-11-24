package no.statkart.sktools.gradle.plugins.weblogic.compile;

import org.gradle.api.tasks.WorkResult;

public interface Compiler<T extends WeblogicCompileSpec> {
    WorkResult execute(T spec);
}
