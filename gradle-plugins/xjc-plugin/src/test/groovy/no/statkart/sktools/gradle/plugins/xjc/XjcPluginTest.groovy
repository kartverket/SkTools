package no.statkart.sktools.gradle.plugins.xjc

import org.testng.annotations.Test
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.XjcProjectBuilder
import no.statkart.sktools.gradle.testutils.filewriter.XjcTestutilFilewriter
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

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

    }

    /**
     * Tester minimal konfigurasjon - uten ekstra funksjonalitet innkoblet
     *
     * Merk at her er ingen artifakter deklarert. JavaPlugin er heller ikke aktivert.
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
            sourceSets {
                main.xjc {
                    schema {
                        srcDir 'src/main/xsd'
                    }
                }
            }
        }


        projectHelper.initializeProject()

        //executes the gen task
        projectHelper.executeTask('compileJava')

        //asserts the results
        projectHelper.project.sourceSets.main.xjc[0].with { XjcSourceDirectorySet schema ->
            projectHelper.assertTaskExecutedNotSkipped(schema.config.genTaskName)
            projectHelper.assertTaskExecutedNotSkipped(schema.config.compileTaskName)
            projectHelper.assertFileExists("${schema.config.genOutputPath}/no/statkart/sktools/test/SimpleType.java")
        }

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
            sourceSets {
                main.xjc {
                    schema {
                        srcDir 'src/main/xsd'
                        config {
                            withGrunnbokDoc
                        }
                    }
                }
            }
        }


        projectHelper.initializeProject()

        projectHelper.project.sourceSets.main.xjc[0].with { XjcSourceDirectorySet schema ->
            //executes the gen task
            projectHelper.executeTask(schema.config.genTaskName)

            //asserts the results
            projectHelper.assertTaskExecutedNotSkipped(schema.config.genTaskName)
            projectHelper.assertFileExists("${schema.config.genOutputPath}/no/statkart/sktools/test/DocumentedSimpleType.java") { File file ->
                assert file.text.contains("Ekstra dokumentasjon for typen.")
            }
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

            projectHelper.project.mkdir("src/adaper/java/some_adapter")
            projectHelper.project.file("src/adaper/java/some_adapter/Fqn.java").createNewFile()
            projectHelper.project.file("src/adaper/java/some_adapter/Fqn.java") << "package some_adapter;\n public class Fqn { }"
        }


        //config
        projectHelper.configureProject {
            sourceSets {
                main.xjc {
                    schema {
                        srcDir 'src/main/xsd'
                        config {
                            withListAdapter 'some_adapter.Fqn'
                            java.srcDir "src/adaper/java"
                        }
                    }
                }
            }
        }


        projectHelper.initializeProject()

        //executes builds the main source
        projectHelper.executeTask('classes')

        //asserts the results
        projectHelper.project.sourceSets.main.xjc[0].with { XjcSourceDirectorySet schema ->
            projectHelper.assertTaskExecutedNotSkipped(schema.config.genTaskName)

            projectHelper.assertFileExists(schema.config.genOutputPath)
            projectHelper.assertFileExists("${schema.config.genOutputPath}/no/statkart/sktools/test/StringList.java") { File file ->
                assert file.text.contains('import some_adapter.Fqn;')
                assert file.text.contains('extends Fqn')

            }
        }

    }


    /**
     * Tester og demonstrer oppsett av konfigurasjon
     */
    @Test
    void testConventionConfiguration2() {

        //forks a new project in a temp folder
        ProjectHelper projectHelper = XjcProjectBuilder.builder().applyXjcPlugin().build()

        //config
        projectHelper.configureProject {

            sourceSets {
                main {
                    xjc {
                        schema {
                            srcDir 'src/main/xsd'
                            config {
                                withGrunnbokDoc
                            }
                        }
                        schema {
                            srcDir 'src/main/xsd'
                            config {
                                withListAdapter
                            }
                        }
                    }
                }

                other.xjc {
                    schema {
                        srcDir 'src/main/xsd'
                        config {
                            withGrunnbokDoc
                            withListAdapter 'someAdapter.fqn'
                        }
                    }
                }
            }

        }

        projectHelper.initializeProject()

        Project project = projectHelper.project

        SourceSetContainer sourceSets = (SourceSetContainer) project.getConvention().getPlugins().get('java').sourceSets;
        assert sourceSets != null //foventer at javaBase plugin er aktivert


        //tester at source set er utvidet med plugin konfigurasjon

        assert sourceSets.main.xjc.schemas[0] //foventer schema konfigurasjon
        assert sourceSets.main.xjc.schemas[1] //foventer schema konfigurasjon


        //tester targetDir
        sourceSets.main.xjc.each { XjcSourceDirectorySet schema ->
            def compileTask = project.tasks[schema.config.compileTaskName]
            assert sourceSets.main.output.dirs.contains(compileTask.destinationDir)
        }


        //tester schema.withGrunnbokDoc
        assert sourceSets.main.xjc.schemas[0].config.xjcOptions.containsKey(XjcConfig.GRUNNBOK_DOC)
        assert sourceSets.other.xjc.schemas[0].config.xjcOptions.containsKey(XjcConfig.LIST_ADAPTER)

        //tester schema.withListAdapter
        assert sourceSets.main.xjc.schemas[1].config.xjcOptions.containsKey(XjcConfig.LIST_ADAPTER)
        assert sourceSets.other.xjc.schemas[0].config.xjcOptions[XjcConfig.LIST_ADAPTER] == [baseClass:'someAdapter.fqn']


    }




    /**
     * SKTOOLS-22
     * Regresjonsstester feil funnet i MAT-9900 der ideaModule task feiler pga feil oppsett av {@link SourceSet }
     */
    @Test
    void ideaTasksCanHandleSourceSetConfiguration() {

        //forks a new project in a temp folder
        ProjectHelper projectHelper = XjcProjectBuilder.builder().applyXjcPlugin().build()

        //config
        projectHelper.configureProject {
            apply plugin: 'idea'

            sourceSets {
                main {
                    xjc {
                        schema {
                            srcDir 'src/main/xsd'
                        }
                    }
                }
            }
        }

        projectHelper.initializeProject()

        final Project project = projectHelper.project
        final SourceSet sourceSet = project.convention.plugins.java.sourceSets.main
        assert sourceSet.allSource.srcDirs.contains(project.file('src/main/xsd'))
    }



    @Test
    void canSpecifyTaskNameForGen() {

        //forks a new project in a temp folder
        ProjectHelper projectHelper = XjcProjectBuilder.builder().applyXjcPlugin().build()

        //config
        projectHelper.configureProject {
            sourceSets {
                main.xjc {
                    schema {
                        config.genTaskName = 'genCustom'
                    }
                }
            }
        }

        projectHelper.initializeProject()

        assert projectHelper.project.convention.plugins.java.sourceSets.main.xjc[0].config.genTaskName == 'genCustom'
        assert projectHelper.project.tasks.findByPath('genCustom')
    }


    @Test
    void canSpecifyTaskNameForCompile() {

        //forks a new project in a temp folder
        ProjectHelper projectHelper = XjcProjectBuilder.builder().applyXjcPlugin().build()

        //config
        projectHelper.configureProject {
            sourceSets {
                main.xjc {
                    schema {
                        config.compileTaskName = 'compileCustom'
                    }
                }
            }
        }

        projectHelper.initializeProject()

        assert projectHelper.project.convention.plugins.java.sourceSets.main.xjc[0].config.compileTaskName == 'compileCustom'
        assert projectHelper.project.tasks.findByPath('compileCustom')
    }


    @Test
    void canSpecifyGenOutputPath() {

        //forks a new project in a temp folder
        ProjectHelper projectHelper = XjcProjectBuilder.builder().applyXjcPlugin().build()

        //config
        projectHelper.configureProject {
            sourceSets {
                main.xjc {
                    schema {
                        config.genTaskName = 'genCustom'
                        config.genOutputPath = 'generated/custom'
                    }
                }
            }
        }

        projectHelper.initializeProject()

        //preconditions
        assert projectHelper.project.convention.plugins.java.sourceSets.main.xjc[0].config.genTaskName == 'genCustom'
        assert projectHelper.project.tasks.findByPath('genCustom')

        //tests
        assert projectHelper.project.convention.plugins.java.sourceSets.main.xjc[0].config.genOutputPath == 'generated/custom'
        assert projectHelper.project.tasks.findByPath('genCustom').outputDirectory == projectHelper.project.file('generated/custom')

    }


}