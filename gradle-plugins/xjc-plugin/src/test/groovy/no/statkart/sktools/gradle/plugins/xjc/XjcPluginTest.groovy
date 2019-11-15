package no.statkart.sktools.gradle.plugins.xjc


import no.statkart.sktools.gradle.testutils.TestKitBase
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.testng.annotations.Test

import static no.statkart.sktools.gradle.testutils.filewriter.XjcTestutilFilewriter.writeSimpleSchema
import static no.statkart.sktools.gradle.testutils.filewriter.XjcTestutilFilewriter.writeSimpleSchemaWithGdoc
import static org.assertj.core.api.Assertions.*

/**
 * Test av {@link XjcPlugin}
 *
 * @author Leif Lislegård
 */
class XjcPluginTest extends TestKitBase {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testApplyPlugin() {
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-xjc-plugin'
        }

        assertThat(project.getPlugins().getPlugin(XjcPlugin.class)).isNotNull()
    }

    /**
     * Tester minimal konfigurasjon - uten ekstra funksjonalitet innkoblet
     *
     * Merk at her er ingen artifakter deklarert. JavaPlugin er heller ikke aktivert.
     */
    @Test
    void testDefaultConfig() {
        //generates a simple source file
        writeSimpleSchema(file("src/main/xsd/simple.xsd"))

        //config
        writeFile("build.gradle", """
            plugins {
              id 'sktools-xjc-plugin'
            }

            repositories {
                maven { url = '${testProperties.MAVEN_REPO}' }
            }

            sourceSets {
                main.xjc {
                    schema {
                        srcDir 'src/main/xsd'
                    }
                }
            }
        """)

        //executes the gen task
        BuildResult buildResult = testGradleBuild("compileJava")
        assertThat(buildResult.getTasks())
                .extracting("path", "outcome")
                .contains(tuple(":genMain0Schema", TaskOutcome.SUCCESS))

        assertThat(file("build/xjc/main/main0Schema/no/statkart/sktools/test/SimpleType.java"))
                .exists()
    }

    /**
     * Tester innkobling av gdoc
     */
    @Test
    void testGrunnbokDoc() {
        //generates a simple source file with gdoc annotations
        writeSimpleSchemaWithGdoc(file("src/main/xsd/simple.xsd"))

        //config
        writeFile("build.gradle", """
            plugins {
              id 'sktools-xjc-plugin'
            }

            repositories {
                maven { url = '${testProperties.MAVEN_REPO}' }
            }

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
        """)

        testGradleBuild("genMain0Schema")

        assertThat(contentOf(file("build/xjc/main/main0Schema/no/statkart/sktools/test/DocumentedSimpleType.java")))
                .contains("Ekstra dokumentasjon for typen.")
    }

    /**
     * Tester innkobling av listAdapter
     */
    @Test
    void testListAdapter() {
        //generates a simple source file
        writeSimpleSchema(file("src/main/xsd/simple.xsd"))

        writeFile("src/adaper/java/some_adapter/Fqn.java",
            "package some_adapter;\n public class Fqn { }")


        writeFile("build.gradle", """
            plugins {
              id 'sktools-xjc-plugin'
            }

            repositories {
                maven { url = '${testProperties.MAVEN_REPO}' }
            }

            sourceSets {
                main.java.srcDir "src/adaper/java"
                main.xjc {
                    schema {
                        srcDir 'src/main/xsd'
                        config {
                            withListAdapter 'some_adapter.Fqn'
                        }
                    }
                }
            }
        """)


        //executes builds the main source
        BuildResult buildResult = testGradleBuild("classes")
        //asserts the results
        assertThat(buildResult.task(':genMain0Schema').getOutcome()).isEqualTo(TaskOutcome.SUCCESS)

        assertThat(contentOf(file("build/xjc/main/main0Schema/no/statkart/sktools/test/StringList.java")))
        .contains(
            'import some_adapter.Fqn;'
            , 'extends Fqn'
        )
    }

    /**
     * Verifiserer at {@link org.gradle.api.file.SourceDirectorySet#srcDir srcDir} kan konfigureres.
     * Regresjonsstester feil funnet i MAT-9900 der ideaModule task feiler pga feil oppsett av {@link SourceSet}
     */
    @Test
    void ideaTasksCanHandleSourceSetConfiguration() {
        writeFile("build.gradle", '''
            plugins {
              id 'sktools-xjc-plugin'
              id 'idea'
            }
            
            sourceSets {
                main {
                    xjc {
                        schema {
                            srcDir 'src/main/xsd'
                        }
                    }
                }
            }
        ''')

        testGradleBuild("ideaModule")
        assertThat(contentOf(file(rootProjectName() + ".iml")))
            .contains('"file://$MODULE_DIR$/build/xjc/main/main0Schema"')
    }


    @Test
    void canSpecifyTaskNameForGen() {
        writeFile("build.gradle", '''
            plugins {
              id 'sktools-xjc-plugin'
            }
            
            sourceSets {
                main.xjc {
                    schema {
                        genTaskName = 'genCustom'
                    }
                }
            }
        ''')

        BuildResult result = testGradleBuild("genCustom")
        assertThat(result.task(":genCustom"))
            .isNotNull();
    }


    @Test
    void canSpecifyGenOutputPath() {
        writeFile("build.gradle", '''
            plugins {
              id 'sktools-xjc-plugin'
              id 'idea'
            }
            
            sourceSets {
                main.xjc {
                    schema {
                        genOutputPath = 'generated/custom'
                    }
                }
            }
        ''')

        testGradleBuild("ideaModule")

        //tests
        assertThat(file('generated/custom')).exists().isDirectory()

        assertThat(contentOf(file(rootProjectName() + ".iml")))
            .contains('"file://$MODULE_DIR$/generated/custom"') //customized placement
    }

    /**
     * Verifiserer at {@link org.gradle.api.file.SourceDirectorySet#srcDirs srcDirs} kan konfigureres.
     * Denne er i tillegg til {@link org.gradle.api.file.SourceDirectorySet#srcDir srcDir}.
     */
    @Test
    void testSrcDirs() {

        writeFile("src/main/xsd/simple.xsd", "")
        writeFile("src/main/xsd1/simple1.xsd", "")
        writeFile("src/main/xsd2/simple2.xsd", "")
        writeFile("src/main/xsd3/simple3.xsd", "")

        writeFile("build.gradle", '''
            plugins {
              id 'sktools-xjc-plugin'
            }
            
            sourceSets {
                main.xjc {
                    schema {
                        srcDir 'src/main/xsd1'
                        srcDir 'src/main/xsd2'
                        srcDirs 'src/main/xsd3', 'src/main/xsd'
                    }
                }
            }
            
            task echoSourceFiles() {
              doLast {
                project.tasks[project.sourceSets.main.xjc[0].genTaskName].getSource().files.each {
                  println it.name
                }
              }
            }
            
        ''')

        BuildResult buildResult = testGradleBuild("echoSourceFiles")
        //asserts the results
        assertThat(buildResult.getOutput()).
            contains(
                "simple.xsd"
                , "simple1.xsd"
                , "simple2.xsd"
                , "simple3.xsd"
            )
    }


    /**
     * jaxb-xjc har ANT som optional avhengighet.
     * Denne skal en ikke trenge å laste ned.
     */
    @Test
    void doesNotHaveAntOnClasspath() {
        final Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-xjc-plugin'

            repositories {
                maven { url = testProperties.MAVEN_REPO }
            }

        }

        assertThat(project.configurations.jaxb.resolvedConfiguration.getResolvedArtifacts()).
                extracting("artifact.name").
                contains("jaxb-xjc").doesNotContain("ant")
    }

}