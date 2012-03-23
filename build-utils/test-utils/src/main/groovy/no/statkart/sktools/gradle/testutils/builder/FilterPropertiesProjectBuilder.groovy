package no.statkart.sktools.gradle.testutils.builder

/**
 *
 * @author Leif Lislegård
 */
class FilterPropertiesProjectBuilder <T extends FilterPropertiesProjectBuilder> extends GradleProjectBuilder<T> {

    public static FilterPropertiesProjectBuilder<FilterPropertiesProjectBuilder> builder() {
        return new FilterPropertiesProjectBuilder();
    }


    public T applyFilterPropertiesPlugin() {
        closures.add {
            apply plugin: 'sktools-filterproperties-plugin'
        }
        return this
    }

}
