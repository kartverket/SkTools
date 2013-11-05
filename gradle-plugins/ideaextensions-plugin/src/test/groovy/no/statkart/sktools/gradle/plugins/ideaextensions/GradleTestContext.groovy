package no.statkart.sktools.gradle.plugins.ideaextensions

/**
 * @since 1.3
 * @author Leif Lislegård
 */
class GradleTestContext extends IdeaTestContext<GradleTestContext> {

    static String GRADLE_SETTINGS_1_GRADLE_HOME = "X:\\testcase\\gradle\\gradle-1.6"
    static String GRADLE_SETTINGS_1_XML = buildGradleSettings(GRADLE_SETTINGS_1_GRADLE_HOME)

    static String IDEA_TEMPLATE_WITH_GRADLE_XML = buildIdeaTemplate(GRADLE_SETTINGS_1_XML)


    protected static String buildGradleSettings(def gradleHome) {
      """
        <component name="GradleSettings">
            <option name="gradleHome" value="${gradleHome}" />
        </component>
        """.trim()
    }



}
