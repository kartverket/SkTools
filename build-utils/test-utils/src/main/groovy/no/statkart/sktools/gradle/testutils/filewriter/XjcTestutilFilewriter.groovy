package no.statkart.sktools.gradle.testutils.filewriter

/**
 * Statiske understøttende hjelpemetoder for generering av kildekode for bruk i testing.
 *
 * @author Leif Lislegård
 */
class XjcTestutilFilewriter extends AbstractTestutilFilewriter {

    /**
     * Skriver enkelt schema til fil.
     * targetNamespace="http://sktools.statkart.no/test"
     */
    public static void writeSimpleSchema(File targetFilePath) {
        writeSimpleSchemaImpl(targetFilePath, [])
    }

    /**
     * Skriver enkelt schema til fil der gdoc prefikset er koblet inn slik at gdoc dokumentasjon er aktivert.
     * targetNamespace="http://sktools.statkart.no/test"
     */
    public static void writeSimpleSchemaWithGdoc(File targetFilePath) {
        writeSimpleSchemaImpl(targetFilePath, [gdoc: true])
    }


    private static void writeSimpleSchemaImpl(File file, def args) {
        def extensionBindingPrefixes = []
        if (args['gdoc']) { extensionBindingPrefixes += 'gdoc' }

        file.parentFile.mkdirs()
        file.withPrintWriter('ISO-8859-1') { writer ->
            writer.print """<?xml version="1.0" encoding="ISO-8859-1"?>
                <xs:schema
                        version="1.0"
                        elementFormDefault="qualified"
                        targetNamespace="http://sktools.statkart.no/test"
                        xmlns="http://sktools.statkart.no/test"
                        xmlns:xs="http://www.w3.org/2001/XMLSchema"
                        xmlns:jaxb="http://java.sun.com/xml/ns/jaxb"
                        jaxb:version="2.1"

                        xmlns:gdoc="http://grunnbok.statkart.no/tools/gdoc"
                        """
            if (!extensionBindingPrefixes.empty) {
                writer.print('jaxb:extensionBindingPrefixes="' + extensionBindingPrefixes.join(',') + '"')
            }
            writer.print """>

                    <xs:complexType name="SimpleType">
                        <xs:sequence>
                            <xs:element name="var1" type="xs:string"/>
                            <xs:element name="var2" type="xs:string"/>
                        </xs:sequence>
                    </xs:complexType>

                    <xs:complexType name="DocumentedSimpleType">
                        <xs:annotation>
                            <xs:appinfo>
                                <gdoc:doc><![CDATA[ Ekstra dokumentasjon for typen.
Merk at denne er multiline og definert som CDATA element.
                                    ]]>
                                </gdoc:doc>
                            </xs:appinfo>
                        </xs:annotation>

                        <xs:complexContent>
                            <xs:extension base="SimpleType">
                                <xs:sequence>
                                    <xs:element name="documentedVar" type="xs:string"/>
                                </xs:sequence>
                            </xs:extension>
                        </xs:complexContent>
                    </xs:complexType>

                    <xs:complexType name="StringList">
                        <xs:sequence>
                            <xs:element name="item" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
                        </xs:sequence>
                    </xs:complexType>

                </xs:schema>
            """
    }
    }


}
