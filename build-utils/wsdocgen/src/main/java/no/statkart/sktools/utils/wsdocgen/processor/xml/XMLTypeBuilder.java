package no.statkart.sktools.utils.wsdocgen.processor.xml;

import org.w3c.dom.Document;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import no.statkart.sktools.utils.wsdocgen.processor.util.*;

import static javax.lang.model.type.TypeKind.DECLARED;
import static javax.lang.model.type.TypeKind.ERROR;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

/**
 * @author Leif Lislegård
 * @since 1.3
 */
class XMLTypeBuilder {
    private static transient HashMap<TypeMirror, QName> typeCache = null;

    private final XMLBuilderFactory factory;
    private final ProcessingEnvironment processingEnv;
    private final org.w3c.dom.Document document;


    XMLTypeBuilder(XMLBuilderFactory factory) {
        this.factory = factory;
        this.processingEnv = factory.getProcessingEnv();
        this.document = factory.getDocument();

        if (typeCache == null) {
            initializeCommponTypes();
        }
    }


    public org.w3c.dom.Element appendTypeTo(org.w3c.dom.Node parent, Element element) {
        org.w3c.dom.Element typeNode = buildType(element);
        parent.appendChild(typeNode);
        return typeNode;
    }

    public org.w3c.dom.Element buildType(Element element) {
        if (ERROR.equals(element.getKind())) {
            return null;
        }
        if (element instanceof VariableElement || element instanceof TypeElement) {
            final String name = WSUtils.findName(element, true);
            final String ns = findObjectNamespace(element);
            String docComment = processingEnv.getElementUtils().getDocComment(element);
            final JavaDocUtils javaDocUtils = JavaDocUtils.parse(docComment);
            return buildTypeImpl(document, name, ns, javaDocUtils);
        } else {
            throw new RuntimeException(String.format("Unhandled element type: %s", element.getSimpleName()));
        }
    }


    public org.w3c.dom.Element appendTypeTo(org.w3c.dom.Node parent, TypeMirror typeMirror) {
        org.w3c.dom.Element typeNode = buildType(typeMirror);
        parent.appendChild(typeNode);
        return typeNode;
    }

    public org.w3c.dom.Element buildType(TypeMirror typeMirror) {
        for (Map.Entry<TypeMirror, QName> entry : typeCache.entrySet()) {
            if (entry.getKey().equals(typeMirror)) {
                final String name = entry.getValue().getLocalPart();
                final String ns = entry.getValue().getNamespaceURI();
                return buildTypeImpl(document, name, ns, null);
            }
        }

        switch (typeMirror.getKind()) {
            case DECLARED: {
                DeclaredType declaredType = (DeclaredType) typeMirror;
                final Element declaredElement = declaredType.asElement();

                final String name = declaredElement.getSimpleName().toString();
                final String ns = findObjectNamespace(typeMirror);
                return buildTypeImpl(document, name, ns, null);
            }
            case VOID: {
                throw new IllegalArgumentException("Void type not allowed here!");
            }
            case ERROR: {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, String.format("No sources found. Unknown type: %s", typeMirror));
            }
            default: {
                return buildUnknownType(document, typeMirror);
            }
        }

    }

    private org.w3c.dom.Element buildUnknownType(Document document, TypeMirror typeMirror) {
        return buildTypeImpl(document, typeMirror.toString(), findObjectNamespace(typeMirror), null);
    }


    private org.w3c.dom.Element buildTypeImpl(Document document, String name, String ns, JavaDocUtils javaDocUtils) {
        org.w3c.dom.Element type = document.createElement("type");
        type.setAttribute("name", name);
        type.setAttribute("namespace", ns);
        type.setAttribute("javadocPath", buildJavadocPath(processingEnv.getOptions().get("javaDocLookupPath"), ns, name));
        type.appendChild(factory.getDescriptionBuilder().buildDescription(javaDocUtils));
        return type;
    }


    public static String buildJavadocPath(String basePath, String ns, String clazz) {
        if (ns == null || "".equals(ns)) {
            return "";
        }
        //String javadocPath = basePath == null ? "VALUE_NOT_PARAMETRIZED" : basePath;
        //NT 17.01.2014, tom streng vil trigge relativ url sti i browseren.
        String javadocPath = (basePath == null) ? "" : basePath;
        String remainingUrlPath = buildJavadocPath(ns, clazz);
        final StringBuilder buffer = new StringBuilder(javadocPath);
        if (remainingUrlPath != null && remainingUrlPath.trim().length() > 0)
            buffer.append("?").append(remainingUrlPath);
        //javadocPath += '?' + remainingUrlPath;
        return buffer.toString();
    }

    /*
     getJavadocURL("http://grunnbok.statkart.no/borett/info/wsapi/exception", "ServiceException")

     => "no/statkart/grunnbok/borett/info/wsapi/exception/ServiceException.html"
     **/
    public static String buildJavadocPath(String ns, String clazz) {
        if (ns != null) {
            try {
                URL url = new URL(ns);
                String host = url.getHost();
                String path = url.getPath();

                StringBuilder builder = new StringBuilder();
                for (String str : host.split("\\.")) {
                    builder.insert(0, str + "/");
                }
                builder.append(path);
                builder.append("/");
                builder.append(clazz);
                builder.append(".html");

                return builder.toString().replace("//", "/");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        } else {
            return "";
        }
    }


    /**
     * Forsøker å finne namespace til objekt ved å se i package-info
     * <p/>
     * For kjente typer returneres namespace for disse.
     */
    private String findObjectNamespace(Element element) {
        return findObjectNamespace(element.asType());
    }

    /**
     * Forsøker å finne namespace til objekt ved å se i package-info
     * <p/>
     * For kjente typer returneres namespace for disse.
     */
    private String findObjectNamespace(TypeMirror typeMirror) {
        String objectNS = "";
        if (typeMirror != null) {
            for (Map.Entry<TypeMirror, QName> entry : typeCache.entrySet()) {
                if (entry.getKey().equals(typeMirror)) {
                    return entry.getValue().getNamespaceURI();
                }
            }
            XmlType xmlTypeAnnotation = typeMirror.getClass().getAnnotation(XmlType.class);
            if (xmlTypeAnnotation != null) {
                if (!"##default".equals(xmlTypeAnnotation.namespace())) {
                    return xmlTypeAnnotation.namespace();
                }
            }
            XmlSchema xmlSchemaAnnotation = typeMirror.getClass().getPackage().getAnnotation(XmlSchema.class);
            if (xmlSchemaAnnotation != null) {
                if (!"".equals(xmlSchemaAnnotation.namespace())) {
                    return xmlSchemaAnnotation.namespace();
                }
            }
        }

        if (objectNS.isEmpty()) {
            if (ERROR.equals(typeMirror.getKind())) {
                System.out.println(String.format("WARNING: no namespace found for %s", typeMirror));
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, String.format("WARNING: no namespace found for %s", typeMirror));
                return "";
            }
        }
        return objectNS;
    }

    private void initializeCommponTypes() {
        typeCache = new HashMap<TypeMirror, QName>();
        defineCachedType(String.class,                 new QName(W3C_XML_SCHEMA_NS_URI, "string"));
        defineCachedType(Character.class,              new QName(W3C_XML_SCHEMA_NS_URI, "string"));
        defineCachedType(char.class,                   new QName(W3C_XML_SCHEMA_NS_URI, "string"));
        defineCachedType(java.math.BigInteger.class,   new QName(W3C_XML_SCHEMA_NS_URI, "integer"));
        defineCachedType(int.class,                    new QName(W3C_XML_SCHEMA_NS_URI, "int"));
        defineCachedType(Integer.class,                new QName(W3C_XML_SCHEMA_NS_URI, "int"));
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
        defineCachedType(QName.class,                  new QName(W3C_XML_SCHEMA_NS_URI, "QName"));
        defineCachedType(java.util.Calendar.class,     new QName(W3C_XML_SCHEMA_NS_URI, "dateTime"));
        defineCachedType(XMLGregorianCalendar.class,   new QName(W3C_XML_SCHEMA_NS_URI, "dateTime"));
        defineCachedType(byte[].class,                 new QName(W3C_XML_SCHEMA_NS_URI, "base64Binary"));
    }

    private void defineCachedType(Class clazz, QName qName) {
        typeCache.put(elementFor(clazz), qName);
    }

    private TypeMirror elementFor(Class clazz) {
        if (clazz.isPrimitive()) {
            TypeKind typeKind = null;
            if (clazz == boolean.class) {
                typeKind = TypeKind.BOOLEAN;
            } else if (clazz == byte.class) {
                typeKind = TypeKind.BYTE;
            } else if (clazz == char.class) {
                typeKind = TypeKind.CHAR;
            } else if (clazz == double.class) {
                typeKind = TypeKind.DOUBLE;
            } else if (clazz == float.class) {
                typeKind = TypeKind.FLOAT;
            } else if (clazz == int.class) {
                typeKind = TypeKind.INT;
            } else if (clazz == long.class) {
                typeKind = TypeKind.LONG;
            } else if (clazz == short.class) {
                typeKind = TypeKind.SHORT;
            }

            if (typeKind != null) {
                PrimitiveType primitiveType = processingEnv.getTypeUtils().getPrimitiveType(typeKind);
                return primitiveType;
            }
        } else if (clazz.isArray()) {
            TypeMirror componentType = elementFor(clazz.getComponentType());
            return processingEnv.getTypeUtils().getArrayType(componentType);
        } else {
            final TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(clazz.getCanonicalName());
            return typeElement.asType();
        }

        throw new RuntimeException("Unhandled primitive type!");
    }


}
