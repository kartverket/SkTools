package com.sun.tools.xjc.addon.statkart

import org.testng.annotations.Test

import org.gradle.api.logging.LogLevel
import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.filewriter.XjcTestutilFilewriter

/**
 * Test av {@link GrunnbokDocPlugin}
 *
 * @author Leif Lislegård
 */
class GrunnbokDocPluginTest {


    /**
     * Tester default oppsett.
     *
     * Eksekverer plugin via ant task i gradle.
     */
    @Test
    void testGrunnbokDocPlugin() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder('ListgenTest').build()
        def outputPath = 'build/gen/java'
        def sourcePath = 'src/main/schema'


        //generer eksempel-kildekode
        use(XjcTestutilFilewriter) {
            projectHelper.writeSimpleSchemaWithGdoc(sourcePath + "/base.xsd")
        }


        //setter opp test-prosjekt
        projectHelper.configureProject {

                ant.taskdef(name: 'xjc', classname: 'com.sun.tools.xjc.XJCTask') //classpath: runs in classpath of this test fixture. no need to spesify it here.

                //oppretter gradle task
                task('testGrunnbokDocPlugin') {
                    logging.captureStandardOutput LogLevel.ERROR
                    logging.captureStandardError LogLevel.ERROR

                    doLast {
                        //oppretter mapper for gen kode + ant defgen
                        ant.mkdir(dir: outputPath)

                        //xjc ant task
                        ant.xjc(destDir: outputPath, extension: true) {
                            schema(dir: sourcePath, includes: '**/*.xsd')
                            arg(line: "-grunnbokDoc no.statkart.sktools.test no.statkart.sktools.annen.pakke")
                        }
                    }
                }

            } //end configure


        //utfører xjc task ihht til konfigurasjon
        projectHelper.executeTask('testGrunnbokDocPlugin')


        //tester
        projectHelper.assertFileExists(outputPath + '/no/statkart/sktools/test/SimpleType.java') { File file ->

            //sjekker at @see tag er blitt med
            assert file.text ==~ /(?ms).*@see no\.statkart\.sktools\.annen\.pakke\.SimpleType.*/ //(?ms) matches regex over multiple lines.
        }


        projectHelper.assertFileExists(outputPath + '/no/statkart/sktools/test/DocumentedSimpleType.java') { File file ->

            //sjekker at dokumentasjon av klasse er blitt med
            assert file.text ==~ /(?ms).*Ekstra dokumentasjon for typen\.([\n\s\*]+)Merk at denne er multiline.*/  //(?ms) matches regex over multiple lines.
        }

    }



}