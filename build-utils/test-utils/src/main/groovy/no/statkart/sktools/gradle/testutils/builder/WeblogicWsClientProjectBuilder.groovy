package no.statkart.sktools.gradle.testutils.builder

/**
 * Convenience metoder for enkelt oppsett av Project instanser
 *
 * @author Leif Lislegård
 */
class WeblogicWsClientProjectBuilder<T extends WeblogicWsClientProjectBuilder> extends GradleProjectBuilder<T> {


    public static WeblogicWsClientProjectBuilder<? extends WeblogicWsClientProjectBuilder> builder() {
        return new WeblogicWsClientProjectBuilder();
    }


    public T applyWsClientPlugin() {
        closures.add {
            apply plugin: 'sktools-weblogic-wsclient-plugin'
        }
        return this
    }


}
