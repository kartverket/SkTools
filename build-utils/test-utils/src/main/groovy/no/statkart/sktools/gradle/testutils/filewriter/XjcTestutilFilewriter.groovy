package no.statkart.sktools.gradle.testutils.filewriter

/**
 * Statiske understøttende hjelpemetoder for generering av kildekode for bruk i testing.
 *
 * @author Leif Lislegård
 */
class XjcTestutilFilewriter {

    /**
     * Skriver enkelt schema til fil.
     * targetNamespace="http://sktools.statkart.no/test"
     */
    public static void writeSimpleSchema(File targetFilePath) {
        writeSimpleSchemaImpl(targetFilePath, [])
    }

    private static void writeSimpleSchemaImpl(File file, def args) {
        def extensionBindingPrefixes = []

        file.parentFile.mkdirs()
        file.withPrintWriter('UTF-8') { writer ->
            writer.print """<?xml version="1.0" encoding="UTF-8"?>
                <xs:schema
                        version="1.0"
                        elementFormDefault="qualified"
                        targetNamespace="http://sktools.statkart.no/test"
                        xmlns="http://sktools.statkart.no/test"
                        xmlns:xs="http://www.w3.org/2001/XMLSchema"
                        xmlns:jaxb="http://java.sun.com/xml/ns/jaxb"
                        jaxb:version="2.1"
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
                                <jaxb:class>
                                    <jaxb:javadoc><![CDATA[Dokumentasjon for type.
Multiline.]]></jaxb:javadoc>
                                </jaxb:class>
                            </xs:appinfo>
                        </xs:annotation>

                        <xs:complexContent>
                            <xs:extension base="SimpleType">
                                <xs:sequence>
                                    <xs:element name="documentedVar" type="xs:string">
                                        <xs:annotation>
                                            <xs:appinfo>
                                                <jaxb:property>
                                                    <jaxb:javadoc><![CDATA[Dokumentasjon for felt.]]></jaxb:javadoc>
                                                </jaxb:property>
                                            </xs:appinfo>
                                        </xs:annotation>
                                    </xs:element>
                                </xs:sequence>
                            </xs:extension>
                        </xs:complexContent>
                    </xs:complexType>

                    <xs:complexType name="StringList">
                        <xs:sequence>
                            <xs:element name="item" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
                        </xs:sequence>
                    </xs:complexType>

                    <xs:annotation>
                        <xs:appinfo>
                            <jaxb:schemaBindings>
                                <jaxb:package>
                                    <jaxb:javadoc><![CDATA[<body>Dokumentasjon av pakke.</body>]]></jaxb:javadoc>
                                </jaxb:package>
                            </jaxb:schemaBindings>
                        </xs:appinfo>
                    </xs:annotation>

                </xs:schema>
            """
    }
    }


}
