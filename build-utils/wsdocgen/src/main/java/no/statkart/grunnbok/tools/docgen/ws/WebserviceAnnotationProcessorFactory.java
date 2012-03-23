package no.statkart.grunnbok.tools.docgen.ws;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.TypeDeclaration;

import java.util.*;

import static com.sun.mirror.util.DeclarationVisitors.NO_OP;
import static com.sun.mirror.util.DeclarationVisitors.getDeclarationScanner;

/**
 * Genererer dokumentasjon av java web-services (JAX-WS) via en apt-plugin
 * - Eksempel på bruk via ant:
 *
 *<code><pre>
      
      &lt;path id="build.apt.class.path.refs">
         &lt;pathelement path="${wsdocgen.jar}" />
         &lt;pathelement path="${tools.jar}" />
       &lt;/path>

       &lt;apt srcdir="src"
            destdir="${jws.docs.dir}"
            debug="on"
            compile="false"
            classpathref="build.apt.class.path.refs"
            factory="no.statkart.grunnbok.tools.docgen.ws.WebserviceAnnotationProcessorFactory"
            >
           &lt;option name="LookupPath" value="/dokumentasjon/fast/main/wsdomain/lookup" />
           &lt;include name="&#42;&#42;/*WSBean.java"/>
       &lt;/apt>
 
 
 *</pre></code>
 *
 *
 * Parametere
 * <ul>
 *  <li>{@code LookupPath} bestemmer url for oppslag av javadoc dokumentasjon</li>
 * </ul>
 *
 * @author Leif Lislegård
 * @since 0.3
 */
public class WebserviceAnnotationProcessorFactory implements AnnotationProcessorFactory {

    // Process any set of annotations
    private static final Collection<String> supportedAnnotations = Collections.unmodifiableCollection(Arrays.asList("*"));

    // No supported options
    private static final Collection<String> supportedOptions = Collections.unmodifiableCollection(
            Arrays.asList("-ALookupPath")
    );


    public Collection<String> supportedAnnotationTypes() {
        return supportedAnnotations;
    }

    public Collection<String> supportedOptions() {
        return supportedOptions;
    }

    public AnnotationProcessor getProcessorFor(
            Set<AnnotationTypeDeclaration> atds,
            AnnotationProcessorEnvironment env) {
        return new ListClassAp(env);
    }

    private static class ListClassAp implements AnnotationProcessor {
        private final AnnotationProcessorEnvironment env;

        ListClassAp(AnnotationProcessorEnvironment env) {
            this.env = env;
        }

        public void process() {
            for (TypeDeclaration typeDecl : env.getSpecifiedTypeDeclarations()) {
                PrintWebServiceDocumentationVisitor visitor = new PrintWebServiceDocumentationVisitor(env.getFiler());

                for (Map.Entry<String, String> entry : env.getOptions().entrySet()) {

                    //somehow the values are concatened to the keys... seems like like there might be bug in ant.

                    if (entry.getKey().startsWith("-ALookupPath=")) {
                        visitor.setLookupPath(entry.getKey().substring("-ALookupPath=".length()));
                    }
                }

                typeDecl.accept(getDeclarationScanner(visitor, NO_OP));
            }
        }

    }
}