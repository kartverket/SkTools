package no.statkart.sktools.gradle.testutils.builder

/**
 * Convenience metoder for enkelt oppsett av Project instanser for testing.
 *
 * @author Leif Lislegård
 */
class WebstartProjectBuilder<T extends WebstartProjectBuilder> extends GradleProjectBuilder<T> {

    public static WebstartProjectBuilder<WebstartProjectBuilder> builder() {
        return new WebstartProjectBuilder();
    }


    public T applyWebstartPlugin() {
        closures.add {
            apply plugin: 'sktools-webstart-plugin'
        }
        return this
    }
}
