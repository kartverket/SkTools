package no.statkart.sktools.utils.wsdocgen.processor.util;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.net.MalformedURLException;
import java.net.URL;

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

    public static String findName(ExecutableElement executableElement, boolean usingWebMethodAnnotation) {
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

    public static String findName(Element element) {
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
            name = element.getSimpleName().toString();
        }
        return name;
    }

    /**
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
            return null;
        }
    }
}
