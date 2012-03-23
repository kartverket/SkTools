package com.sun.tools.xjc.addon.statkart

import org.testng.annotations.Test
import org.gradle.api.logging.LogLevel
import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.filewriter.XjcTestutilFilewriter

/**
 * Test av {@link ListGenPlugin}
 *
 * @author Leif Lislegård
 */
class ListGenPluginTest {


    /**
     * Tester default oppsett.
     *
     * Eksekverer plugin via ant task i gradle.
     */
    @Test
    void testListgenPlugin() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder('ListgenTest').build()
        def sourcePath = 'src/main/schema'
        def outputPath = 'build/gen/java'


        //generer eksempel-kildekode
        use(XjcTestutilFilewriter) {
            projectHelper.writeSimpleSchema(sourcePath + "/base.xsd")
        }

        //setter opp test-prosjekt
        projectHelper.configureProject {
            ant.taskdef(name: 'xjc', classname: 'com.sun.tools.xjc.XJCTask') //classpath: runs in classpath of this test fixure. no need to spesify it here.

                task('testListgenPlugin') {
                    logging.captureStandardError LogLevel.ERROR

                    doLast {
                        //oppretter mapper for gen kode + ant defgen
                        ant.mkdir(dir: outputPath)

                        //kjører xjc task
                        ant.xjc(destDir: outputPath, extension: true) {
                            schema(dir: sourcePath, includes: '**/*.xsd')
                            arg(line: '-listgen')
                        }
                    }
                }
            }

        //eksekverer task
        projectHelper.executeTask('testListgenPlugin')


        //tester
        projectHelper.assertFileExists(outputPath + '/no/statkart/sktools/test/StringList.java') { File file ->

            //sjekker at import statement er blitt med
            assert file.text ==~ /(?ms).*import\s+no\.statkart\.grunnbok\.skif\.util\.ListIterable;.*/ //(?ms) matches regex over multiple lines.

            //sjekker at klassen extender ListItarable
            assert file.text ==~ /(?ms).*StringList[\s\n]+ extends ListIterable.*/ //(?ms) matches regex over multiple lines.

            //sjekker at interface metoder er lagt til
            assert file.text ==~ /(?ms).*public\s+java\.util\.List<String>\s+_getList\(\)\s+\{.*/ //(?ms) matches regex over multiple lines.

        }

    }



    /**
     * Tester anngivelse av egen implementasjon av baseClass
     *
     * Eksekverer plugin via ant task i gradle.
     */
    @Test
    void testListgenPluginWithBaseClass() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder('ListgenTest').build()
        def outputPath = 'build/gen/java'
        def sourcePath = 'src/main/schema'

        //generer eksempel-kildekode
        use(XjcTestutilFilewriter) {
            projectHelper.writeSimpleSchema(sourcePath + "/base.xsd")
        }

        //setter opp test-prosjekt
        projectHelper.configureProject {
                ant.taskdef(name: 'xjc', classname: 'com.sun.tools.xjc.XJCTask') //classpath: runs in classpath of this test fixure. no need to spesify it here.

                task('testListgenPluginWithBaseClass') {
                    logging.captureStandardError LogLevel.ERROR

                    doLast {
                        //oppretter mapper for gen kode + ant defgen
                        ant.mkdir(dir: outputPath)

                        //kjører xjc task
                        ant.xjc(destDir: outputPath, extension: true) {
                            schema(dir: sourcePath, includes: '**/*.xsd')
                            arg(line: '-listgen baseClass=some.implementation.ListTestIterable')
                        }
                    }
                }

        }


        //eksekverer task
        projectHelper.executeTask('testListgenPluginWithBaseClass')


        //tester
        projectHelper.assertFileExists(outputPath + '/no/statkart/sktools/test/StringList.java') { File file ->

            //sjekker at import statement er blitt med
            assert file.text ==~ /(?ms).*import\s+some\.implementation\.ListTestIterable;.*/ //(?ms) matches regex over multiple lines.

            //sjekker at klassen extender ListTestIterable
            assert file.text ==~ /(?ms).*StringList[\s\n]+ extends ListTestIterable.*/ //(?ms) matches regex over multiple lines.

            //sjekker at interface metoder er lagt til
            assert file.text ==~ /(?ms).*public\s+java\.util\.List<String>\s+_getList\(\)\s+\{.*/ //(?ms) matches regex over multiple lines.
        }

    }



}