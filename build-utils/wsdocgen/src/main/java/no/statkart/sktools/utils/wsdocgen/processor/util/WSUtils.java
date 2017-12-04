package no.statkart.sktools.utils.wsdocgen.processor.util;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

/**
 * Helper methods for JAX-WS implementations.
 *
 * @author Leif Lislegård
 * @since 1.3 - ny grunnbok sprint 30
 */
public class WSUtils {

    public static String findWebServicePortTypeName(Element element) {
        String name = null;

        WebService webService = element.getAnnotation(WebService.class);
        if (webService != null) {
            name = webService.name();
        }

        return name;
    }

    public static String findWebServiceName(Element element) {
        String name = null;

        WebService webService = element.getAnnotation(WebService.class);
        if (webService != null) {
            name = webService.serviceName();
        }

        return name;
    }

    public static String findTargetNamespace(Element element) {
        String namespace = null;

        WebService webService = element.getAnnotation(WebService.class);
        if (webService != null) {
            namespace = webService.targetNamespace();
        }

        return namespace;
    }

    /**
     * @return navn, eller {@code null} dersom ikke navn funnet og {@code usingWebMethodAnnotation==false}
     */
    public static String findMethodName(ExecutableElement executableElement, boolean usingWebMethodAnnotation) {
        String name = null;
        if (usingWebMethodAnnotation) {
            WebMethod annotation = executableElement.getAnnotation(WebMethod.class);
            if (annotation != null && !annotation.exclude()) {
                name = annotation.operationName();
            }

        }
        if (name == null || name.isEmpty()) {
            name = executableElement.getSimpleName().toString();
        }
        return name;
    }

    public static String nameForReturn(Element element) {
       return nameFor(element, "return"); //weblogic defaulter til return?
    }
    public static String nameForParameter(Element element) {
        String defaultName = element.getSimpleName().toString();
        return nameFor(element, defaultName);
    }
    public static String nameForException(Element element) {
        return element.getSimpleName().toString();
    }

    public static String nameFor(Element element, String defaultName) {
        String name = null;

        WebParam webParam = element.getAnnotation(WebParam.class);
        if (webParam != null) {
            name = webParam.name();
        }

        WebResult webResult = element.getAnnotation(WebResult.class);
        if (webResult != null) {
            name = webResult.name();
        }

        if (name == null || name.isEmpty()) {
            name = defaultName;
        }

        return name;
    }


}
