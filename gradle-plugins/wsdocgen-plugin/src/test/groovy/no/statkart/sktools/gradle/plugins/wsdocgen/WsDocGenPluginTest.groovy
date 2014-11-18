package no.statkart.sktools.gradle.plugins.wsdocgen

import org.testng.annotations.Test
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import no.statkart.sktools.gradle.testutils.builder.WsDocGenProjectBuilder
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.filewriter.WsDocgenTestutilFilewriter
import org.gradle.api.plugins.JavaPluginConvention

/**
 * Test av {@link WsDocGenPlugin}
 *
 * <p>
 *     For testing av generering av dokumentasjon se {@link no.statkart.sktools.utils.wsdocgen.processor.WSDocProcessorTest}
 * </p>
 *
 * @author Leif Lislegård
 */
class WsDocGenPluginTest {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testAppplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-wsdocgen-plugin'


        assert project.convention.plugins.wsdoc != null
        Assert.assertTrue(project.convention.plugins.wsdoc instanceof WsDocGenConvention)

    }


    /**
     * Tester minimalt oppsett, kun defaultverdier
     */
    @Test
    void testDefaultSetting() {
        //forks a new java project in a temp folder
        ProjectHelper projectHelper = WsDocGenProjectBuilder.builder().applyJavaPlugin().applyWsDocGenPlugin().build()


        //generates a simple source file
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeSimpleDemoServiceWSBean("src/main/java")
        }

        projectHelper.initializeProject()

        projectHelper.executeTask(WsDocGenPlugin.GEN_TASK_NAME)

        projectHelper.assertFileExistsInBuildDir('main/docs/wsdoc/TestService.html')

    }

    /**
     * Tester verdier for default konfigurasjon
     */
    @Test
    void testDefaultConfiguration() {
        //forks a new java project in a temp folder
        ProjectHelper projectHelper = WsDocGenProjectBuilder.builder().applyJavaPlugin().applyWsDocGenPlugin().build()

        projectHelper.initializeProject()

        Project project = projectHelper.project
        WsDocGenConvention convention = project.getConvention().getPlugins().get(WsDocGenPlugin.CONVENTION_NAME) as WsDocGenConvention;

        assert convention != null

        assert convention.sourceSetName == 'main'

        assert project.tasks.findByName('genWsDoc')
        assert project.tasks.findByName('genMainWSDoc')
        assert project.tasks.findByName('genWsDoc').dependsOn.contains('genMainWSDoc')

        assert convention.groups.size() == 1
        assert convention.groups[0].includes == ['**/*Bean.java']
        assert convention.groups[0].targetDir == project.file('build/main/docs/wsdoc')

    }



    /**
     * Tester annet sourceSet
     */
    @Test
    void testCustomSourceSet() {
        //forks a new java project in a temp folder
        ProjectHelper projectHelper = WsDocGenProjectBuilder.builder().applyWsDocGenPlugin().build()
        Project project = projectHelper.project

        JavaPluginConvention javaConvention = project.getConvention().getPlugins().get("java");
        javaConvention.getSourceSets().create('custom')


        projectHelper.initializeProject()

        WsDocGenConvention convention = project.getConvention().getPlugins().get(WsDocGenPlugin.CONVENTION_NAME) as WsDocGenConvention;

        assert convention.sourceSetName == 'custom'
    }



    /**
     * Tester syntaks for konfigurasjon
     */
    @Test
    void testConventionConfiguration() {

        //forks a new java project in a temp folder
        //ps: notice that the java plugin is applied after the plugin, at a  later stage.
        ProjectHelper projectHelper = WsDocGenProjectBuilder.builder().applyWsDocGenPlugin().applyJavaPlugin().build()


        //generer eksempel-kildekode som har domene-klasse definert
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeInterfaceServiceWSBean('src/main/java')
        }


        projectHelper.configureProject {
          wsDoc {
              docGroup {
                  targetPath 'build/mydocs'
                  lookupPath '../../some/wacky/place'
                  include '**/*WSBean.java'
              }
            }
        }
        projectHelper.initializeProject()

        projectHelper.executeTask(WsDocGenPlugin.GEN_TASK_NAME)

        projectHelper.assertFileExists('build/mydocs/InterfaceService.html') { File file ->
            assert file.text.contains("../../some/wacky/place") //skal ha link som peker til domeneklasse (javadoc)
        }

    }


    /**
     * Demonstrerer hvordan en kan spre kilekode over flere mapper
     */
    @Test
    void testMultipleSourceFolders() {

        //forks a new java project in a temp folder
        //ps: notice that the java plugin is applied after the plugin, at a  later stage.
        ProjectHelper projectHelper = WsDocGenProjectBuilder.builder().applyWsDocGenPlugin().applyJavaPlugin().build()


        //generer eksempel-kildekode som har domene-klasse definert
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeSimpleDemoServiceWSBean('src/main/java')
            projectHelper.writeInterfaceServiceWSBean('src/main/morejava')
        }


        projectHelper.configureProject {
            sourceSets.main.java.srcDir 'src/main/morejava'
            wsDoc {
                docGroup {
                    targetPath 'build'
                }
            }
        }
        projectHelper.initializeProject()


        projectHelper.executeTask(WsDocGenPlugin.GEN_TASK_NAME)

        projectHelper.assertFileExists('build/InterfaceService.html')
        projectHelper.assertFileExists('build/TestService.html')

    }


}