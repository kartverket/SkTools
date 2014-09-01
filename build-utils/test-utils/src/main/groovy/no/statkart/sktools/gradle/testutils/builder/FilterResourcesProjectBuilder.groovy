package no.statkart.sktools.gradle.testutils.builder

/**
 *
 * @author Leif Lislegård
 */
class FilterResourcesProjectBuilder<T extends FilterResourcesProjectBuilder> extends GradleProjectBuilder<T> {

    public static FilterResourcesProjectBuilder<? extends FilterResourcesProjectBuilder> builder() {
        return new FilterResourcesProjectBuilder();
    }


    public T applyFilterResourcesPlugin() {
        closures.add {
            apply plugin: 'sktools-filter-resources-plugin'
        }
        return this
    }

}
