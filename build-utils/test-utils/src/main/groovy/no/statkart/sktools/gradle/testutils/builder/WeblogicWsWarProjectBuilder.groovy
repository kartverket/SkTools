package no.statkart.sktools.gradle.testutils.builder

/**
 * Convenience metoder for enkelt oppsett av Project instanser
 *
 * @author Leif Lislegård
 */
class WeblogicWsWarProjectBuilder<T extends WeblogicWsWarProjectBuilder> extends GradleProjectBuilder<T> {


    private boolean setWeblogicClasspath


    public static WeblogicWsWarProjectBuilder<WeblogicWsWarProjectBuilder> builder() {
        return new WeblogicWsWarProjectBuilder();
    }


    public T withWeblogicClasspath() {
        setWeblogicClasspath = true
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
                dependencies.weblogic projectHelper.weblogicClasspath
            }
        }
        return this
    }


}
