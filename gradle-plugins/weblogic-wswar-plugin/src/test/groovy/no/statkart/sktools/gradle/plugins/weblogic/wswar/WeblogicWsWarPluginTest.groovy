package no.statkart.sktools.gradle.plugins.weblogic.wswar


import no.statkart.sktools.gradle.testutils.TestKitBase
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.testng.SkipException
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
     * Dependencies to run jwsc for WLS 12.2.1.3 with JDK8
     */
    static String WLS12_2_1_TOOLS = '''([
        'com.oracle.fmwshare:com.oracle.webservices.wls.wls-soap-stack-impl:12.2.1-3-0',
        'com.oracle.weblogic:wls_sharedLibraries.com.oracle.webservices.wls.jaxws-wlswss-client:12.2.1-3-0',
        'com.oracle.weblogic:pcl2:12.2.1-3-0',
        'com.oracle.weblogic:com.bea.core.jsr166e:12.2.1-3-0',
        'com.oracle.weblogic:com.bea.core.beangen:12.2.1-3-0',
        'com.oracle.weblogic:com.bea.core.annogen:12.2.1-3-0',
        'com.oracle.weblogic:com.oracle.weblogic.application:12.2.1-3-0',
        'com.oracle.weblogic:com.bea.core.descriptor.application:12.2.1-3-0',
        'com.oracle.weblogic:com.bea.core.descriptor.application.binding:12.2.1-3-0',
        'com.oracle.weblogic:com.bea.core.descriptor.j2ee:12.2.1-3-0',
        'com.oracle.weblogic:com.bea.core.descriptor.settable.binding:12.2.1-2-0',
        'com.oracle.weblogic:com.bea.core.xml.staxb.runtime:12.2.1-3-0',
        'com.oracle.weblogic:javax.javaee-api:12.2.1-3-0',
        'com.oracle.soa:com.bea.core.xml.xmlbeans:12.2.1-3-0',
    ])'''



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
        writeFileUTF8("build.gradle", """\
            plugins {
              id 'sktools.weblogic-wswar'
            }

            repositories {
                maven { url = '${testProperties.MAVEN_REPO}' }
            }

            dependencies {
              weblogicProvided $WLS12_2_1_TOOLS
            }
        """)

        if (JavaVersion.current().isJava9Compatible()) {
            throw new SkipException("JwscTask only for JDK8!")
        }

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
        writeFileUTF8("build.gradle", """\
            plugins {
              id 'sktools.weblogic-wswar'
            }

        repositories {
            maven { url = '${testProperties.MAVEN_REPO}' }
        }

        dependencies {
            weblogicProvided $WLS12_2_1_TOOLS
        }
        """)

        if (JavaVersion.current().isJava9Compatible()) {
            throw new SkipException("JwscTask only for JDK8!")
        }

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

        File weblogicSourceFile = createEmptyFile("src/weblogic/java/SomeFile.java")
        File mainSourceFile = createEmptyFile("src/main/java/Main.java")

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
        File weblogicResourceFile = createEmptyFile("src/weblogic/resources/weblogic.txt")
        File mainResourceFile = createEmptyFile("src/main/resources/main.txt")
        File otherResourceFile = createEmptyFile("src/other/resources/other.txt")


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
        File weblogicSourceFile = createEmptyFile('src/weblogic/java/DummyWLS.java')
        File divSourceFile = createEmptyFile('src/div/java/Div.java')

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
     * Tanken er da at {@source main} inneholder felles kildekode, mens man legger plattform spesifikk kildekode inn i forskjellige source sets, eks {@source weblogic}.
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
                weblogicImplementation project(path: ':', configuration: 'runtimeElements')   // rootProject.path == ':'

                //felles bibliotek
                implementation files('lib/common.jar')
            }
        }

        File weblogicSourceFile = createEmptyFile("src/weblogic/java/WLS.java")
        File mainSourceFile = createEmptyFile("src/main/java/Main.java")

        File weblogicResource = createEmptyFile("src/weblogic/resources/wls.txt")
        File mainResource = createEmptyFile("src/main/resources/main.txt")
        File commonResource = createEmptyFile("src/common/resources/common.txt")


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

        //tester kjente artifakter
        File mainJarFile = file('lib/common.jar')
        assertThat(project.getConfigurations().getByName('weblogicCompileClasspath')).contains(mainJarFile)
        assertThat(project.getConfigurations().getByName('weblogicRuntimeClasspath')).contains(mainJarFile)
        assertThat(project.getConfigurations().getByName('compileClasspath')).contains(mainJarFile)
        assertThat(project.getConfigurations().getByName('runtimeClasspath')).contains(mainJarFile)

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
        writeFileUTF8("build.gradle", """\
            plugins {
              id 'java'
              id 'sktools.weblogic-wswar'
            }

            repositories {
                maven { url = '${testProperties.MAVEN_REPO}' }
            }

            dependencies {
                compileOnly 'jakarta.xml.ws:jakarta.xml.ws-api:2.3.2'
                weblogicProvided $WLS12_2_1_TOOLS
            }
        """)

        if (JavaVersion.current().isJava9Compatible()) {
            throw new SkipException("JwscTask only for JDK8!")
        }

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