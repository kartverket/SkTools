package no.statkart.sktools.gradle.testutils.filewriter

import no.statkart.sktools.gradle.testutils.ProjectHelper

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
    public static Collection<File> writeSimpleSchema(ProjectHelper projectHelper, String targetFilePath) {
        return writeSimpleSchema(projectHelper, targetFilePath, [])
    }

    /**
     * Skriver enkelt schema til fil der gdoc prefikset er koblet inn slik at gdoc dokumentasjon er aktivert.
     * targetNamespace="http://sktools.statkart.no/test"
     */
    public static Collection<File> writeSimpleSchemaWithGdoc(ProjectHelper projectHelper, String targetFilePath) {
        return writeSimpleSchema(projectHelper, targetFilePath, [gdoc: true])
    }

    /**
     * Genererer kildekode for ListTestIterable.java
     */
    public static Collection<File> writeListTestIterableJava(ProjectHelper projectHelper, String targetPath) {
        ArrayList<File> generatedFiles = new ArrayList<File>()

        generatedFiles.add projectHelper.project.file(targetPath+'/ListTestIterable.java').with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
                writer.print """
                    import java.util.Iterator;
                    import javax.xml.bind.annotation.XmlTransient;


                    @XmlTransient
                    public abstract class ListTestIterable  implements Iterable  {

                        abstract public java.util.List  _getList();


                        public Iterator  iterator() {
                            return _getList().iterator();
                        }

                    }
                """
            }
            return file
        }

        return generatedFiles
    }



    private static Collection<File> writeSimpleSchema(ProjectHelper projectHelper, String targetFilePath, def args) {
        ArrayList<File> generatedFiles = new ArrayList<File>()

        def extensionBindingPrefixes = []
        if (args['gdoc']) { extensionBindingPrefixes += 'gdoc' }

        generatedFiles.add projectHelper.project.file(targetFilePath).with { File file ->
            file.parentFile.mkdirs()
            file.withPrintWriter { writer ->
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
            return file
        }

        return generatedFiles
    }


}
