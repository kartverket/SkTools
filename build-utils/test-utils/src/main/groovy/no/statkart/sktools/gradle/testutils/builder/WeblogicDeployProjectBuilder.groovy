package no.statkart.sktools.gradle.testutils.builder

/**
 *
 * @author Leif Lislegård
 */
class WeblogicDeployProjectBuilder<T extends WeblogicDeployProjectBuilder> extends GradleProjectBuilder<T> {

    public static WeblogicDeployProjectBuilder<WeblogicDeployProjectBuilder> builder() {
        return new WeblogicDeployProjectBuilder();
    }


    public T applyWeblogicDeployPlugin() {
        closures.add {
            apply plugin: 'sktools-weblogic-deploy-plugin'
        }
        return this
    }

}
