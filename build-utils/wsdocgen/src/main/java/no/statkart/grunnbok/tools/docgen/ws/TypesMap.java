package no.statkart.grunnbok.tools.docgen.ws;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Leif Lislegård
 * @since 0.3
 */
public class TypesMap {

    public static final String XSI_SCHEMA_URL = "http://www.w3.org/2001/XMLSchema";
    public static final Map<Class, QName> TYPES;

    static {
        HashMap<Class, QName> commonTypes = new HashMap<Class, QName>();
        commonTypes.put(String.class,                 new QName(XSI_SCHEMA_URL, "string"));
        commonTypes.put(java.math.BigInteger.class,   new QName(XSI_SCHEMA_URL, "integer"));
        commonTypes.put(int.class,                    new QName(XSI_SCHEMA_URL, "int"));
        commonTypes.put(Integer.class,                    new QName(XSI_SCHEMA_URL, "int"));
        commonTypes.put(long.class,                   new QName(XSI_SCHEMA_URL, "long"));
        commonTypes.put(Long.class,                   new QName(XSI_SCHEMA_URL, "long"));
        commonTypes.put(short.class,                  new QName(XSI_SCHEMA_URL, "short"));
        commonTypes.put(Short.class,                  new QName(XSI_SCHEMA_URL, "short"));
        commonTypes.put(java.math.BigDecimal.class,   new QName(XSI_SCHEMA_URL, "decimal"));
        commonTypes.put(float.class,                  new QName(XSI_SCHEMA_URL, "float"));
        commonTypes.put(Float.class,                  new QName(XSI_SCHEMA_URL, "float"));
        commonTypes.put(double.class,                 new QName(XSI_SCHEMA_URL, "double"));
        commonTypes.put(Double.class,                 new QName(XSI_SCHEMA_URL, "double"));
        commonTypes.put(boolean.class,                new QName(XSI_SCHEMA_URL, "boolean"));
        commonTypes.put(Boolean.class,                new QName(XSI_SCHEMA_URL, "boolean"));
        commonTypes.put(byte.class,                   new QName(XSI_SCHEMA_URL, "byte"));
        commonTypes.put(QName.class, new QName(XSI_SCHEMA_URL, "QName"));
        commonTypes.put(java.util.Calendar.class, new QName(XSI_SCHEMA_URL, "dateTime"));

        TYPES = Collections.unmodifiableMap(commonTypes);
    }

}
