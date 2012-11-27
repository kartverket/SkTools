package no.statkart.sktools.gradle.testutils.builder

/**
 *
 * @author Leif Lislegård
 */
class WsDocGenProjectBuilder<T extends WsDocGenProjectBuilder> extends GradleProjectBuilder<T> {


    public static WsDocGenProjectBuilder<? extends WsDocGenProjectBuilder> builder() {
        return new WsDocGenProjectBuilder();
    }


    public T applyWsDocGenPlugin() {
        closures.add {
            apply plugin: 'sktools-wsdocgen-plugin'
        }
        return this
    }


}
