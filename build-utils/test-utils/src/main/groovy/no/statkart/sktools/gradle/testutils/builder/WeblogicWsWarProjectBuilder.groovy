package no.statkart.sktools.gradle.testutils.builder

/**
 * Convenience metoder for enkelt oppsett av Project instanser
 *
 * @author Leif Lislegård
 */
class WeblogicWsWarProjectBuilder<T extends WeblogicWsWarProjectBuilder> extends GradleProjectBuilder<T> {


    public static WeblogicWsWarProjectBuilder<? extends WeblogicWsWarProjectBuilder<WeblogicWsWarProjectBuilder>> builder() {
        return new WeblogicWsWarProjectBuilder();
    }


    public T applyWsWarPlugin(boolean weblogicClasspath) {
        return applyWsWarPlugin()
    }

    public T applyWsWarPlugin() {
        closures.add {
            apply plugin: 'sktools-weblogic-wswar-plugin'
        }
        return this
    }


}
