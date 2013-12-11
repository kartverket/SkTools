package no.statkart.sktools.utils.wsdocgen.processor.xml;

import org.w3c.dom.Document;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static no.statkart.sktools.utils.wsdocgen.processor.util.WSUtils.*;

/**
 * @author Leif Lislegård
 * @since 1.3
 */
class XMLTypeBuilder {
    private static transient HashMap<Element, QName> typeCache = null;

    private final ProcessingEnvironment processingEnv;
    private final org.w3c.dom.Document document;
    



    public XMLTypeBuilder(Document document, ProcessingEnvironment processingEnv) {
        this.document = document;
        this.processingEnv = processingEnv;
        if (typeCache == null) {
            initializeCommponTypes();
        }
    }


    public org.w3c.dom.Node buildType(Element element) {
        if (element instanceof VariableElement || element instanceof TypeElement) {
            final String name = findName(element);
            final String ns = findObjectNamespace(element);
            return buildTypeImpl(document, name, ns);
        } else {
            throw new RuntimeException(String.format("Unhandled element type: %s", element.getSimpleName()));
        }
    }

    public org.w3c.dom.Node buildType(TypeMirror typeMirror) {
        for (Map.Entry<Element, QName> entry : typeCache.entrySet()) {
            if (entry.getKey().asType().equals(typeMirror)) {
                final String name = entry.getValue().getLocalPart();
                final String ns = entry.getValue().getNamespaceURI();
                return buildTypeImpl(document, name, ns);
            }
        }

        if (typeMirror.getKind().equals(TypeKind.DECLARED)) {
            DeclaredType declaredType = (DeclaredType) typeMirror;
            final Element declaredElement = declaredType.asElement();

            final String name = declaredElement.getSimpleName().toString();
            final String ns = findObjectNamespace(declaredType);
            return buildTypeImpl(document, name, ns);
        }

        throw new RuntimeException(String.format("Unhandled type: %s", typeMirror));
    }



    private org.w3c.dom.Node buildTypeImpl(Document document, String name, String ns) {
        org.w3c.dom.Element type = document.createElement("type");
        type.setAttribute("name", name);
        type.setAttribute("namespace", ns);
        type.setAttribute("javadocPath", buildJavadocPath(ns, name));
        return type;
    }


    /**
     * Forsøker å finne namespace til objekt ved å se i package-info
     * <p/>
     * For kjente typer returneres namespace for disse.
     */
    private String findObjectNamespace(Element element) {
        if (typeCache.containsKey(element)) {
            return typeCache.get(element).getNamespaceURI();
        }

        return findObjectNamespace(element.asType());
    }

    /**
     * Forsøker å finne namespace til objekt ved å se i package-info
     * <p/>
     * For kjente typer returneres namespace for disse.
     */
    private String findObjectNamespace(TypeMirror typeMirror) {
        String objectNS = null;
        if (typeMirror != null) {
            for (Map.Entry<Element, QName> entry : typeCache.entrySet()) {
                if (entry.getKey().asType().equals(typeMirror)) {
                    return entry.getValue().getNamespaceURI();
                }
            }
            XmlSchema xmlSchemaAnnotation = typeMirror.getClass().getPackage().getAnnotation(XmlSchema.class);
            if (xmlSchemaAnnotation != null) {
                objectNS = xmlSchemaAnnotation.namespace();
            }
        }
        if (objectNS == null || objectNS.isEmpty()) {
            System.err.println(String.format("WARNING: no namespace defined for %s", typeMirror));
        }
        return objectNS;
    }

    private void initializeCommponTypes() {
        typeCache = new HashMap<Element, QName>();
        defineCachedType(String.class,                 new QName(W3C_XML_SCHEMA_NS_URI, "string"));
        defineCachedType(java.math.BigInteger.class,   new QName(W3C_XML_SCHEMA_NS_URI, "integer"));
        defineCachedType(int.class,                    new QName(W3C_XML_SCHEMA_NS_URI, "int"));
        defineCachedType(Integer.class,                    new QName(W3C_XML_SCHEMA_NS_URI, "int"));
        defineCachedType(long.class,                   new QName(W3C_XML_SCHEMA_NS_URI, "long"));
        defineCachedType(Long.class,                   new QName(W3C_XML_SCHEMA_NS_URI, "long"));
        defineCachedType(short.class,                  new QName(W3C_XML_SCHEMA_NS_URI, "short"));
        defineCachedType(Short.class,                  new QName(W3C_XML_SCHEMA_NS_URI, "short"));
        defineCachedType(java.math.BigDecimal.class,   new QName(W3C_XML_SCHEMA_NS_URI, "decimal"));
        defineCachedType(float.class,                  new QName(W3C_XML_SCHEMA_NS_URI, "float"));
        defineCachedType(Float.class,                  new QName(W3C_XML_SCHEMA_NS_URI, "float"));
        defineCachedType(double.class,                 new QName(W3C_XML_SCHEMA_NS_URI, "double"));
        defineCachedType(Double.class,                 new QName(W3C_XML_SCHEMA_NS_URI, "double"));
        defineCachedType(boolean.class,                new QName(W3C_XML_SCHEMA_NS_URI, "boolean"));
        defineCachedType(Boolean.class,                new QName(W3C_XML_SCHEMA_NS_URI, "boolean"));
        defineCachedType(Byte.class,                   new QName(W3C_XML_SCHEMA_NS_URI, "byte"));
        defineCachedType(byte.class,                   new QName(W3C_XML_SCHEMA_NS_URI, "byte"));
        defineCachedType(QName.class, new QName(W3C_XML_SCHEMA_NS_URI, "QName"));
        defineCachedType(java.util.Calendar.class, new QName(W3C_XML_SCHEMA_NS_URI, "dateTime"));
        defineCachedType(XMLGregorianCalendar.class, new QName(W3C_XML_SCHEMA_NS_URI, "dateTime"));
        
    }

    private void defineCachedType(Class clazz, QName qName) {
        if (!clazz.isPrimitive()) {
            typeCache.put(elementFor(clazz), qName);
        }
    }

    private TypeElement elementFor(Class clazz) {
        return processingEnv.getElementUtils().getTypeElement(clazz.getCanonicalName());
    }


}
