package no.statkart.grunnbok.tools.docgen.ws;

import com.sun.mirror.apt.Filer;
import com.sun.mirror.declaration.*;
import com.sun.mirror.type.*;
import com.sun.mirror.util.SimpleDeclarationVisitor;
import com.sun.mirror.util.TypeVisitor;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.namespace.QName;
import javax.xml.ws.WebFault;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Leif Lislegård
 * @since 0.3
 */
public class PrintWebServiceDocumentationVisitor extends SimpleDeclarationVisitor {

    static final Pattern returnPattern = Pattern.compile("^\\s*@return\\s+(.*)");
    static final Pattern inheritDocPattern = Pattern.compile("^\\s*@inheritDoc\\s*");
    static final Pattern paramPattern = Pattern.compile("^\\s*@param\\s*([^\\s]*)\\s+(.*)");
    static final Pattern throwsPattern = Pattern.compile("^\\s*@throws\\s*([^\\s]*)\\s+(.*)");


    private Filer filer;

    private PrintWriter out;
    private Map<String, List<MethodDeclaration>> eksponerteMetoder;

    /**
     * Plassering av javadoc for linking av dokumentasjon.
     * <p>
     *     Eksempel verdi kan være {@code "../index.html"} eller til en annen path som sammenfaller
     *     med en evt konfigurasjon av en webmodul (web.xml)
     * <p>
     * <p>
     * Fungerer med javadoc generert av java versjoner 1.5 eller nyere.
     */
    private String lookupPath = null;

    public PrintWebServiceDocumentationVisitor(Filer filer) {
        this.filer = filer;
    }

    public void setLookupPath(String lookupPath) {
        this.lookupPath = lookupPath;
    }

    public void visitClassDeclaration(ClassDeclaration typeDecl) {
        System.out.println("processing class: " + typeDecl.getQualifiedName());

        String serviceName = findSerivceName(typeDecl);

        if (serviceName == null) return;    //dokumentasjon kun for ekte webservicer. Se findServiceName...

        eksponerteMetoder = findMethods(typeDecl);


        try {
            File file = new File(serviceName + ".html");
            System.out.println("...creating file " + file);
            out = filer.createTextFile(Filer.Location.CLASS_TREE, "", file, "UTF8");

            out.println("<html>\n<head>\n<title>");
            out.println(serviceName);
            out.println("</title>");
            out.println(" <META http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
            out.println(" <link type=\"text/css\" href=\"ws-style.css\" rel=\"stylesheet\" />");
            out.println("</head>");
            out.println("<body>\n");


            out.println("<div id=\"header\">");

            out.println("<h3 class=\"overskrift\">");
            out.println(serviceName);
            out.println("</h3>");

            String serviceNS = findServiceNS(typeDecl);

            if (serviceNS != null) {
                out.println("\n namespaceURI = " + serviceNS);
            }


            { // service doc ....
                Map<String, String> comments = findJavaDocTags(typeDecl);

                if (comments.isEmpty() || !comments.containsKey("doc") || comments.containsKey("inheritDoc")) {
                    for (InterfaceType interfaceType : typeDecl.getSuperinterfaces()) {
                        comments = findJavaDocTags(interfaceType.getDeclaration());
                        if (!comments.containsKey("inheritDoc") && comments.containsKey("doc")) {
                            break;
                        }
                    }
                }
                if (comments.containsKey("doc")) {
                    out.print("<p>");
                    out.print(comments.get("doc"));
                    out.println("</p>");
                }
            }


            out.println("<h5>Metoder</h5>");
            out.println("<ul>");
            for (String metodeNavn : eksponerteMetoder.keySet()) {
                out.println(" <li><a href=\"#" + metodeNavn + "\">" + metodeNavn + "</a></li>");
            }
            out.println("</ul>");

            out.println("</div>");
            out.println("<div id=\"hoved\">");


//            System.out.println("processign class: " + typeDecl.getQualifiedName());
//            System.out.println("has superinterfaces: " + typeDecl.getSuperinterfaces().size() + " -> " + typeDecl.getSuperinterfaces());
//            System.out.println("has nested-types" + typeDecl.getNestedTypes().size() + " -> " + typeDecl.getNestedTypes());


            WebService webServiceAnnotation = typeDecl.getAnnotation(WebService.class);

            for (MethodDeclaration methodDeclaration : typeDecl.getMethods()) {
                printMethodDeclaration(methodDeclaration.getSimpleName(), webServiceAnnotation.targetNamespace());
            }

            out.println("</div></body></html>");
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }


    public void printMethodDeclaration(String methodName, String targetNamespace) {
        List<MethodDeclaration> methodDeclarations = eksponerteMetoder.get(methodName);
        if (methodDeclarations != null) {

            out.println("<div class=\"metode\">");

            out.print("<h4 class=\"metode\"><a name=\"" + methodName + "\" />");
            out.print(methodName);
            out.println("</h4>");

            MethodDeclaration methodDeclaration = null;
            for (MethodDeclaration declaration : methodDeclarations) {
                String methodDoc = declaration.getDocComment();
                if (methodDoc != null && !methodDoc.contains("@inheritDoc") && !methodDoc.trim().isEmpty()) {
                    methodDeclaration = declaration;
                    break;
                }
            }
            if (methodDeclaration == null) {
                System.out.println("Documentation not found for method " + methodName + "...");
                methodDeclaration = methodDeclarations.get(0);
            }


            Map<String, String> comments = findJavaDocTags(methodDeclaration);

            if (comments.containsKey("doc")) {
                out.println("<h5 class=\"dokumentasjon\">Dokumentasjon</h5>");
                out.print("<p>");
                out.print(comments.get("doc"));
                out.print("</p>\n");
            }


            out.println("<h5 class=\"request\">Inn</h5>");
            out.println("<ul>");
            for (ParameterDeclaration parameterDeclaration : methodDeclaration.getParameters()) {
                printParameterDeclarationListItem(parameterDeclaration, targetNamespace, comments);
            }
            out.println("</ul>");


            out.println("<h5 class=\"response\">Ut</h5>");
            out.println("<ul>");


            { //return...

                TypeMirror returnTypeMirror = methodDeclaration.getReturnType();
                if (returnTypeMirror != null && !(returnTypeMirror instanceof VoidType)) {

                    String retName = "return";
                    String ns = targetNamespace;

                    //slår opp annotasjon på deklarasjon av impementerende klasse (skal ligge først i lista)
                    WebResult annotation = methodDeclarations.get(0).getAnnotation(WebResult.class);
                    if (annotation != null) {
//                        System.out.println("found annotation for method: " + methodName);
                        if (annotation.targetNamespace() != null && !annotation.targetNamespace().trim().isEmpty()) {
                            ns = annotation.targetNamespace();
                        }
                        if (annotation.name() != null && !annotation.name().trim().isEmpty()) {
                            retName = annotation.name();
                        }
                    }

                    out.print("<li>");
                    printName(retName, ns, false);
                    out.print(" : ");
                    printName(getSimpleClassName(returnTypeMirror), annotation != null ? annotation.targetNamespace() : findObjectNamespace(returnTypeMirror), true);

                    String returnDoc = comments.get("return");
                    if (returnDoc != null) {
                        out.print("\n<br />&nbsp;&nbsp;&nbsp; - ");
                        out.println(returnDoc);
                        out.print("<br />");
                    }

                    out.println("<br /><br />");
                    out.println("</li>");
                }
            }

            { //throws ...
                for (ReferenceType type : methodDeclaration.getThrownTypes()) {
                    DeclaredType thrownType = (DeclaredType) type;
                    TypeDeclaration thrownTypeDeclaration = thrownType.getDeclaration();

                    String retName = thrownTypeDeclaration.getSimpleName();
                    String ns = targetNamespace;

                    WebFault annotation = thrownTypeDeclaration.getAnnotation(WebFault.class);
                    if (annotation != null) {
                        if (annotation.name() != null && !annotation.name().trim().isEmpty()) {
                            retName = annotation.name();
                        }
                    }

                    out.print("<li>");
                    printName(retName, ns, false);
                    out.print(" : ");
                    printName(getSimpleClassName(thrownType), findObjectNamespace(thrownType), true);


                    String doc = comments.get(thrownTypeDeclaration.getSimpleName());
                    if (doc != null) {
                        out.print("\n<br />&nbsp;&nbsp;&nbsp; - ");
                        out.println(doc);
                    }
                    out.println("</li>");

                }

            }

            out.println("</ul>");
            out.println("</div>");
        }
    }


    /**
     * Forsøker å finne namespace til objekt ved å se i package-info
     * <p/>
     * For kjente typer returneres namespace for disse.
     */
    private String findObjectNamespace(TypeMirror returnTypeMirror) {
        String objectNS = null;
        Class<?> clazz = getClazz(returnTypeMirror);
        if (clazz != null) {
            if (TypesMap.TYPES.containsKey(clazz)) {
                return TypesMap.TYPES.get(clazz).getNamespaceURI();
            }
            XmlSchema xmlSchemaAnnotation = clazz.getPackage().getAnnotation(XmlSchema.class);
            if (xmlSchemaAnnotation != null) {
                objectNS = xmlSchemaAnnotation.namespace();
            }
        }
        if (objectNS == null) {
            System.err.println("WARNING: namespace ikke definert for type " + returnTypeMirror.toString());
        }
        return objectNS;
    }


    /**
     * @return navn på deklarert klasse, eller standard navn til vanlige elementer definert i {@link TypesMap#TYPES }
     */
    private String getSimpleClassName(TypeMirror returnTypeMirror) {

        //forsøker å finne navn via klasse.
        Class clazz = getClazz(returnTypeMirror);
        if (clazz != null) {
            //sjekker om mapping finnes for standard typer
            for (Map.Entry<Class, QName> entry : TypesMap.TYPES.entrySet()) {
                if (entry.getKey().equals(clazz)) {
                    return entry.getValue().getLocalPart();
                }
            }

            return clazz.getSimpleName();
        }

        //forsøker  å finne navn via deklarasjon
        if (returnTypeMirror instanceof DeclaredType) {
            DeclaredType declaredType = (DeclaredType) returnTypeMirror;
            TypeDeclaration declaration = declaredType.getDeclaration();
            if (declaration != null) {
                return declaration.getSimpleName();
            }
        }

        return null;
    }

    /**
     * klasser finnes enten som deklarerte typer, eller som klasser som kan slås opp via classpath.
     *
     * @retun {@code null} dersom klasse ikke kunne finnes via classpath
     */
    private Class getClazz(TypeMirror returnTypeMirror) {
        class ClassNameVisitor implements TypeVisitor {
            Class clazz;

            public void visitClassType(ClassType classType) {
                if (classType != null) {
                    String name = classType.toString();
                    if (name != null) {
                        try {
                            clazz = Class.forName(name);
                        } catch (ClassNotFoundException e) {
                            ;//klasse finnes ikke på classpath
                        }
                    }
                }
            }

            public void visitTypeMirror(TypeMirror typeMirror) {
            }

            public void visitPrimitiveType(PrimitiveType primitiveType) {
                if (primitiveType != null) {
                    String name = primitiveType.toString();
                    if ("int".equals(name)) {
                        clazz = int.class;
                    } else if ("short".equals(name)) {
                        clazz = short.class;
                    } else if ("long".equals(name)) {
                        clazz = long.class;
                    } else if ("float".equals(name)) {
                        clazz = float.class;
                    } else if ("double".equals(name)) {
                        clazz = double.class;
                    } else if ("boolean".equals(name)) {
                        clazz = boolean.class;
                    }
                }
            }

            public void visitVoidType(VoidType voidType) {
            }

            public void visitReferenceType(ReferenceType referenceType) {
            }

            public void visitDeclaredType(DeclaredType declaredType) {
            }

            public void visitEnumType(EnumType enumType) {
            }

            public void visitInterfaceType(InterfaceType interfaceType) {
            }

            public void visitAnnotationType(AnnotationType annotationType) {
            }

            public void visitArrayType(ArrayType arrayType) {
            }

            public void visitTypeVariable(TypeVariable typeVariable) {
            }

            public void visitWildcardType(WildcardType wildcardType) {
            }
        }

        ClassNameVisitor visitor = new ClassNameVisitor();
        returnTypeMirror.accept(visitor);

        return visitor.clazz;
    }


    private void printParameterDeclarationListItem(ParameterDeclaration parameterDeclaration, String targetNamespace, Map<String, String> comments) {
        String ns = findWebParamNamespace(parameterDeclaration, targetNamespace);

//        String documentation = parameterDeclaration.getDocComment();
//        if (documentation != null) {
//            out.print("Dokumentasjon: " + documentation);
//        }
        out.print("  <li>");
        printName(findWebParamName(parameterDeclaration), ns, false);
        out.print(" : ");
        printName(getSimpleClassName(parameterDeclaration.getType()), findObjectNamespace(parameterDeclaration.getType()), true);
        String doc = comments.get(parameterDeclaration.getSimpleName());
        if (doc != null) {
            out.print(" - ");
            out.println(doc);
        }
        out.println("</li>");
    }


    /**
     * Finner deklarert namespace. Returneres blankt, anntas typen
     * å være standard type som mappes automatisk (feks xsd:string osv)
     */
    private String findWebParamNamespace(ParameterDeclaration parameterDeclaration, String targetNamespace) {
        WebParam annotation = parameterDeclaration.getAnnotation(WebParam.class);
        if (annotation != null) {
            if (annotation.targetNamespace() != null && !annotation.targetNamespace().trim().isEmpty()) {
                return annotation.targetNamespace();
            }
        }
        return targetNamespace;
    }

    /**
     * Finner deklarert parameter-navn.
     */
    private String findWebParamName(ParameterDeclaration parameterDeclaration) {
        String navn = parameterDeclaration.getSimpleName();
        WebParam annotation = parameterDeclaration.getAnnotation(WebParam.class);
        if (annotation != null && annotation.name() != null && !"".equals(annotation.name())) {
            navn = annotation.name();
        }
        return navn;
    }

    /**
     * Finner name space for service. Primært i targetNamespace annotasjon i interface, sekundært på implementasjonen.
     */
    private String findServiceNS(TypeDeclaration typeDecl) {

        String ns = null;

        for (TypeDeclaration superTypeDeclaration : typeDecl.getNestedTypes()) {
            ns = findServiceNS(superTypeDeclaration);
            if (ns != null) return ns;
        }

        WebService webService = typeDecl.getAnnotation(WebService.class);
        return (webService != null) ? webService.targetNamespace() : null;

    }

    /**
     * Finner definerte web-service metoder.
     * Dersom annotert med {@link javax.jws.WebMethod}, benyttes kun disse metoder.
     * <p/>
     * Algoritmen søker først i superklasserså i interface. Metodedeklarasjoner blir lagt i listen i den rekkefølge.
     *
     * @return sortert liste av alle definerte web-service metoder
     */
    private Map<String, List<MethodDeclaration>> findMethods(ClassDeclaration impltypeDecl) {
        ArrayList<String> metoder = new ArrayList<String>();
        ArrayList<String> wsMetoder = new ArrayList<String>();

        ArrayList<MethodDeclaration> methods = new ArrayList<MethodDeclaration>();
        ArrayList<InterfaceType> interfaces = new ArrayList<InterfaceType>();


        //finner aller metoder + interfaces
        if (impltypeDecl != null) {
            ClassDeclaration typeDecl = impltypeDecl;
            while (typeDecl != null && !Object.class.getName().equals(typeDecl.getQualifiedName())) {
                methods.addAll(typeDecl.getMethods());
                interfaces.addAll(typeDecl.getSuperinterfaces());

                if (typeDecl.getSuperclass() != null ) {
                    typeDecl = typeDecl.getSuperclass().getDeclaration();
                } else {
                    break;
                }
            }
        }

        //løper igjennom alle interfaces og henter også disee metodene
        for (InterfaceType interfaceType : interfaces) {
            methods.addAll(interfaceType.getDeclaration().getMethods());
        }

        //løper igjennom alle metodene
        for (MethodDeclaration methodDeclaration : methods) {
            String methodName = methodDeclaration.getSimpleName();
            metoder.add(methodName);
            WebMethod webMethod = methodDeclaration.getAnnotation(WebMethod.class);
            if (webMethod != null || wsMetoder.contains(methodName)) {
                wsMetoder.add(methodName);
            }
        }

        return metodenavnEllerWsMetodenavn(metoder, wsMetoder, methods);

    }

    private Map<String, List<MethodDeclaration>> metodenavnEllerWsMetodenavn(ArrayList<String> metoder, ArrayList<String> wsMetoder, ArrayList<MethodDeclaration> methods) {
        Map<String, List<MethodDeclaration>> map = new LinkedHashMap<String, List<MethodDeclaration>>();

        Collections.sort(metoder);
        Collections.sort(wsMetoder);

        //populerer map
        ArrayList<String> valgteMetoder = wsMetoder.isEmpty() ? metoder : wsMetoder;
        for (String name : valgteMetoder) {
            map.put(name, new ArrayList<MethodDeclaration>());
        }

        //populerer hvert metodenavn med alle funnede MethodDeclorations
        for (MethodDeclaration method : methods) {
            List<MethodDeclaration> methodDeclarationList = map.get(method.getSimpleName());
            if (methodDeclarationList != null) {
                methodDeclarationList.add(method);
            }
        }

        return map;
    }


    /**
     * Finner deklarert navn for webservice basert på {@link javax.jws.WebService } annotasjon i implementasjonsfil, eller i superklasser/superinterface
     *
     * @return deklarert navn, eller <tt>null</tt> dersom ikke funnet
     */
    private String findSerivceName(ClassDeclaration classDecl) {

        if (classDecl != null) {
            WebService webService = classDecl.getAnnotation(WebService.class);
            if (webService != null) {
                String serviceName = webService.name();
                if (serviceName != null && !serviceName.trim().isEmpty()) {
//                    System.out.println("->" + webService.name());
                    return webService.name();
                }
            }
//            System.out.println("fant ikke servicename for " + classDecl.toString());


            //finner navnet på service utifra annotaions
            for (AnnotationMirror annotationMirror : classDecl.getAnnotationMirrors()) {
                if ("javax.jws.WebService".equals(annotationMirror.getAnnotationType().getDeclaration().getQualifiedName())) {
                    for (Map.Entry<AnnotationTypeElementDeclaration, AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
                        if ("serviceName".equals(entry.getKey().getSimpleName())) {
                            AnnotationValue value = entry.getValue();
                            if (value != null && value.getValue() != null) {
                                return value.getValue().toString();
                            }
                        }
                    }
                }
            }

        }
        return null;
    }

    private void printName(String navn, String ns, boolean link) {
        boolean endA = false;
        if (lookupPath != null && ns != null && !ns.isEmpty()) {
            if (link && ns.contains("statkart")) {
                endA = true;
                out.print("<a href=\"");
                out.print(lookupPath);
                out.print("?");
                out.print(getJavadocURL(ns, navn));
                out.println("\">");
            }
            out.print("<span title=\"");
            out.println("namespaceURI:");
            out.print(ns);
            out.print("\" style=\"cursor: help\">");
        } else {
            out.print("<span>");
        }
        out.print(navn);
        out.print("</span>");
        if (endA) {
            out.print("</a>");
        }
    }

    /*
        getJavadocURL("http://grunnbok.statkart.no/borett/info/wsapi/exception", "ServiceException")

        => "no/statkart/grunnbok/borett/info/wsapi/exception/ServiceException.html"
     */
    private String getJavadocURL(String ns, String classs) {
        try {
            URL url = new URL(ns);
            String host = url.getHost();
            String path = url.getPath();

            StringBuffer buffer = new StringBuffer();
            for (String str : host.split("\\.")) {
                buffer.insert(0, str + "/");
            }
            buffer.append(path);
            buffer.append("/");
            buffer.append(classs);
            buffer.append(".html");

            return buffer.toString().replace("//", "/");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> findJavaDocTags(Declaration declaration) {
        HashMap<String, String> comments = new HashMap<String, String>();
        String docComment = declaration.getDocComment();
        if (docComment != null) {
            StringTokenizer st = new StringTokenizer(docComment, "\n", true);
            StringBuilder sb = new StringBuilder();
            while (st.hasMoreTokens()) {
                String token = st.nextToken();

                if (token.matches("^\\s*@.*")) {
                    Matcher returnMatcher = returnPattern.matcher(token);
                    Matcher paramMatcher = paramPattern.matcher(token);
                    Matcher throwsMatcher = throwsPattern.matcher(token);
                    Matcher inheritDocMatcher = inheritDocPattern.matcher(token);

                    if (returnMatcher.find()) {
                        comments.put("return", returnMatcher.group(1));
                    } else if (paramMatcher.find()) {
                        comments.put(paramMatcher.group(1), paramMatcher.group(2));
                    } else if (throwsMatcher.find()) {
                        comments.put(throwsMatcher.group(1), throwsMatcher.group(2));
                    } else if (inheritDocMatcher.matches()) {
                        comments.put("inheritDoc", "");
                    }

                } else if (!token.trim().isEmpty()) {
                    sb.append(token).append("<br />");
                }
            }
            comments.put("doc", sb.toString().trim());
        }
        return comments;
    }


}