package no.statkart.sktools.gradle.plugins.webstart

import org.testng.annotations.Test
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.WebstartProjectBuilder
import org.gradle.api.Project
import org.gradle.util.GradleVersion

/**
 * Test av {@link WebstartTask}
 *
 * @author Leif Lislegård
 */
class WebstartTaskTest {



    /**
     * Tester at jar-filer finnes i jnlp, version.xml og i lib katalog
     */
    @Test
    void testLinkingAvRessursser() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = WebstartProjectBuilder.builder().withName('root').applyWebstartPlugin().build()

        final Project project = projectHelper.project;
        final WebstartConvention webstartConvention = project.convention.plugins[WebstartPlugin.CONVENTION_NAME]


        webstartConvention.clients.add new WebstartClientConfiguration(webstartConvention).with { WebstartClientConfiguration client ->
            client.signJars(false)
            client.outputPath('gen/clients')
            client.jnlpFile('client1.jnlp')

            client.jnlp.with { JnlpConfiguration jnlp ->
                jnlp.resources.add new ResourcesConfiguration(client.jnlp).with { ResourcesConfiguration resources ->
                    resources.libPath('lib')
                    resources.jarDependencies.files(projectHelper.gradleJars[0])
                    resources
                }
                jnlp
            }
            client
        }


        projectHelper.initializeProject(true)

        project.task(type: WebstartTask.class, 'testSomething') {
            clients = webstartConvention.clients
        }.execute()




        projectHelper.assertFileExists('gen/clients/lib') { File dir ->
            assert dir.isDirectory()
            assert dir.list().length == 2    //forventer at jarfiler + version.xml finnes
        }

        File versionXmlFile = projectHelper.assertFileExists('gen/clients/lib/version.xml')
        def versions = new XmlSlurper().parse(versionXmlFile)

        projectHelper.assertFileExists('gen/clients/client1.jnlp') {
            def jnlp = new XmlSlurper().parseText(it.text)

            String version = jnlp.resources[0].jar[0].@version.text()
            String href = jnlp.resources[0].jar[0].@href.text()

            assert href =~ 'lib/(.*)'    //forventer at jar fil ligger i lib katalog
            String resourceId = href.replaceAll('lib/(.*)', '$1')

            String gradleVersion = "-" + GradleVersion.current().getVersion()
            assert resourceId == projectHelper.gradleJars[0].name.replace(gradleVersion, "") //foventer filnavn strippet for versjon

            //henter alle definerte ressurser i version.xml
            def matchingResources = versions.resource.findAll { it.pattern.name.text() == resourceId }
            assert matchingResources.size() == 1
            assert matchingResources[0].pattern.name.text() == resourceId
            assert matchingResources[0].pattern.'version-id'.text() == version

            //filnavn for jar ifra version.xml
            String jarFileName = matchingResources[0].file.text()

            //sjekker at filen eksisterer på disk
            projectHelper.assertFileExists('gen/clients/lib/' + jarFileName)
        }

        def debug = 1
    }


    /**
     * Tester kopiering av jar-filer.
     *
     * Testens mål er å demonstrere at samme jarfiler kan bli kopiert til ulike lib kataloger.
     */
    @Test
    void testJarHandling() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = WebstartProjectBuilder.builder().withName('root').applyWebstartPlugin().build()

        final Project project = projectHelper.project;
        final WebstartConvention webstartConvention = project.convention.plugins[WebstartPlugin.CONVENTION_NAME]


        webstartConvention.clients.add new WebstartClientConfiguration(webstartConvention).with { WebstartClientConfiguration client ->
            client.signJars(false)
            client.outputPath('gen/clients')
            client.jnlpFile('client1.jnlp')

            client.jnlp.with { JnlpConfiguration jnlp ->
                jnlp.title('client1')
                jnlp.resources.add new ResourcesConfiguration(client.jnlp).with { ResourcesConfiguration resources ->
                    resources.libPath('lib1')
                    resources.jarDependencies.files(projectHelper.gradleJars[0])
                    resources
                }
                jnlp.resources.add new ResourcesConfiguration(client.jnlp).with { ResourcesConfiguration resources ->
                    resources.libPath('lib2')
                    resources.jarDependencies.files(projectHelper.gradleJars[0])
                    resources.jarDependencies.files(projectHelper.gradleJars[1])
                    resources
                }
                jnlp
            }
            client
        }

        //nesten lik klient som over.
        webstartConvention.clients.add new WebstartClientConfiguration(webstartConvention).with { WebstartClientConfiguration client ->
            client.signJars(false)
            client.outputPath('gen/clients')
            client.jnlpFile('client2.jnlp')

            client.jnlp.with { JnlpConfiguration jnlp ->
                jnlp.title('client2')
                jnlp.resources.add new ResourcesConfiguration(client.jnlp).with { ResourcesConfiguration resources ->
                    resources.libPath('lib1')
                    resources.jarDependencies.files(projectHelper.gradleJars[0])
                    resources
                }
                jnlp.resources.add new ResourcesConfiguration(client.jnlp).with { ResourcesConfiguration resources ->
                    resources.libPath('lib2')
                    resources.jarDependencies.files(projectHelper.gradleJars[0])
                    resources.jarDependencies.files(projectHelper.gradleJars[1])
                    resources
                }
                jnlp
            }
            client
        }


        projectHelper.initializeProject(true)

        def task = project.task(type: WebstartTask.class, 'testSomething') {
            clients = webstartConvention.clients
        }
                task.execute()


        assert projectHelper.assertFileExists('gen/clients/lib1') { File dir ->
            assert dir.isDirectory()
            assert dir.list().length == 2    //forventer at jarfiler + version.xml finnes
        }
        assert projectHelper.assertFileExists('gen/clients/lib2') { File dir ->
            assert dir.isDirectory()
            assert dir.list().length == 3    //forventer at jarfiler + version.xml finnes
        }

        def debug = 1
    }




    /**
     * Generering av jnlp fil i {@link WebstartTask}.
     *
     * Testens mål er å verifisere at all all konfigurasjon kommer ut ihht til jnlp filsyntaks.
     *
     */
    @Test
    void testJnlpFileGeneration() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = WebstartProjectBuilder.builder().withName('root').applyWebstartPlugin().build()
        projectHelper.setProjectProperties(version: 101)

        final Project project = projectHelper.project;
        final WebstartConvention webstartConvention = project.convention.plugins[WebstartPlugin.CONVENTION_NAME]


        webstartConvention.clients.add new WebstartClientConfiguration(webstartConvention).with { WebstartClientConfiguration client ->
            client.signJars(false)
            client.outputPath('gen/clients')
            client.jnlpFile('client1.jnlp')

            client.jnlp.with { JnlpConfiguration jnlp ->

                jnlp.title('title1')
                jnlp.vendor('testvendor')
                jnlp.description('some description.')
                jnlp.homepage('ftp://example.net')
                jnlp.resources.add new ResourcesConfiguration(client.jnlp).with { ResourcesConfiguration resources ->
                    resources.runtimes.add new JavaRuntimeConfiguration(resources).with { JavaRuntimeConfiguration javaRuntime ->
                        javaRuntime.version('1.5.0')
                        javaRuntime.href('downloadLink')
                        javaRuntime.xms('1024m')
                        javaRuntime.xmx('2g')
                        javaRuntime.vmArgs('-kewlargs')
                        javaRuntime
                    }
                    resources.runtimes.add new JavaRuntimeConfiguration(resources).with { JavaRuntimeConfiguration javaRuntime ->
                        javaRuntime.version('1.6+')
                        javaRuntime
                    }
                    resources.systemProperties('jnlp.versionEnabled': false)
                    resources.systemProperties('jnlp.versionEnabled': true)   //overskriver forrige
                    resources.systemProperties('uhuh': 'whatagoodfeelin')
                    resources
                }
                jnlp.resources.add new ResourcesConfiguration(client.jnlp).with { ResourcesConfiguration resources ->
                    //tom tag
                    resources
                }

                jnlp.application = new ApplicationConfiguration(jnlp).with { ApplicationConfiguration application ->
                    application.mainClass('my.Launcher')
                    application
                }
                jnlp
            }
            client
        }



        project.task(type: WebstartTask.class, 'testSomething') {
            clients = webstartConvention.clients
        }.execute()


        projectHelper.assertFileExists('gen/clients/client1.jnlp') {
            def jnlp = new XmlSlurper().parseText(it.text)
            assert jnlp.@spec.text() == '1.5+'
            assert jnlp.information[0].title.text() == 'title1'
            assert jnlp.information[0].vendor.text() == 'testvendor'
            assert jnlp.information[0].description.text() == 'some description.'
            assert jnlp.information[0].homepage.@href.text() == 'ftp://example.net'

            assert jnlp.'application-desc'[0].@'main-class'.text() == 'my.Launcher'

            assert jnlp.resources[0].j2se[0].@version.text() == '1.5.0'
            assert jnlp.resources[0].j2se[0].@href.text() == 'downloadLink'
            assert jnlp.resources[0].j2se[0].@'initial-heap-size'.text() == '1024m'
            assert jnlp.resources[0].j2se[0].@'max-heap-size'.text() == '2g'
            assert jnlp.resources[0].j2se[0].@'java-vm-args'.text() == '-kewlargs'

            assert jnlp.resources[0].j2se[1].@version.text() == '1.6+'

            assert jnlp.resources[0].property[0].@name.text() == 'jnlp.versionEnabled'
            assert jnlp.resources[0].property[0].@value.text() == 'true'
            assert jnlp.resources[0].property[1].@name.text() == 'uhuh'
            assert jnlp.resources[0].property[1].@value.text() == 'whatagoodfeelin'

            assert jnlp.resources[1].children().size() == 0 //tom resources

        }

        def debug = 1
    }

}
