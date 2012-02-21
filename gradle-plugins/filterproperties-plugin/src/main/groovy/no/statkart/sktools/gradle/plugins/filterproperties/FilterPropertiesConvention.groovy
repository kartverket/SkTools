package no.statkart.sktools.gradle.plugins.filterproperties

/**
 * todo: dokumentasjon
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class FilterPropertiesConvention {
    Map<String, String> resources = [main: 'src/main/unfilteredResources', test: 'src/test/unfilteredResources']
    Map<String, String> properties = Collections.emptyMap()

    def filteredProperties(Closure closure) {
        closure.delegate = this
        closure()
    }

    /**
     * @depricated since 1.0 - bruk heller {@link #filteredProperties(Closure)}.
     */
    def statKartFilterProperties(Closure closure) {
        println 'statKartFilterProperties(Closure) is now depricated - use filteredProperties(Closure) instead!'
        return filteredProperties(closure)
    }

}
