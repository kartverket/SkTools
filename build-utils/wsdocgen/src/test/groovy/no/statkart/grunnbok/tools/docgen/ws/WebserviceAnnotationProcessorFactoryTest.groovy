package no.statkart.grunnbok.tools.docgen.ws

import org.testng.annotations.Test
import org.gradle.api.Project

import groovy.util.slurpersupport.GPathResult
import org.gradle.api.logging.LogLevel
import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.filewriter.XjcTestutilFilewriter
import no.statkart.sktools.gradle.testutils.filewriter.WsDocgenTestutilFilewriter

/**
 * Test av {@link WebserviceAnnotationProcessorFactory}
 *
 * @author Leif Lislegård
 */
class WebserviceAnnotationProcessorFactoryTest {

    /**
     * Tester eksekvering med default parametere
     */
    @Test
    void testDefaultConfiguration() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder('WsDocgenTest').build()
        def outputPath = 'build/gen/wsdoc'
        def sourcePath = 'src/main/java'

        //generer eksempel-kildekode
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeSimpleDemoServiceWSBean(sourcePath)
        }


        //setter opp testprosjekt
        projectHelper.configureProject {
            task('testDefaultConfiguration') {
                doLast {
                    ant.mkdir(dir: outputPath)

                    ant.apt(factory: no.statkart.grunnbok.tools.docgen.ws.WebserviceAnnotationProcessorFactory.class.name,
                            srcdir: sourcePath,
                            destdir: outputPath,
                            compile: false,
                            debug: true,
                            // classpath: runs in classpath of this test fixure. no need to spesify it here.
                    ) {
                        include(name: '**/*WSBean.java')
                    }
                }
            }
        } //end configure

        //utfører task
        projectHelper.executeTask('testDefaultConfiguration')


        //tester resultat
        projectHelper.assertFileExists(outputPath + '/TestService.html') { File file ->

            //leser inn html dokumentasjon som xml - dette steget validerer derfor html-koden
            GPathResult html = new XmlSlurper().parse(file)

            //sjekker innhold
            assert html.head.title.text().contains('TestService')

            //sjekker dokumenterte metoder
            assert html.body.div[0].ul.li.size() == 2 //forventet antall metoder
            assert html.body.div[0].ul.li[0].a.text() == 'noPing'  //forventet metodenavn
            assert html.body.div[0].ul.li[1].a.text() == 'ping'  //forventet metodenavn

            assert html.body.div[1].div.size() == 2 //forventet antall metoder
            html.body.div[1].div.each {
                assert it.p.text().trim() in ['Returnerer PONG', 'Returnerer ikke noe'] //forventet dokumentasjon
                assert it.h4.text().trim() in ['ping', 'noPing'] //forventet overskrift

            }
        }

    }

    /**
     * Tester angivelse av LookupPath
     */
    @Test
    void testLookupPath() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder('WsDocgenTest').applyJavaPlugin().build()
        def outputPath = 'build/gen/wsdoc'
        def sourcePath = 'src/main/java'

        //generer eksempel-kildekode
        use(WsDocgenTestutilFilewriter) {
            projectHelper.writeInterfaceServiceWSBean(sourcePath)
        }


        //setter opp testprosjekt
        projectHelper.configureProject {

                task('testLookupPath', dependsOn: 'build') {
                    doLast {
                        ant.mkdir(dir: outputPath)

                        ant.apt(factory: no.statkart.grunnbok.tools.docgen.ws.WebserviceAnnotationProcessorFactory.class.name,
                                srcdir: sourcePath,
                                destdir: outputPath,
                                compile: false,
                                debug: true,
                                // classpath: runs in classpath of this test fixure. no need to spesify it here.
                        ) {
                            option(name: 'LookupPath', value: '../javadoc/index.html')
                            include(name: '**/*WSBean.java')
                        }
                    }
                }
            } //end configure

        //utfører task
        projectHelper.executeTask('testLookupPath')


        //tester resultat
        projectHelper.assertFileExists(outputPath + '/InterfaceService.html') { File file ->

            //leser inn html dokumentasjon som xml - dette steget validerer derfor html-koden
            GPathResult html = new XmlSlurper().parse(file)

            //sjekker innhold

            //return verdi for ping() skal referere til SimpleClass - forventer at href for denne er blitt generert
            def href = html.body.div.div.find {
                it.h4.text() == 'ping'
            }.ul[1].li[0].a['@href']

            assert "${href}".startsWith('../javadoc/index.html?') //forventer at href er blitt satt

            assert href == '../javadoc/index.html?no/statkart/sktools/test/service/interfaceservice/domain/SimpleClass.html' //forventet javadoc url

        }

    }

    /**
     * Tester at css fil er med i distro
     */
    @Test
    void testAtCssFilErMed() {
        def fileContents = this.class.getClassLoader().getResourceAsStream('ws-style.css').text

        assert !fileContents.isEmpty()   //forventer at css stilsett finnes
    }


}