package no.statkart.sktools.gradle.testutils.builder

/**
 *
 * @author Leif Lislegård
 */
class PropertiesProjectBuilder<T extends PropertiesProjectBuilder> extends GradleProjectBuilder<T> {

    public static PropertiesProjectBuilder<? extends PropertiesProjectBuilder> builder() {
        return new PropertiesProjectBuilder();
    }


    public T applyPropertiesPlugin() {
        closures.add {
            apply plugin: 'sktools-properties-plugin'
        }
        return this
    }

}
