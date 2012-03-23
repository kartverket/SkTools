package no.statkart.sktools.gradle.plugins.xjc

import org.testng.annotations.Test
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.XjcProjectBuilder
import no.statkart.sktools.gradle.testutils.filewriter.XjcTestutilFilewriter

/**
 * Test av {@link XjcPlugin}
 *
 * @author Leif Lislegård
 */
class XjcPluginTest {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testAppplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-xjc-plugin'


        assert project.convention.plugins.xjc != null
        Assert.assertTrue(project.convention.plugins.xjc instanceof XjcConvention)

    }

    /**
     * Tester minimal konfigurasjon - uten ekstra funksjonalitet innkoblet
     */
    @Test
    void testDefaultSetting() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = XjcProjectBuilder.builder().applyXjcPlugin().build()


        //generates a simple source file
        use(XjcTestutilFilewriter) {
            projectHelper.writeSimpleSchema("src/main/xsd/simple.xsd")
        }


        //config
        projectHelper.configureProject {
            xjc {
                schema {
                    path 'src/main/xsd'
                }
            }
        }
        projectHelper.initializeProject()


        //executes the gen task
        projectHelper.executeTask(XjcPlugin.XJC_TASK_NAME)

        //asserts the results
        projectHelper.assertTaskExecutedNotSkipped(XjcPlugin.XJC_TASK_NAME)
        projectHelper.assertFileExists("gen/main/java/no/statkart/sktools/test/SimpleType.java")

    }


    /**
     * Tester innkobling av gdoc
     */
    @Test
    void testGrunnbokDoc() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = XjcProjectBuilder.builder().applyXjcPlugin().build()


        //generates a simple source file with gdoc annotations
        use(XjcTestutilFilewriter) {
            projectHelper.writeSimpleSchemaWithGdoc("src/main/xsd/simple.xsd")
        }


        //config
        projectHelper.configureProject {
            xjc {
                schema {
                    path 'src/main/xsd'
                    withGrunnbokDoc
                }
            }
        }
        projectHelper.initializeProject()


        //executes the gen task
        projectHelper.executeTask(XjcPlugin.XJC_TASK_NAME)

        //asserts the results
        projectHelper.assertTaskExecutedNotSkipped(XjcPlugin.XJC_TASK_NAME)
        projectHelper.assertFileExists("gen/main/java/no/statkart/sktools/test/DocumentedSimpleType.java") { File file ->
            assert file.text.contains("Ekstra dokumentasjon for typen.")
        }

    }

    /**
     * Tester innkobling av listAdapter
     */
    @Test
    void testListAdapter() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = XjcProjectBuilder.builder().applyXjcPlugin().build()

        //generates a simple source file
        use(XjcTestutilFilewriter) {
            projectHelper.writeSimpleSchema("src/main/xsd/simple.xsd")
        }


        //config
        projectHelper.configureProject {
            xjc {
                targetDir 'build'
                schema {
                    path 'src/main/xsd'
                    withListAdapter 'someAdapter.Fqn'
                }
            }
        }
        projectHelper.initializeProject()


        //executes the gen task
        projectHelper.executeTask(XjcPlugin.XJC_TASK_NAME)

        //asserts the results
        projectHelper.assertTaskExecutedNotSkipped(XjcPlugin.XJC_TASK_NAME)
        projectHelper.assertFileExists("build/no/statkart/sktools/test/StringList.java") { File file ->

            assert file.text.contains('import someAdapter.Fqn;')
            assert file.text.contains('extends Fqn')

        }

    }



    /**
     * Tester og demonstrer oppsett av konfigurasjon
     */
    @Test
    void testConventionConfiguration() {

        //forks a new project in a temp folder
        ProjectHelper projectHelper = XjcProjectBuilder.builder().applyXjcPlugin().build()

        //config
        projectHelper.configureProject {
            xjc {

                sourceSetName 'mysource'

                schema {
                    path 'src/main/xsd'
                    withGrunnbokDoc
                }
                schema {
                    path project.file('src/main/xsd')
                    includes '**/*.xsd'
                    withListAdapter
                }
                schema {
                    path "${project.buildDir}/../src/main/xsd"
                    includes '**/*.xsd'
                    includes '**/*.xml'

                    withGrunnbokDoc
                    withListAdapter 'someAdapter.fqn'
                }
            }
        }

        projectHelper.initializeProject()

        Project project = projectHelper.project
        XjcConvention convention = project.getConvention().getPlugins().get(XjcPlugin.CONVENTION_NAME);

        assert convention != null


        //tester targetDir
        assert convention.targetDir == project.file('gen/mysource/java')
        convention.targetDir('src/gen/myjava')
        assert convention.targetDir == project.file('src/gen/myjava')



        //tester schema.dir
        File expectedPath = project.file('src/main/xsd')
        (0..2).each {
            assert convention.schema[it].dir == expectedPath;
        }


        //tester schema.includes (default verdi er '**/*.xsd')
        (0..2).each {
            assert convention.schema[it].includes.contains('**/*.xsd');
        }
        assert convention.schema[2].includes.contains('**/*.xml');


        //tester schema.withGrunnbokDoc
        assert convention.schema[0].xjcOptions.containsKey(Schema.GRUNNBOK_DOC)
        assert convention.schema[2].xjcOptions.containsKey(Schema.GRUNNBOK_DOC)

        //tester schema.withListAdapter
        assert convention.schema[1].xjcOptions.containsKey(Schema.LIST_ADAPTER)
        assert convention.schema[2].xjcOptions.containsKey(Schema.LIST_ADAPTER)
        assert convention.schema[2].xjcOptions[Schema.LIST_ADAPTER] == [baseClass:'someAdapter.fqn']


    }





}