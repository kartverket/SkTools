package no.statkart.sktools.gradle.plugins.dbtools.database

import org.codehaus.groovy.runtime.MethodClosure
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency

class LoadDriversHelper {

    static Closure loadDriverClosure(final Configuration configuration, final Dependency dependency) {
        return {
            //GroovyCastException: Cannot cast object 'org.gradle.internal.classloader.MutableURLClassLoader
            final ClassLoader groovyClassloader = GroovyObject.class.getClassLoader();
            final MethodClosure addURLClosure = new MethodClosure(groovyClassloader, "addURL");

            final Set<File> files = configuration.files(dependency);
            for (File file : files) {
                //For å kunne benytte jdbc funksjonalitet, må jdbc klasser være lastet inn i classloader til groovy.
                try {
                    addURLClosure.call(file.toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new Error("Implementation error", e);
                }
            }

        };
    }

}
