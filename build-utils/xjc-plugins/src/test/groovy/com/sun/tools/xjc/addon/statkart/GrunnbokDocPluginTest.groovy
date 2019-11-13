package com.sun.tools.xjc.addon.statkart


import no.statkart.sktools.gradle.testutils.TestKitBase
import org.testng.annotations.Test

import static no.statkart.sktools.gradle.testutils.filewriter.XjcTestutilFilewriter.writeSimpleSchemaWithGdoc
import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.contentOf

/**
 * Test av {@link GrunnbokDocPlugin}
 *
 * @author Leif Lislegård
 */
class GrunnbokDocPluginTest extends TestKitBase {


    /**
     * Tester default oppsett.
     *
     * Eksekverer plugin via ant task i gradle.
     */
    @Test
    void testGrunnbokDocPlugin() {
        File schemaFile = file("schema/base.xsd")
        File destDir = file('gen')

        //eksempel-kildekode
        writeSimpleSchemaWithGdoc(schemaFile)

        def xjc = new com.sun.tools.xjc.XJC2Task()
        xjc.setDestdir(destDir)
        xjc.setExtension(true)
        xjc.setSchema(schemaFile.toURI().toString())
        xjc.createArg().setLine("-grunnbokDoc no.statkart.sktools.test no.statkart.sktools.annen.pakke")

        destDir.mkdirs()

        //utfører xjc task ihht til konfigurasjon
        xjc.execute()


        //PS: (?ms) matches regex over multiple lines.
        //tester
        assertThat(contentOf(file('gen/no/statkart/sktools/test/SimpleType.java')))
            .matches(/(?ms).*@see no\.statkart\.sktools\.annen\.pakke\.SimpleType.*/)

        assertThat(contentOf(file('gen/no/statkart/sktools/test/DocumentedSimpleType.java')))
            .matches(/(?ms).*Ekstra dokumentasjon for typen\.([\n\s\*]+)Merk at denne er multiline.*/)
    }



}