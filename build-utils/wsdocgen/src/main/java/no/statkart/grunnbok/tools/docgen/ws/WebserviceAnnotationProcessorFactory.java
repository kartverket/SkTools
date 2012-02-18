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
 *
      <path id="build.apt.class.path.refs">
           <pathelement path="${build.classes.dir}" />
           <path refid="weblogic.classpath"/>
       </path>

       <apt srcdir="src"
            destdir="${jws.docs.dir}"
            debug="on"
            compile="false"
            classpathref="build.apt.class.path.refs"
            factory="no.statkart.grunnbok.tools.docgen.ws.WebserviceAnnotationProcessorFactory"
            >
           <option name="LookupPath" value="/dokumentasjon/fast/main/wsdomain/lookup" />
           <option name="LookupParameter" value="ns" />
           <include name="*.java"/>
       </apt>
 *
 *
 * Parametere
 * <ul>
 *  <li>LookupPath bestemmer url for oppslag av dokumentasjon</li>
 *  <li>LookupParameter bestemmer navn for parameter som inneholder namespace for typen. Standard verdi er "ns"</li>
 * </ul>
 *
 * @author Leif Lislegård
 * @since 0.3
 */
public class WebserviceAnnotationProcessorFactory implements AnnotationProcessorFactory {

    // Process any set of annotations
    private static final Collection<String> supportedAnnotations = Collections.unmodifiableCollection(Arrays.asList("*"));

    // No supported options
    private static final Collection<String> supportedOptions = Collections.emptySet();


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

                    //somehow the values are concatened to the keys... looks like a bug in ant somehow

                    if (entry.getKey().startsWith("-ALookupPath=")) {
                        visitor.setLookupPath(entry.getKey().substring("-ALookupPath=".length()));
                    }
                }

                typeDecl.accept(getDeclarationScanner(visitor, NO_OP));
            }
        }

    }
}