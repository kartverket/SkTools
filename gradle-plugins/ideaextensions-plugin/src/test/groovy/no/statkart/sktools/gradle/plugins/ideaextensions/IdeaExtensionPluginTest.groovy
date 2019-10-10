package no.statkart.sktools.gradle.plugins.ideaextensions

import no.statkart.sktools.gradle.testutils.TestKitBase
import org.testng.annotations.Test

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.contentOf

/**
 * @author Leif Lislegård
 */
class IdeaExtensionPluginTest extends TestKitBase {

    /**
     * Tester registrering av plugin via navn
     * og at extension er registrert.
     */
    @Test
    void testApplyPlugin() {
        writeFile("build.gradle", '''
            plugins {
              id 'sktools-ideaextensions-plugin'
            }
            
            assert ideaExtensions instanceof no.statkart.sktools.gradle.plugins.ideaextensions.IdeaExtensionsPluginExtension
        ''')

        assertNoFailures(testGradleBuild("tasks"))
    }


    /**
     * Tester at filer maskeres bort
     * Tester at stier maskeres bort (paths)
     */
    @Test
    void testMasksAndPaths() {
        writeFile("build.gradle", '''
            plugins {
              id 'sktools-ideaextensions-plugin'
            }
            
            ideaExtensions {
                ignoreMasks += '*.tmp'
                ignorePaths += '.gradle/'
            }
        ''')

        writeFile("settings.gradle", '''
            rootProject.name = 'myproject'
        ''')

        testGradleBuild("ideaWorkspace")
        assertThat(contentOf(file('myproject.iws')))
                .contains('mask="*.tmp"')
                .contains('path=".gradle/"');
    }


    @Test
    void testVCS() {
        writeFile("build.gradle", '''
            plugins {
              id 'sktools-ideaextensions-plugin'
            }
            
            ideaExtensions {
                vcs 'Perforce'
            }
        ''')

        writeFile("settings.gradle", '''
            rootProject.name = 'myproject'
        ''')

        testGradleBuild("idea")
        assertThat(contentOf(file('myproject.ipr')))
                .contains('vcs="Perforce"');
    }

    @Test
    void testInspections() {
        writeFile("build.gradle", '''
            plugins {
              id 'sktools-ideaextensions-plugin'
            }
            
            ideaExtensions {
                inspectionProfile = 'inspections.xml'
            }
        ''')

        writeFile("inspections.xml", '''<?xml version="1.0" encoding="UTF-8"?>
            <inspections version="1.0" is_locked="false">
              <option name="myName" value="SKTools Default" />
              <option name="myLocal" value="false" />
            
              <inspection_tool class="CollectionsFieldAccessReplaceableByMethodCall" enabled="true" level="WARNING" enabled_by_default="true" />
            </inspections>
        ''')

        writeFile("settings.gradle", '''
            rootProject.name = 'myproject'
        ''')

        testGradleBuild("idea")
        assertThat(contentOf(file('myproject.ipr')))
                .contains('"SKTools Default"');
    }


    @Test
    void testCodeStyle() {
        writeFile("build.gradle", '''
            plugins {
              id 'sktools-ideaextensions-plugin'
            }
            
            ideaExtensions {
                codeStyle = 'codestyle.xml'
            }
        ''')

        writeFile("codestyle.xml", '''<?xml version="1.0" encoding="UTF-8"?>
            <component name="ProjectCodeStyleSettingsManager">
                <option name="PER_PROJECT_SETTINGS">
                    <value>
                        <XML>
                            <option name="XML_ATTRIBUTE_WRAP" value="0" />
                            <option name="XML_TEXT_WRAP" value="0" />
                            <option name="XML_LEGACY_SETTINGS_IMPORTED" value="true" />
                        </XML>
                    </value>
                </option>
                <option name="USE_PER_PROJECT_SETTINGS" value="true" />
            </component>
        ''')

        writeFile("settings.gradle", '''
            rootProject.name = 'myproject'
        ''')

        testGradleBuild("idea")
        assertThat(contentOf(file('myproject.ipr')))
                .contains('name="XML_ATTRIBUTE_WRAP"');
    }


    @Test
    void testFoldergeneration() {
        writeFile("subproject/build.gradle", '''
            plugins {
              id 'sktools-ideaextensions-plugin'
              id 'groovy'
            }
            
            sourceSets {
                //main sourceSet som blir brukt av Intellij
                main {
                    java.srcDir 'src/main2/java'
                    allJava.srcDir 'src/main2/allJava' // defineres ikke som allSource
                }
            
                //legger til nytt sourceSet
                hiddenSource {
                    java.srcDir 'src/hidden/java2'
                    groovy.srcDir 'src/hidden/groovy2'
                    resources.srcDir 'src/hidden/resources2'
            
                    allGroovy.srcDir 'src/hidden/allGroovy' // defineres ikke som allSource
                    allJava.srcDir 'src/hidden/allJava' // defineres ikke som allSource
                }
            }            
        ''')


        writeFile("settings.gradle", '''
            rootProject.name = 'myproject'
            include ':subproject'
        ''')


        testGradleBuild("idea")

        //tester at iml filen inneholder folders for main og test sourceSet  [SKIF-178]
        assertThat(contentOf(file('subproject/subproject.iml')))
                .describedAs("iml filen inneholder folders for main og test sourceSet")
                .contains(
                        'src/main/java',
                        'src/main2/java',
                        'src/main/groovy',
                        'src/main/resources',

                        'src/test/java',
                        'src/test/groovy',
                        'src/test/resources',
                );

        //tester at mapper blir generert opp ihht sourceSet  [SKIF-178]
        assertThat(file('subproject/src/main/java')).exists();
        assertThat(file('subproject/src/main2/java')).exists();
        assertThat(file('subproject/src/main2/allJava')).doesNotExist();
        assertThat(file('subproject/src/main/resources')).exists();

        assertThat(file('subproject/src/hiddenSource/java')).exists();
        assertThat(file('subproject/src/hidden/java2')).exists();
        assertThat(file('subproject/src/hidden/allJava')).doesNotExist();

        assertThat(file('subproject/src/hiddenSource/groovy')).exists();
        assertThat(file('subproject/src/hidden/groovy2')).exists();
        assertThat(file('subproject/src/hidden/allGroovy')).doesNotExist();

        assertThat(file('subproject/src/hiddenSource/resources')).exists();
        assertThat(file('subproject/src/hidden/resources2')).exists();
    }

}
