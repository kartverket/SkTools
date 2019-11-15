package no.statkart.sktools.gradle.plugins.weblogic.wswar


import no.statkart.sktools.gradle.testutils.TestKitBase
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.testng.annotations.Test

import java.util.zip.ZipFile

import static java.util.Collections.list
import static no.statkart.sktools.gradle.plugins.weblogic.wswar.WeblogicTestUtil.writeDemoServiceWSBean
import static no.statkart.sktools.gradle.plugins.weblogic.wswar.WeblogicTestUtil.writeExceptionService01
import static no.statkart.sktools.gradle.plugins.weblogic.wswar.WeblogicTestUtil.writeExceptionService01Exceptions
import static org.assertj.core.api.Assertions.assertThat

/**
 * Test av {@link WeblogicWsWarPlugin}
 *
 * @author Leif Lislegård
 */
class WeblogicWsWarPluginTest extends TestKitBase {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testApplyPlugin() {
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-weblogic-wswar-plugin'
        }

        assert project.plugins.hasPlugin('sktools-weblogic-wswar-plugin')
    }



    /**
     * Tester og demonstrerer angivelse av konfigurasjon
     */
    @Test
    void testWeblogicSourceSetConfiguration() {
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-weblogic-wswar-plugin'

            sourceSets.weblogic {
                java.srcDir 'scr/someJavaSourceDir'
                resources.srcDir 'scr/someResourcesDir'
            }
        }

        JavaPluginConvention javaConvention = project.getConvention().getPlugins().get("java");

        assert javaConvention.sourceSets['weblogic'].java.srcDirs.contains(project.file("scr/someJavaSourceDir"))
        assert javaConvention.sourceSets['weblogic'].resources.srcDirs.contains(project.file("scr/someResourcesDir"))
    }


    /**
     * Tester {@link WeblogicWsWarPlugin#WEBLOGIC_GEN_TASK_NAME}
     */
    @Test
    void testCompileTaskClasspath() {
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-weblogic-wswar-plugin'
        }

        AbstractCompile genTask = project.tasks[WeblogicWsWarPlugin.WEBLOGIC_GEN_TASK_NAME]

        for (def classesDir : project.sourceSets[WeblogicWsWarPlugin.WEBLOGIC_SOURCE_SET_NAME].output.classesDirs) {
            assert genTask.classpath.contains(classesDir) // forventer kompilerte classfiler på classpath
        }
    }


    /**
     * Tester task med default verdier
     *
     * @see WeblogicWsWarPlugin#WEBLOGIC_GEN_TASK_NAME
     */
    @Test
    void testCompileTask() {
        writeFile("build.gradle", """
            plugins {
              id 'sktools-weblogic-wswar-plugin'
            }
        """)
        writeGradleProperties(testProperties.findAll {
            'WEBLOGIC_HOME'.equals(it.key) || 'WEBLOGIC_VERSION'.equals(it.key)
        });

        // java kildekode for en testservice
        writeDemoServiceWSBean(file("src/weblogic/java"))

        assertNoFailures(testGradleBuild(":genWeblogic"))

        assertThat(file("build/weblogic/webapp/WEB-INF/TestServiceWS_v1.wsdl")).exists()
    }

    /**
     * Tester at compile blir kalt og at war fil blir generert
     *
     * @see WeblogicWsWarPlugin#WEBLOGIC_WAR_TASK_NAME
     */
    @Test
    void testWarTask() {
        writeFile("build.gradle", """
            plugins {
              id 'sktools-weblogic-wswar-plugin'
            }
        """)
        writeGradleProperties(testProperties.findAll {
            'WEBLOGIC_HOME'.equals(it.key) || 'WEBLOGIC_VERSION'.equals(it.key)
        });

        // java kildekode for en testservice
        writeDemoServiceWSBean(file("src/weblogic/java"))

        //eksekverer task
        BuildResult buildResult = testGradleBuild(":warWeblogic")

        //tester task
        assertThat(buildResult.task(':warWeblogic').getOutcome()).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(buildResult.task(':genWeblogic').getOutcome()).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(buildResult.task(':compileWeblogicJava').getOutcome()).isEqualTo(TaskOutcome.SUCCESS)

        assertThat(file("build/libs/${rootProjectName()}-weblogic.war")).exists()
    }


    /**
     * Tester oppsett av {@code SourceSet}
     */
    @Test
    void testSourceSetConfig() {
        //forks a new project in a temp folder
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-weblogic-wswar-plugin'
        }

        File weblogicSourceFile = writeFile("src/weblogic/java/SomeFile.java")
        File mainSourceFile = writeFile("src/main/java/Main.java")

        assert project.tasks['compileWeblogicJava'].source.contains(weblogicSourceFile)    //forventer tilgang til kildekode i weblogic source set
        assert !project.tasks['compileWeblogicJava'].source.contains(mainSourceFile)   //forventer ingen tilgang til kildekode i main source set
    }

    /**
     * Tester oppsett av {@code SourceSet} mtp resources
     */
    @Test
    void testResourcesConfig() {
        //forks a new project in a temp folder
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-weblogic-wswar-plugin'
        }

        //definerer noen filer
        File weblogicResourceFile = writeFile("src/weblogic/resources/weblogic.txt")
        File mainResourceFile = writeFile("src/main/resources/main.txt")
        File otherResourceFile = writeFile("src/other/resources/other.txt")


        //tester tilgang for en kjent task
        assert project.tasks['processWeblogicResources'].source.contains(weblogicResourceFile)    //forventer tilgang til ressursfiler i src/weblogic
        assert !project.tasks['processWeblogicResources'].source.contains(mainResourceFile)   //forventer INGEN tilgang til ressursfiler i src/main
        assert !project.tasks['processWeblogicResources'].source.contains(otherResourceFile)   //forventer INGEN tilgang til ressursfiler i src/other

        //legger til mappe
        project.tap {
            sourceSets {
                weblogic.resources.srcDir 'src/other/resources'
            }
        }

        //tester tilgang for en kjent task
        assert project.tasks['processWeblogicResources'].source.contains(weblogicResourceFile)    //forventer tilgang til ressursfiler i src/weblogic
        assert !project.tasks['processWeblogicResources'].source.contains(mainResourceFile)   //forventer FORTSATT INGEN tilgang til ressursfiler i src/main
        assert project.tasks['processWeblogicResources'].source.contains(otherResourceFile)   //forventer NÅ TILGANG tilgang til ressursfiler i src/other
    }

    /**
     * Tester oppsett av {@code SourceSet} med flere sourceDirs
     */
    @Test
    void testSourceConfig() {
        //forks a new project in a temp folder
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-weblogic-wswar-plugin'
        }

        // some java files...
        File weblogicSourceFile = writeFile('src/weblogic/java/DummyWLS.java')
        File divSourceFile = writeFile('src/div/java/Div.java')

        assert project.tasks['compileWeblogicJava'].source.contains(weblogicSourceFile)    //forventer tilgang til kildekode i weblogic source set
        assert !project.tasks['compileWeblogicJava'].source.contains(divSourceFile)   //forventer INGEN tilgang til kildekode i div source set

        //konfigurerer project
        project.tap {
            sourceSets {
                weblogic.java.srcDir 'src/div/java'
                weblogic.java.srcDir 'src/weblogic/java2'
            }

        }

        assert project.tasks['compileWeblogicJava'].source.contains(divSourceFile)   //forventer NÅ TILGANG til kildekode i div source set
    }

    /**
     * Tester oppsett av {@code SourceSet} i et tenkt multi build prosjekt.
     *
     * Tenker at main har paralell kompilering av plattform spesifik kode. Dette kan feks være for Weblogic, JBoss, mm.
     * Tanken er da at {@source main} inneholder felles kildekode, mens man legger plattfor spsifikk kildekode inn i forskjellige source sets, eks {@source weblogic}.
     *
     *
     * Testen setter opp enn felles resource katalog.
     * Det blir også demonstrert deklarasjon av avhengighet til 'main'
     */
    @Test
    void testJavaPluginIntegration() {
        //forks a new rootProject in a temp folder
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-weblogic-wswar-plugin'
            apply plugin: 'java'

            sourceSets {
                weblogic.resources.srcDir 'src/common/resources'
                main.resources.srcDir 'src/common/resources'
            }

            dependencies {
                //dette er muligens en knotete måte å deklarere det på..
                weblogicCompile project(path: ':', configuration: 'runtime')   // rootProject.path == ':'

                //felles bibliotek
                compile files('lib/common.jar')
            }
        }

        File weblogicSourceFile = writeFile("src/weblogic/java/WLS.java")
        File mainSourceFile = writeFile("src/main/java/Main.java")

        File weblogicResource = writeFile("src/weblogic/resources/wls.txt")
        File mainResource = writeFile("src/main/resources/main.txt")
        File commonResource = writeFile("src/common/resources/common.txt")


        assert project.tasks['compileWeblogicJava'].source.contains(weblogicSourceFile)    //forventer tilgang til kildekode for  weblogicCompile configuration
        assert !project.tasks['compileJava'].source.contains(weblogicSourceFile)    //forventer INGEN tilgang til kildekode for javaCompile configuration

        assert !project.tasks['compileWeblogicJava'].source.contains(mainSourceFile)    //forventer INGEN tilgang til kildekode for weblogicCompile configuration
        assert project.tasks['compileJava'].source.contains(mainSourceFile)    //forventer tilgang til kildekode for javaCompile configuration


        assert project.tasks['processWeblogicResources'].source.contains(weblogicResource)    //forventer tilgang til ressurser for  processWeblogicResources
        assert !project.tasks['processResources'].source.contains(weblogicResource)    //forventer INGEN tilgang til ressurser for processResources

        assert !project.tasks['processWeblogicResources'].source.contains(mainResource)    //forventer INGEN tilgang til ressurser for processWeblogicResources
        assert project.tasks['processResources'].source.contains(mainResource)    //forventer tilgang til ressurser for processResources

        assert project.tasks['processWeblogicResources'].source.contains(commonResource)    //forventer tilgang til ressurser for processWeblogicResources
        assert project.tasks['processResources'].source.contains(commonResource)    //forventer tilgang til ressurser for processResources

        //henter ut filer for configurations
        Iterable<File> weblogicArtifacts = project.getConfigurations().getByName('weblogicRuntime').getFiles()
        Iterable<File> mainArtifacts = project.getConfigurations().getByName('runtime').getFiles()

        //tester kjente artifakter
        File mainJarFile = file('lib/common.jar')
        assert weblogicArtifacts.contains(mainJarFile)
        assert mainArtifacts.contains(mainJarFile)

        //sjekker at artifakt ifra 'main' blir med på classpath
        assert project.tasks['compileWeblogicJava'].classpath.contains(mainJarFile)
    }




    /**
     * Demonstrerer oppsett med egne exception klasser (i main)
     *
     * @see WeblogicWsWarPlugin#WEBLOGIC_WAR_TASK_NAME
     */
    @Test
    void testCustomExceptions() {
        writeFile("build.gradle", """
            plugins {
              id 'java'
              id 'sktools-weblogic-wswar-plugin'
            }
        """)
        writeGradleProperties(testProperties.findAll {
            'WEBLOGIC_HOME'.equals(it.key) || 'WEBLOGIC_VERSION'.equals(it.key)
        });


        // exception klasser
        writeExceptionService01Exceptions(file("src/main/java"))

        // java kildekode for en testservice
        writeExceptionService01(file("src/weblogic/java"))

        //eksekverer task
        BuildResult buildResult = testGradleBuild(":warWeblogic")

        //tester task
        File file = file("build/libs/${rootProjectName()}-weblogic.war")
        assertThat(file).exists()

        ZipFile zip = new ZipFile(file)
        try {
            assertThat(list(zip.entries()))
                .extractingResultOf("getName")
                .as("Contents of jar file")
                .contains(
                    'WEB-INF/classes/WebConfig.class',
                    'WEB-INF/classes/exceptiondemo01/ExceptionService1WSBean.class',
                )
            .doesNotContain(
                'WEB-INF/web.xml', // legges inn eksplisitt når ønskeligt
                'WEB-INF/classes/exceptiondemo01/exception/ServiceException.class', //ligger i jar fil (evt i internt domene)
                'WEB-INF/classes/exceptiondemo01/exception/ServiceFaultInfo.class', //ligger i jar fil (evt i internt domene)
            )
        } finally {
            zip.close()
        }
    }
}