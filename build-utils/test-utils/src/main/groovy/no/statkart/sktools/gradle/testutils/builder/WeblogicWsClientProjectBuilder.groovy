package no.statkart.sktools.gradle.testutils.builder

/**
 * Convenience metoder for enkelt oppsett av Project instanser
 *
 * @author Leif Lislegård
 */
class WeblogicWsClientProjectBuilder<T extends WeblogicWsClientProjectBuilder> extends GradleProjectBuilder<T> {


    private boolean setWeblogicClasspath
    private boolean addToolsJar


    public static WeblogicWsClientProjectBuilder<WeblogicWsClientProjectBuilder> builder() {
        return new WeblogicWsClientProjectBuilder();
    }


    public T withWeblogicClasspath() {
        setWeblogicClasspath = true
        addToolsJar = true     //SKIF-191: weblogic 10.3.5 eller nyerer avhenger av tools.jar på classpath
        closures.add {
            projectHelper.defineWEBLOGIC_HOME()
        }
        return this
    }


    public T applyWsClientPlugin(boolean weblogicClasspath) {
        if (weblogicClasspath) {
            withWeblogicClasspath()
        }
        return applyWsClientPlugin()
    }

    public T applyWsClientPlugin() {
        closures.add {
            apply plugin: 'sktools-weblogic-wsclient-plugin'
        }
        if (setWeblogicClasspath) {
            closures.add {
                //set dependency
                dependencies.weblogic projectHelper.weblogicClasspath
            }
        }
        if (addToolsJar) {
            closures.add {
                //sets dependency
                dependencies.weblogic projectHelper.toolsJar
            }
        }
        return this
    }


}
