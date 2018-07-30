package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import org.gradle.api.XmlProvider
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import org.testng.annotations.Test
import no.statkart.sktools.gradle.testutils.ProjectHelper
import org.gradle.api.Project

/**
 * Test av {@link WebstartTask}
 *
 * @author Leif Lislegård
 * @author Tor Egil R. Strand
 */
class WebstartTaskTest {

    /**
     * Tester og demonstrerer angivelse av konfigurasjon.
     */
    @Test
    void testConfigurationNoResources() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder('root').build {
            apply plugin: 'sktools-webstart-plugin'
        }
        projectHelper.setProjectProperties(version: 101, description: 'Project description')

        projectHelper.configureProject {
            webstart {
                client {
                    jnlp {
                        jnlpFilename 'goodClient.jnlp'
                        description 'Description client1'
                        title 'Client1 title'
                        vendor 'MyCompany'
                        homepage 'http://intra.statkart.no'
                        applicationMainClass 'some.pkg.MyApplicationLauncher'
                        resources {
                            javaRuntime '1.5+'
                        }
                    }
                    jnlp {
                        jnlpFilename 'client2.jnlp'
                        description project.description
                        title 'Client2 title'
                        vendor 'SomeCompany'
                        homepage '\\\\intra\\somefolder\\someproject\\index.html'
                        application {
                            mainClass 'AnotherLauncher'
                            arg 'argument1'
                        }
                        resources {
                            javaRuntime '1.6+', '128m', '256m', 'http://some.download/location' //shortcut notation
                            javaRuntime {   //same as above but only with vmArgs set.
                                version '1.6+'
                                href 'http://some.download/location'
                                xms '128m'
                                xmx '256m'
                                vmArgs 'someargs'
                            }
                        }
                    }
                }
            }
        }

        projectHelper.initializeProject()


        final Project project = projectHelper.project
        final WebstartTask task = project.tasks.getByName('genClientJnlp') as WebstartTask
        final List<JnlpConfiguration> configurations = task.jnlpConfigurations;

        Assert.assertEquals(configurations[0].jnlpFilename, 'goodClient.jnlp')
        Assert.assertEquals(configurations[1].jnlpFilename, 'client2.jnlp')

        Assert.assertEquals(configurations[0].description, "Description client1")
        Assert.assertEquals(configurations[1].description, "Project description")

        Assert.assertEquals(configurations[0].title, "Client1 title")
        Assert.assertEquals(configurations[1].title, "Client2 title")

        Assert.assertEquals(configurations[0].vendor, "MyCompany")
        Assert.assertEquals(configurations[1].vendor, "SomeCompany")

        Assert.assertEquals(configurations[0].homepage, "http://intra.statkart.no")
        Assert.assertEquals(configurations[1].homepage, "\\\\intra\\somefolder\\someproject\\index.html")

        Assert.assertEquals(configurations[0].application.mainClass, 'some.pkg.MyApplicationLauncher')
        Assert.assertEquals(configurations[1].application.mainClass, 'AnotherLauncher')
        Assert.assertEquals(configurations[1].application.args[0], 'argument1')

        configurations[0].resources.runtimes.flatten().with { ArrayList<JavaRuntimeConfiguration> runtimesConfiguration0 ->
            Assert.assertEquals(runtimesConfiguration0.size(), 1)
            runtimesConfiguration0.each {
                Assert.assertEquals(it.version, '1.5+')
                Assert.assertEquals(it.href, "http://java.sun.com/products/autodl/j2se")
                Assert.assertEquals(it.xms, null)
                Assert.assertEquals(it.xmx, "128m")
                Assert.assertEquals(it.vmArgs, null)
            }
        }

        configurations[1].resources.runtimes.flatten().with { ArrayList<JavaRuntimeConfiguration> runtimesConfiguration1 ->
            Assert.assertEquals(runtimesConfiguration1.size(), 2)
            runtimesConfiguration1.each {
                Assert.assertEquals(it.version, '1.6+')
                Assert.assertEquals(it.href, "http://some.download/location")
                Assert.assertEquals(it.xms, "128m")
                Assert.assertEquals(it.xmx, "256m")
            }
            Assert.assertEquals(runtimesConfiguration1[0].vmArgs, null)
            Assert.assertEquals(runtimesConfiguration1[1].vmArgs, "someargs")
        }
    }


    /**
     * Builds a webstart project with extensive use of configuration(s).
     * <p>
     *     Webstart project; the {@code root} project; depends on submodules {@code main} and {@code dep}.
     * </p>
     *
     * @return a pre built jnlp file from webstart task.
     */
    private static ProjectHelper jnlpFileFromFullConfiguration_instance;
    public static ProjectHelper jnlpFileFromFullConfiguration() {
        if (!jnlpFileFromFullConfiguration_instance) {
            //forks a new project in a temp folder
            ProjectHelper projectHelper = GradleProjectBuilder.builder('root').build {
            }
            projectHelper.setProjectProperties(version: 101)

            Project depProject = ProjectBuilder.builder().withName('dep').withParent(projectHelper.project).build()
            depProject.apply(plugin: 'java')
            depProject.jar.manifest.attributes 'Implementation-Version': '0.1'

            Project mainProject = ProjectBuilder.builder().withName('main').withParent(projectHelper.project).build()
            mainProject.apply(plugin: 'java')
            mainProject.jar.manifest.attributes 'Implementation-Version': '0.1'

            final Project webstartProject = projectHelper.project;

            mainProject.jar.execute()
            depProject.jar.execute()

            webstartProject.task('webstart', type: WebstartTask) {
                jarResources webstartProject.files(mainProject.jar, depProject.jar)
                mainJar webstartProject.files(mainProject.jar)
                jnlp {
                    jnlpFilename = 'client1.jnlp'

                    title('title1')
                    vendor('testvendor')
                    description('some description.')
                    homepage('ftp://example.net')
                    resources {
                        javaRuntime {
                            version('1.5.0')
                            href('downloadLink')
                            xms('1024m')
                            xmx('2g')
                            vmArgs('-kewlargs')
                        }
                        javaRuntime {
                            version('1.6+')
                        }
                        systemProperties('jnlp.versionEnabled': false)
                        systemProperties('jnlp.versionEnabled': true)   //overskriver forrige
                        systemProperties('uhuh': 'whatagoodfeelin')
                    }

                    application {
                        mainClass 'my.Launcher'
                    }

                    withXml { XmlProvider xmlProvider ->
                        Node jnlp = xmlProvider.asNode()
                        jnlp.resources[0].appendNode('property', [name: 'withXml', value: 'oh,yeah'])
                    }
                }

                //dummy configuration for testing javaFx
                jnlp {
                    jnlpFilename 'client2FX.jnlp'
                    resources {
                        javaFxRuntime {
                            version "1.1+"
                            href 'href1'
                        }
                        javaFxRuntime version: "1.1+", href: 'href1'
                        javaFxRuntime(version: "1.1+") {
                            href 'href1'
                        }
                    }
                }

            }

            webstartProject.tasks.webstart.execute()

            jnlpFileFromFullConfiguration_instance = projectHelper
        }

        return jnlpFileFromFullConfiguration_instance
    }


    @Test
    void jnlpHasSpecVersion() {
        ProjectHelper projectHelper = jnlpFileFromFullConfiguration()
        projectHelper.assertFileExists('build/webstart/client1.jnlp') {
            def jnlp = new XmlSlurper().parseText(it.text)

            Assert.assertEquals(jnlp.@spec.text(), '1.6+')
        }
    }

    @Test
    void jnlpHasInformation() {
        ProjectHelper projectHelper = jnlpFileFromFullConfiguration()
        projectHelper.assertFileExists('build/webstart/client1.jnlp') {
            def jnlp = new XmlSlurper().parseText(it.text)

            Assert.assertEquals(jnlp.information[0].title.text(), 'title1')
            Assert.assertEquals(jnlp.information[0].vendor.text(), 'testvendor')
            Assert.assertEquals(jnlp.information[0].description.text(), 'some description.')
            Assert.assertEquals(jnlp.information[0].homepage.@href.text(), 'ftp://example.net')
        }
    }

    @Test
    void jnlpHasMainClass() {
        ProjectHelper projectHelper = jnlpFileFromFullConfiguration()
        projectHelper.assertFileExists('build/webstart/client1.jnlp') {
            def jnlp = new XmlSlurper().parseText(it.text)

            Assert.assertEquals(jnlp.'application-desc'[0].@'main-class'.text(), 'my.Launcher')
        }
    }

    @Test
    void jnlpHasRuntime() {
        ProjectHelper projectHelper = jnlpFileFromFullConfiguration()
        projectHelper.assertFileExists('build/webstart/client1.jnlp') {
            def jnlp = new XmlSlurper().parseText(it.text)

            Assert.assertEquals(jnlp.resources[0].j2se[0].@version.text(), '1.5.0')
            Assert.assertEquals(jnlp.resources[0].j2se[0].@href.text(), 'downloadLink')
            Assert.assertEquals(jnlp.resources[0].j2se[0].@'initial-heap-size'.text(), '1024m')
            Assert.assertEquals(jnlp.resources[0].j2se[0].@'max-heap-size'.text(), '2g')
            Assert.assertEquals(jnlp.resources[0].j2se[0].@'java-vm-args'.text(), '-kewlargs')
        }
    }

    @Test
    void jnlpCanHaveMultipleRuntimes() {
        ProjectHelper projectHelper = jnlpFileFromFullConfiguration()
        projectHelper.assertFileExists('build/webstart/client1.jnlp') {
            def jnlp = new XmlSlurper().parseText(it.text)

            Assert.assertTrue(jnlp.resources[0].j2se.size() >= 2)
            Assert.assertEquals(jnlp.resources[0].j2se[0].@version.text(), '1.5.0')
            Assert.assertEquals(jnlp.resources[0].j2se[1].@version.text(), '1.6+')
        }
    }


    @Test
    void jnlpCanHaveProperties() {
        ProjectHelper projectHelper = jnlpFileFromFullConfiguration()
        projectHelper.assertFileExists('build/webstart/client1.jnlp') {
            def jnlp = new XmlSlurper().parseText(it.text)

            Assert.assertEquals(jnlp.resources[0].property[0].@name.text(), 'jnlp.versionEnabled')
            Assert.assertEquals(jnlp.resources[0].property[0].@value.text(), 'true')
            Assert.assertEquals(jnlp.resources[0].property[1].@name.text(), 'uhuh')
            Assert.assertEquals(jnlp.resources[0].property[1].@value.text(), 'whatagoodfeelin')
            Assert.assertEquals(jnlp.resources[0].property[2].@name.text(), 'withXml')
            Assert.assertEquals(jnlp.resources[0].property[2].@value.text(), 'oh,yeah')
        }
    }

    @Test
    void jnlpCanHaveResources() {
        ProjectHelper projectHelper = jnlpFileFromFullConfiguration()
        projectHelper.assertFileExists('build/webstart/client1.jnlp') {
            def jnlp = new XmlSlurper().parseText(it.text)

            Assert.assertEquals(jnlp.resources[0].jar[0].@href.text(), 'lib/main.jar')
            Assert.assertEquals(jnlp.resources[0].jar[0].@size.text(), '280')
            Assert.assertEquals(jnlp.resources[0].jar[0].@version.text(), '0.1')

            Assert.assertEquals(jnlp.resources[0].jar[1].@href.text(), 'lib/dep.jar')
            Assert.assertEquals(jnlp.resources[0].jar[1].@size.text(), '280')
            Assert.assertEquals(jnlp.resources[0].jar[1].@version.text(), '0.1')
        }
    }

    @Test
    void jnlpHasTaggedMainJar() {
        ProjectHelper projectHelper = jnlpFileFromFullConfiguration()
        projectHelper.assertFileExists('build/webstart/client1.jnlp') {
            def jnlp = new XmlSlurper().parseText(it.text)

            Assert.assertEquals(jnlp.resources[0].jar[0].@href.text(), 'lib/main.jar')
            Assert.assertEquals(jnlp.resources[0].jar[0].@main.text(), 'true')

            Assert.assertEquals(jnlp.resources[0].jar[1].@href.text(), 'lib/dep.jar')
            Assert.assertEquals(jnlp.resources[0].jar[1].@main.text(), '')
        }
    }

    /**
     * Demonsrerer angivelse av flere resources-elementer i jnlp
     */
    @Test
    void jnlpCanHaveMultipleResourceLists() {
        ProjectHelper projectHelper = jnlpFileFromFullConfiguration()
        projectHelper.assertFileExists('build/webstart/client1.jnlp') {
            def jnlp = new XmlSlurper().parseText(it.text)

            Assert.assertTrue(jnlp.resources[0].children().size() > 0)
            Assert.assertTrue(jnlp.resources[1].children().size() == 0) //tom resources
        }
    }



    /**
     * Demonstrerer angivelse av java-fx runtime [SKTOOLS-120]
     */
    @Test
    void javaFX() {
        ProjectHelper projectHelper = jnlpFileFromFullConfiguration()
        projectHelper.assertFileExists('build/webstart/client2FX.jnlp') {
            def jnlp = new XmlSlurper().parseText(it.text)


            jnlp.resources.'javafx-runtime'.each {
                Assert.assertEquals(it.@version.text(), '1.1+', 'Java FX version')
                Assert.assertEquals(it.@href.text(), 'href1', 'Java FX href download url')
            }
        }
    }

    /**
     * Standard setting
     */
    @Test
    void jnlpHasAllPermissions() {
        ProjectHelper projectHelper = jnlpFileFromFullConfiguration()
        projectHelper.assertFileExists('build/webstart/client1.jnlp') {
            def jnlp = new XmlSlurper().parseText(it.text)

            Assert.assertTrue(jnlp.security[0].childNodes().size() > 0)
            Assert.assertTrue(jnlp.security[0].childNodes()[0].name == 'all-permissions')
        }
    }


}
