package no.statkart.sktools.gradle.plugins.ideaextensions


/**
 * @since 1.3
 * @author Leif Lislegård
 */
class InspectionProfileTestContext extends IdeaTestContext<InspectionProfileTestContext> {

    static String INSPECTION_PROFILE_1_NAME = 'Test profile 1'
    static String INSPECTION_PROFILE_1_LOCAL = 'false'
    static String INSPECTION_PROFILE_1_XML = buildInspectionProfile(INSPECTION_PROFILE_1_NAME, INSPECTION_PROFILE_1_LOCAL)
    static String INSPECTION_PROFILE_2_XML = buildInspectionProfile('Test profile 2', 'false')

    static String IDEA_TEMPLATE_WITH_INSPECTIONS_XML = buildInspectionSettings(INSPECTION_PROFILE_1_XML)


    InspectionProfileTestContext addInspectionProfileFile(String xml) {
        File file = File.createTempFile('inspection_profile', '.xml')
        file.deleteOnExit()
        file.withPrintWriter("UTF-8") {
            it.println xml
        }
        extension.inspectionProfiles += file
        return this
    }


    protected static String buildInspectionProfile(def name, def local) {
        """
          <profile version="1.0" is_locked="false">
            <option name="myName" value="${name}" />
            <option name="myLocal" value="${local}" />
          </profile>
        """.trim()
    }


    static String buildInspectionSettings(String... profiles) {
        buildIdeaIprTemplate("""
          <component name="InspectionProjectProfileManager">
            <profiles>
            ${profiles.join('\n\n')}
            </profiles>
            <option name="PROJECT_PROFILE" value="Test profile 1" />
            <option name="USE_PROJECT_PROFILE" value="true" />
            <version value="1.0" />
          </component>
        """)
    }



}
