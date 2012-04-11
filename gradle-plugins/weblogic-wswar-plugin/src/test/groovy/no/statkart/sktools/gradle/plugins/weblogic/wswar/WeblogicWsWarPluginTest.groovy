package no.statkart.sktools.gradle.plugins.weblogic.wswar

import org.testng.annotations.Test
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.Task

import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.WeblogicWsWarProjectBuilder
import org.gradle.api.file.FileCollection
import no.statkart.sktools.gradle.testutils.filewriter.WeblogicWsWarTestutilFilewriter

/**
 * Test av {@link WeblogicWsWarPlugin}
 *
 * @author Leif Lislegård
 */
class WeblogicWsWarPluginTest {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testAppplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()
        ProjectHelper projectHelper = new ProjectHelper(project)

        //konfigurerer project
        project.with {
            apply plugin: 'sktools-weblogic-wswar-plugin'
        }


        assert project.convention.plugins.weblogicWsWar != null
        Assert.assertTrue(project.convention.plugins.weblogicWsWar instanceof WeblogicWsWarConvention)

    }



    /**
     * Tester og demonstrerer angivelse av konfigurasjon
     */
    @Test
    void testConventionConfiguration() {
        ProjectHelper projectHelper = WeblogicWsWarProjectBuilder.builder().applyWsWarPlugin(true).build()

        projectHelper.configureProject {
            weblogicWsWar {
                sourceSet {
                    java.srcDir 'scr/someJavaSourceDir'
                    resources.srcDir 'scr/someResourcesDir'
                }
            }
        }


        JavaPluginConvention javaConvention = projectHelper.project.getConvention().getPlugins().get("java");


        assert javaConvention.sourceSets['weblogic'].java.srcDirs.contains(projectHelper.project.file('scr/someJavaSourceDir'))
        assert javaConvention.sourceSets['weblogic'].resources.srcDirs.contains(projectHelper.project.file('scr/someResourcesDir'))

    }


    /**
     * Tester task med default verdier
     *
     * @see WeblogicWsWarPlugin#COMPILE_WEBLOGIC_TASK_NAME
     */
    @Test
    void testCompileTask() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = WeblogicWsWarProjectBuilder.builder().applyWsWarPlugin(true).build()
        Project project = projectHelper.project

        //genererer java kildekode for en testservice
        use(WeblogicWsWarTestutilFilewriter) {
            projectHelper.writeDemoServiceWSBean('src/weblogic/java')
        }

        projectHelper.executeTask(WeblogicWsWarPlugin.COMPILE_WEBLOGIC_TASK_NAME)

        projectHelper.assertTaskExecutedNotSkipped(WeblogicWsWarPlugin.COMPILE_WEBLOGIC_TASK_NAME, '') { Task task ->
            assert task.outputs.hasOutput //skal ha fått deklarert at denne tasken har outputs
            assert !task.outputs.getFiles().isEmpty() //forventer genererte filer
            assert 1 == task.outputs.getFiles().getAsFileTree().findAll {it.name.endsWith('.wsdl')}.size() //antall wsdl filer generert
        }

    }

    /**
     * Tester at compile blir kalt og at war fil blir generert
     *
     * @see WeblogicWsWarPlugin#WEBLOGIC_WAR_TASK_NAME
     */
    @Test
    void testWarTask() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = WeblogicWsWarProjectBuilder.builder().applyWsWarPlugin(true).build()
        Project project = projectHelper.project

        //genererer java kildekode for en testservice
        use(WeblogicWsWarTestutilFilewriter) {
            projectHelper.writeDemoServiceWSBean('src/weblogic/java')
        }

        //eksekverer task
        projectHelper.executeTask(WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME)

        //tester task
        Task warTask = projectHelper.assertTaskExecutedNotSkipped(WeblogicWsWarPlugin.WEBLOGIC_WAR_TASK_NAME)
        projectHelper.assertTaskExecutedNotSkipped(WeblogicWsWarPlugin.COMPILE_WEBLOGIC_TASK_NAME)

        File warFile = projectHelper.assertFileExists('build/libs/WeblogicWsWarProjectBuilder-weblogic.war')

        assert warTask.getOutputs().getFiles().contains(warFile) //forventer at output inneholder denne filen

    }

    /**
     * Tester at følgende statiske variabler har riktig verdi
     * <ul>
     *  <li>{@link WeblogicWsWarPlugin#COMPILE_WEBLOGIC_TASK_NAME}
     *  <li>{@link WeblogicWsWarPlugin#PROCESS_WEBLOGIC_RESOURCES_TASK_NAME}
     *  </ul>
     */
    @Test
    void testTaskName() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = WeblogicWsWarProjectBuilder.builder().applyWsWarPlugin(false).build()
        Project project = projectHelper.project


        JavaPluginConvention javaPluginConvention = project.getConvention().getPlugins().get("java")
        SourceSet weblogicSourceSet = javaPluginConvention.getSourceSets().getByName(WeblogicWsWarPlugin.WEBLOGIC_SOURCE_SET_NAME)

        assert weblogicSourceSet.getCompileJavaTaskName() == WeblogicWsWarPlugin.COMPILE_WEBLOGIC_TASK_NAME
        assert weblogicSourceSet.getProcessResourcesTaskName() == WeblogicWsWarPlugin.PROCESS_WEBLOGIC_RESOURCES_TASK_NAME
    }

    /**
     * Tester oppsett av {@code SourceSet}
     */
    @Test
    void testSourceSetConfig() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = WeblogicWsWarProjectBuilder.builder().applyWsWarPlugin(false).build()
        Project project = projectHelper.project


        Collection<File> weblogicSourceFiles = null
        Collection<File> mainSourceFiles = null

        //generering av kildekode
        use(WeblogicWsWarTestutilFilewriter) {
            weblogicSourceFiles = projectHelper.writeDemoServiceWSBean('src/weblogic/java')
            mainSourceFiles = projectHelper.writeDummyClass('src/main/java')
        }

        weblogicSourceFiles.each {
            assert project.tasks['compileWeblogicJava'].source.contains(it)    //forventer tilgang til kildekode i weblogic source set
        }
        mainSourceFiles.each {
            assert !project.tasks['compileWeblogicJava'].source.contains(it)   //forventer ingen tilgang til kildekode i main source set
        }

    }

    /**
     * Tester oppsett av {@code SourceSet} mtp resources
     */
    @Test
    void testResourcesConfig() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = WeblogicWsWarProjectBuilder.builder().applyWsWarPlugin(false).build()
        Project project = projectHelper.project

        Collection<File> weblogicResourceFiles = null
        Collection<File> mainResourceFiles = null
        Collection<File> otherResourceFiles = null

        //genererer noen filer
        use(WeblogicWsWarTestutilFilewriter) {
            weblogicResourceFiles = projectHelper.writeDemoServiceWSBean('src/weblogic/resources')
            mainResourceFiles = projectHelper.writeDummyClass('src/main/resources')
            otherResourceFiles = projectHelper.writeDummyClass('src/other/resources')
        }

        //tester tilgang for en kjent task
        weblogicResourceFiles.each {
            assert project.tasks['processWeblogicResources'].source.contains(it)    //forventer tilgang til ressursfiler i src/weblogic
        }
        mainResourceFiles.each {
            assert !project.tasks['processWeblogicResources'].source.contains(it)   //forventer INGEN tilgang til ressursfiler i src/main
        }
        otherResourceFiles.each {
            assert !project.tasks['processWeblogicResources'].source.contains(it)   //forventer INGEN tilgang til ressursfiler i src/other
        }

        //legger til mappe
        projectHelper.configureProject {
            weblogicWsWar {
                sourceSet.resources.srcDir 'src/other/resources'
            }
        }

        //tester tilgang for en kjent task
        weblogicResourceFiles.each {
            assert project.tasks['processWeblogicResources'].source.contains(it)    //forventer tilgang til ressursfiler i src/weblogic
        }
        mainResourceFiles.each {
            assert !project.tasks['processWeblogicResources'].source.contains(it)   //forventer FORTSATT INGEN tilgang til ressursfiler i src/main
        }
        otherResourceFiles.each {
            assert project.tasks['processWeblogicResources'].source.contains(it)   //forventer NÅ TILGANG tilgang til ressursfiler i src/other
        }


    }

    /**
     * Tester oppsett av {@code SourceSet} med annet sourceDir
     */
    @Test
    void testSourceConfig() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = WeblogicWsWarProjectBuilder.builder().applyWsWarPlugin(false).build()
        Project project = projectHelper.project

        Collection<File> weblogicSourceFiles = null
        Collection<File> divSourceFiles = null
        Collection<File> sourceDirViaSourceSetSourceFiles = null

        //genererer java kildekode for en testservice
        use(WeblogicWsWarTestutilFilewriter) {
            weblogicSourceFiles = projectHelper.writeDemoServiceWSBean('src/weblogic/java')
            divSourceFiles = projectHelper.writeDummyClass('src/div/java')
            sourceDirViaSourceSetSourceFiles = projectHelper.writeDummyClass('src/weblogic/java2')
        }



        weblogicSourceFiles.each {
            assert project.tasks['compileWeblogicJava'].source.contains(it)    //forventer tilgang til kildekode i weblogic source set
        }
        divSourceFiles.each {
            assert !project.tasks['compileWeblogicJava'].source.contains(it)   //forventer INGEN tilgang til kildekode i div source set
        }
        sourceDirViaSourceSetSourceFiles.each {
            assert !project.tasks['compileWeblogicJava'].source.contains(it)    //forventer INGEN tilgang til kildekode i weblogic source set
        }

        //konfigurerer project
        projectHelper.configureProject {
            weblogicWsWar {
                sourceSet.java.srcDir 'src/div/java'
            }

            //alternativ konfigurasjon via JavaPluginConvention
            sourceSets {
                weblogic.java.srcDir 'src/weblogic/java2'
            }

        }

        weblogicSourceFiles.each {
            assert project.tasks['compileWeblogicJava'].source.contains(it)    //forventer tilgang til kildekode i weblogic source set
        }
        divSourceFiles.each {
            assert project.tasks['compileWeblogicJava'].source.contains(it)   //forventer NÅ TILGANG til kildekode i div source set
        }
        sourceDirViaSourceSetSourceFiles.each {
            assert project.tasks['compileWeblogicJava'].source.contains(it)    //forventer NÅ TILGANG til kildekode i weblogic source set
        }

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
     *
     *
     */
    @Test
    void testJavaPluginIntegration() {
        //forks a new rootProject in a temp folder
        ProjectHelper projectHelper = WeblogicWsWarProjectBuilder.builder().applyJavaPlugin().applyWsWarPlugin(false).withName('testproject').build()
        Project rootProject = projectHelper.project

        Collection<File> weblogicSourceFiles = null
        Collection<File> mainSourceFiles = null

        Collection<File> weblogicResources = null
        Collection<File> mainResources = null
        Collection<File> commonResources = null

        //genererer java kildekode for en testservice
        use(WeblogicWsWarTestutilFilewriter) {
            weblogicSourceFiles = projectHelper.writeDemoServiceWSBean('src/weblogic/java')
            mainSourceFiles = projectHelper.writeDummyClass('src/main/java')

            weblogicResources = projectHelper.writeDummyClass('src/weblogic/resources')
            mainResources = projectHelper.writeDummyClass('src/main/resources')
            commonResources = projectHelper.writeDummyClass('src/common/resources')
        }


        projectHelper.configureProject {
            //alternativ konfigurasjon via JavaPluginConvention
            sourceSets {
                weblogic.resources.srcDir 'src/common/resources'
                main.resources.srcDir 'src/common/resources'
            }

            dependencies {
                //dette er muligens en knotete måte å deklarere det på..
                weblogicCompile project(path: ':', configuration: 'runtime')   // rootProject.path == ':'
            }

        }

        weblogicSourceFiles.each {
            assert rootProject.tasks['compileWeblogicJava'].source.contains(it)    //forventer tilgang til kildekode for  weblogicCompile configuration
            assert !rootProject.tasks['compileJava'].source.contains(it)    //forventer INGEN tilgang til kildekode for javaCompile configuration
        }
        mainSourceFiles.each {
            assert !rootProject.tasks['compileWeblogicJava'].source.contains(it)    //forventer INGEN tilgang til kildekode for weblogicCompile configuration
            assert rootProject.tasks['compileJava'].source.contains(it)    //forventer tilgang til kildekode for javaCompile configuration
        }



        weblogicResources.each {
            assert rootProject.tasks['processWeblogicResources'].source.contains(it)    //forventer tilgang til ressurser for  processWeblogicResources
            assert !rootProject.tasks['processResources'].source.contains(it)    //forventer INGEN tilgang til ressurser for processResources
        }
        mainResources.each {
            assert !rootProject.tasks['processWeblogicResources'].source.contains(it)    //forventer INGEN tilgang til ressurser for processWeblogicResources
            assert rootProject.tasks['processResources'].source.contains(it)    //forventer tilgang til ressurser for processResources
        }

        commonResources.each {
            assert rootProject.tasks['processWeblogicResources'].source.contains(it)    //forventer tilgang til ressurser for processWeblogicResources
            assert rootProject.tasks['processResources'].source.contains(it)    //forventer tilgang til ressurser for processResources
        }

        //henter ut definerte artifakter
        FileCollection weblogicArtifacts = rootProject.getConfigurations().getByName('weblogicRuntime').getAllArtifactFiles()
        FileCollection mainArtifacts = rootProject.getConfigurations().getByName('runtime').getAllArtifactFiles()

        //tester kjente artifakter
        File mainJarFile = rootProject.file('build/libs/testproject.jar').with { File file ->
            assert weblogicArtifacts.contains(file)
            assert mainArtifacts.contains(file)
            return file
        }


        //sjekker at artifakt ifra 'main' blir med på classpath
        assert rootProject.tasks['compileWeblogicJava'].classpath.contains(mainJarFile)

    }


}