package no.statkart.sktools.gradle.testutils.builder

/**
 * Convenience metoder for enkelt oppsett av Project instanser
 *
 * @author Leif Lislegård
 */
class WeblogicWsWarProjectBuilder<T extends WeblogicWsWarProjectBuilder> extends GradleProjectBuilder<T> {


    private boolean setWeblogicClasspath
    private boolean addToolsJar


    public static WeblogicWsWarProjectBuilder<WeblogicWsWarProjectBuilder> builder() {
        return new WeblogicWsWarProjectBuilder();
    }


    public T withWeblogicClasspath() {
        setWeblogicClasspath = true
        addToolsJar = true
        closures.add {
            projectHelper.defineWEBLOGIC_HOME()
        }
        return this
    }


    public T applyWsWarPlugin(boolean weblogicClasspath) {
        if (weblogicClasspath) {
            withWeblogicClasspath()
        }
        return applyWsWarPlugin()
    }

    public T applyWsWarPlugin() {
        closures.add {
            apply plugin: 'sktools-weblogic-wswar-plugin'
        }
        if (setWeblogicClasspath) {
            closures.add {
                //set dependency
                dependencies.weblogicProvided projectHelper.weblogicClasspath
            }
        }
        if (addToolsJar) {
            closures.add {
                //set dependency
                dependencies.weblogicCompile files("${System.getenv('JAVA_HOME')}/lib/tools.jar").stopExecutionIfEmpty() //JAVA_HOME ikke satt??
            }
        }
        return this
    }


}
