package no.statkart.sktools.gradle.testutils.builder

/**
 * Convenience metoder for enkelt oppsett av Project instanser
 *
 * @author Leif Lislegård
 */
class XjcProjectBuilder<T extends XjcProjectBuilder> extends GradleProjectBuilder<T> {

    public static XjcProjectBuilder<XjcProjectBuilder> builder() {
        return new XjcProjectBuilder();
    }


    public T applyXjcPlugin() {
        closures.add {
            apply plugin: 'sktools-xjc-plugin'
        }
        return this
    }

}
