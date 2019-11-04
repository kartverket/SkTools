package no.statkart.sktools.gradle

import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

public class Workaround {
    /**
     * Workaround until Gradle 5 / Groovy 2.5
     */
    public static void init() {
        //adding tap method to all Project instances
        Project.metaClass.tap {
            ConfigureUtil.configure(it, delegate)
            return delegate
        }
    }

    static {
        init();
    }
}
