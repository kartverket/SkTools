package com.sun.tools.xjc.addon.statkart

import no.statkart.sktools.gradle.testutils.TestKitBase
import org.testng.annotations.Test

import static no.statkart.sktools.gradle.testutils.filewriter.XjcTestutilFilewriter.writeSimpleSchema
import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.contentOf

/**
 * Test av {@link ListGenPlugin}
 *
 * @author Leif Lislegård
 */
class ListGenPluginTest extends TestKitBase {


    /**
     * Tester default oppsett.
     *
     * Eksekverer plugin via ant task i gradle.
     */
    @Test
    void testListgenPlugin() {
        File schemaFile = file("schema/base.xsd")
        File destDir = file('gen')

        //eksempel-kildekode
        writeSimpleSchema(schemaFile)

        def xjc = new com.sun.tools.xjc.XJC2Task()
        xjc.setDestdir(destDir)
        xjc.setExtension(true)
        xjc.setSchema(schemaFile.toURI().toString())
        xjc.createArg().setLine("-listgen")

        destDir.mkdirs()

        //eksekverer task
        xjc.execute()


        //PS: (?ms) matches regex over multiple lines.
        //tester
        assertThat(contentOf(file('gen//no/statkart/sktools/test/StringList.java')))
            .as("import statement er blitt med")
            .matches(/(?ms).*import\s+no\.statkart\.grunnbok\.skif\.util\.ListIterable;.*/)
            .as("extender ListItarable")
            .matches(/(?ms).*StringList[\s\n]+ extends ListIterable.*/)
            .as("interface metoder er lagt til")
            .matches(/(?ms).*public\s+java\.util\.List<String>\s+_getList\(\)\s+\{.*/)
    }



    /**
     * Tester anngivelse av egen implementasjon av baseClass
     *
     * Eksekverer plugin via ant task i gradle.
     */
    @Test
    void testListgenPluginWithBaseClass() {
        File schemaFile = file("schema/base.xsd")
        File destDir = file('gen')

        //eksempel-kildekode
        writeSimpleSchema(schemaFile)

        def xjc = new com.sun.tools.xjc.XJC2Task()
        xjc.setDestdir(destDir)
        xjc.setExtension(true)
        xjc.setSchema(schemaFile.toURI().toString())
        xjc.createArg().setLine("-listgen baseClass=some.implementation.ListTestIterable")

        destDir.mkdirs()

        //eksekverer task
        xjc.execute()


        //PS: (?ms) matches regex over multiple lines.
        //tester
        assertThat(contentOf(file('gen//no/statkart/sktools/test/StringList.java')))
            .as("import statement er blitt med")
            .matches(/(?ms).*import\s+some\.implementation\.ListTestIterable;.*/)
            .as("extender ListTestIterable")
            .matches(/(?ms).*StringList[\s\n]+ extends ListTestIterable.*/)
            .as("interface metoder er lagt til")
            .matches(/(?ms).*public\s+java\.util\.List<String>\s+_getList\(\)\s+\{.*/)
    }



}