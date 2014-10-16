package no.statkart.sktools.gradle.testutils.xml

import org.xml.sax.InputSource
import org.xml.sax.SAXException

/**
 * Resolver DTD filer uten å laste ned disse ifra internett ({@code systemId})
 *
 * @since 1.3 - ny grunnbok sprint 30
 * @author Leif Lislegård
 */
class XmlTestUtils {

    public final static String XHTML_1_0_STRICT = "-//W3C//DTD XHTML 1.0 Strict//EN"
    public final static String XHTML_1_0_TRANSITIONAL = "-//W3C//DTD XHTML 1.0 Transitional//EN"
    public final static String XHTML_1_0_FRAMESET = "-//W3C//DTD XHTML 1.0 Frameset//EN"

    private static final Map DTD_MAP = [:]

    static {

        final String xhtml1_basePath = "xhtml1-20020801/DTD/";

        DTD_MAP.put(XHTML_1_0_STRICT, xhtml1_basePath + "xhtml1-strict.dtd")
        DTD_MAP.put(XHTML_1_0_TRANSITIONAL, xhtml1_basePath + "xhtml1-transitional.dtd")
        DTD_MAP.put(XHTML_1_0_FRAMESET, xhtml1_basePath + "xhtml1-frameset.dtd")

        DTD_MAP.put("-//W3C//ENTITIES Latin 1 for XHTML//EN", xhtml1_basePath + "xhtml-lat1.ent")
        DTD_MAP.put("-//W3C//ENTITIES Symbols for XHTML//EN", xhtml1_basePath + "xhtml-symbol.ent")
        DTD_MAP.put("-//W3C//ENTITIES Special for XHTML//EN", xhtml1_basePath + "xhtml-special.ent")
    }


    public static XmlSlurper defaultXmlSlurper() {
        return buildXmlSlurper(DTD_MAP);
    }

    public static XmlSlurper buildXmlSlurper(Map entityFiles) {
        XmlSlurper slurper = new XmlSlurper(true, true, true) //allowDocTypeDeclaration=true for html documents
        slurper.setEntityResolver(new TestEntityResolver(entityFiles))

        return slurper
    }
}

class TestEntityResolver implements org.xml.sax.EntityResolver {

    private final Map<String, String> catalogueMap;

    TestEntityResolver(Map catalogueMap) {
        this.catalogueMap = catalogueMap
    }

    @Override
    InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        if (catalogueMap.containsKey(publicId)) {
            String path = catalogueMap.get(publicId)
            final URL resource = this.getClass().getClassLoader().getResource(path)

            return new InputSource(resource.openStream());
        }
        return null;
    }
}
