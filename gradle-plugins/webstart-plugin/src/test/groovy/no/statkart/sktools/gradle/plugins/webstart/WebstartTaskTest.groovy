package no.statkart.sktools.gradle.plugins.webstart

import org.gradle.api.XmlProvider
import org.testng.Assert
import org.testng.annotations.Test
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.WebstartProjectBuilder
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
        ProjectHelper projectHelper = WebstartProjectBuilder.builder().withName('root').applyWebstartPlugin().build()
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
                        application.mainClass 'AnotherLauncher'
                        resources {
                            javaRuntime '1.6+', '128m', '256m', 'http://some.download/location' //shortcut notation
                            runtime {   //same as above but only with vmArgs set.
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

        Assert.assertEquals(configurations[0].resources.runtimes.size(), 1)
        configurations[0].resources.runtimes.each {
            Assert.assertEquals(it.version, '1.5+')
            Assert.assertEquals(it.href, "http://java.sun.com/products/autodl/j2se")
            Assert.assertEquals(it.xms, null)
            Assert.assertEquals(it.xmx, "128m")
            Assert.assertEquals(it.vmArgs, null)
        }
        Assert.assertEquals(configurations[1].resources.runtimes.size(), 2)
        configurations[1].resources.runtimes.each {
            Assert.assertEquals(it.version, '1.6+')
            Assert.assertEquals(it.href, "http://some.download/location")
            Assert.assertEquals(it.xms, "128m")
            Assert.assertEquals(it.xmx, "256m")
        }
        Assert.assertEquals(configurations[1].resources.runtimes[0].vmArgs, null)
        Assert.assertEquals(configurations[1].resources.runtimes[1].vmArgs, "someargs")
    }
    
    /**
     * Generering av jnlp fil i {@link WebstartTask}.
     *
     * Testens mål er å verifisere at all all konfigurasjon kommer ut ihht til jnlp filsyntaks.
     */
    @Test
    void testJnlpFileGeneration() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = WebstartProjectBuilder.builder().withName('root').build()
        projectHelper.setProjectProperties(version: 101)

        final Project project = projectHelper.project;

        project.task('webstart', type: WebstartTask) {
            jnlp {
                jnlpFilename = 'client1.jnlp'

                title('title1')
                vendor('testvendor')
                description('some description.')
                homepage('ftp://example.net')
                resources {
                    runtime {
                        version('1.5.0')
                        href('downloadLink')
                        xms('1024m')
                        xmx('2g')
                        vmArgs('-kewlargs')
                    }
                    runtime {
                        version('1.6+')
                    }
                    systemProperties('jnlp.versionEnabled': false)
                    systemProperties('jnlp.versionEnabled': true)   //overskriver forrige
                    systemProperties('uhuh': 'whatagoodfeelin')
                }

                application {
                    mainClass('my.Launcher')
                }

                withXml { XmlProvider xmlProvider ->
                    Node jnlp = xmlProvider.asNode()
                    jnlp.resources[0].appendNode('property', [name: 'withXml', value: 'oh,yeah'])
                }
            }
        }

        project.tasks.webstart.execute()


        projectHelper.assertFileExists('build/webstart/client1.jnlp') {
            def jnlp = new XmlSlurper().parseText(it.text)
            Assert.assertEquals(jnlp.@spec.text(), '1.5+')
            Assert.assertEquals(jnlp.information[0].title.text(), 'title1')
            Assert.assertEquals(jnlp.information[0].vendor.text(), 'testvendor')
            Assert.assertEquals(jnlp.information[0].description.text(), 'some description.')
            Assert.assertEquals(jnlp.information[0].homepage.@href.text(), 'ftp://example.net')

            Assert.assertEquals(jnlp.'application-desc'[0].@'main-class'.text(), 'my.Launcher')

            Assert.assertEquals(jnlp.resources[0].j2se[0].@version.text(), '1.5.0')
            Assert.assertEquals(jnlp.resources[0].j2se[0].@href.text(), 'downloadLink')
            Assert.assertEquals(jnlp.resources[0].j2se[0].@'initial-heap-size'.text(), '1024m')
            Assert.assertEquals(jnlp.resources[0].j2se[0].@'max-heap-size'.text(), '2g')
            Assert.assertEquals(jnlp.resources[0].j2se[0].@'java-vm-args'.text(), '-kewlargs')

            Assert.assertEquals(jnlp.resources[0].j2se[1].@version.text(), '1.6+')

            Assert.assertEquals(jnlp.resources[0].property[0].@name.text(), 'jnlp.versionEnabled')
            Assert.assertEquals(jnlp.resources[0].property[0].@value.text(), 'true')
            Assert.assertEquals(jnlp.resources[0].property[1].@name.text(), 'uhuh')
            Assert.assertEquals(jnlp.resources[0].property[1].@value.text(), 'whatagoodfeelin')
            Assert.assertEquals(jnlp.resources[0].property[2].@name.text(), 'withXml')
            Assert.assertEquals(jnlp.resources[0].property[2].@value.text(), 'oh,yeah')

            Assert.assertEquals(jnlp.resources[1].children().size(), 0) //tom resources

        }
    }

}
