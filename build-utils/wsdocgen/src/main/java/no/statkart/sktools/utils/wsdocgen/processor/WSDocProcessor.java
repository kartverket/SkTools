package no.statkart.sktools.utils.wsdocgen.processor;

import no.statkart.sktools.utils.wsdocgen.processor.util.WSUtils;
import no.statkart.sktools.utils.wsdocgen.processor.xml.XMLBuilderFactory;
import org.w3c.dom.Document;

import javax.annotation.processing.*;
import javax.jws.WebService;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Processor implemented in {@code Pluggabable Annotation Processing API} (JSR 269)
 *
 * <br/>
 * <b>XML-structure for XSLT processing built by {@link XMLBuilderFactory}: <b/><pre> {@code

<services>
  <service name="" portName="" namespace="" description="" href="relative url">
    <methods>
      <method name="">
        <description>...</description>

        <parameters>
          <parameter name="">
            <description>...</description>
            <type name="" namespace="" javadocPath="">description of type</type>
          </parameter>
        </parameters>

        <returns>
          <!-- empty list when void -->
          <parameter name="">
            <description>...</description>
            <type name="" namespace="" javadocPath="">description of type</type>
          </parameter>
        </returns>

        <exceptions>
          <!-- might be empty -->
          <exception name="">
            <description>...</description>
            <type name="" namespace="" javadocPath="">description of type</type>
          </exception>
        </exceptions>

      </method>
    </methods>
  </service>
</services>
}
 </pre>
 *
 * @author Leif Lislegård
 * @since 1.3 - ny grunnbok sprint 30
 */
@SupportedAnnotationTypes(value= {"javax.jws.WebService"})
@SupportedOptions(value = {
        "xslt",
        "indexXslt",  //SKTOOLS-105
        "javaDocLookupPath",
})
public class WSDocProcessor extends AbstractProcessor {

    private DocumentBuilder docBuilder;


    private boolean generateIndex; //SKTOOLS-105
    private String indexXsltFilePath; //SKTOOLS-105
    private final static String indexFileNamePattern = "index.html"; //SKTOOLS-105
    private XMLBuilderFactory indexXmlBuilderFactory; //SKTOOLS-105
    private org.w3c.dom.Element indexServices;

    public WSDocProcessor() {
        int debug = 0;
 //       System.out.println(String.format("Constructing class %s", this.getClass().getSimpleName()));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("Feil ved opprettelse av DOM: %s ", e.getMessage()));
            e.printStackTrace();
        }

        indexXsltFilePath = processingEnv.getOptions().get("indexXslt");
        generateIndex = indexXsltFilePath != null;

        if (generateIndex) {
            indexXmlBuilderFactory = new XMLBuilderFactory(docBuilder.newDocument(), processingEnv);
            indexServices = indexXmlBuilderFactory.getServicesBuilder().createServices();
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Map<String, Element> wsiByWSBeanName = findWsiNames(roundEnv);

        for (Element element : roundEnv.getElementsAnnotatedWith(WebService.class)) {
            String webServicePortTypeName = WSUtils.findWebServicePortTypeName(element);
            if (webServicePortTypeName != null) {

                final String fileName = String.format("%s.html", webServicePortTypeName);
                System.out.println(String.format("Processing class: %s ", element));
                //processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("Processing class: %s ", element));

                final XMLBuilderFactory xmlBuilder = new XMLBuilderFactory(docBuilder.newDocument(), processingEnv);
                final org.w3c.dom.Element services = xmlBuilder.getServicesBuilder().createServices();
                final Element wsiElement = wsiByWSBeanName.get(element.getSimpleName().toString()); //korresponderende element for WSI deklarasjon

                try {
                    xmlBuilder.getServiceBuilder().appendServiceTo(services, element, fileName, wsiElement);
                } catch (RuntimeException e) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("%s", e.getMessage()), element);
                }

                FileObject outputFile = null;

                try {
                    outputFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", fileName);
                } catch (IOException e) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("Error creating target-file %s", fileName));
                    continue;
                }

                String xsltFilePath = processingEnv.getOptions().get("xslt");
                if (xsltFilePath == null) {
                    throw new RuntimeException(String.format("No xslt file defined! Configure javac with argument -Axslt=<file>"));
                }

                writeToFile(xmlBuilder.getDocument(), outputFile, xsltFilePath);

                addToIndex(element, fileName, wsiElement);

            } else {
                //skipping some WebService elements in input...
                System.out.println(String.format("Unable to resolve portType - skipping class %s", element));
            }
        }

        //index fil: SKTOOLS-105
        if (roundEnv.processingOver() && generateIndex) {
            final String fileName = String.format(indexFileNamePattern);
            FileObject outputFile = null;

            try {
                outputFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", fileName);
                writeToFile(indexXmlBuilderFactory.getDocument(), outputFile, indexXsltFilePath);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("Error creating target-file %s", fileName));
            }

        }

        return false;
    }

    static Map<String, Element> findWsiNames(RoundEnvironment roundEnv) {
        Map<String, Element> wsiByWSBeanName = new HashMap<>();
        for (Element element : roundEnv.getRootElements()) {
            String simpleName = element.getSimpleName().toString();
            if(simpleName.endsWith("WSI")) {
                String wsBeanName = simpleName.replaceAll("WSI\\z", "WSBean");
                wsiByWSBeanName.put(wsBeanName, element);
            }
        }
        return wsiByWSBeanName;
    }

    /**
     * index fil: SKTOOLS-105
     */
    void addToIndex(Element element, String fileName, Element wsiElement) {
        if (indexServices != null) {
            indexXmlBuilderFactory.getServiceBuilder().appendServiceTo(indexServices, element, fileName, wsiElement);
        }
    }


    void writeToFile(Document document, FileObject outputFile, String xsltFilePath) {
        if (xsltFilePath != null) {
            final File xsltFile = new File(xsltFilePath);
            if (xsltFile.exists()) {
                try {
                    final DOMSource in = new DOMSource(document);
                    StreamResult out = new StreamResult(outputFile.openOutputStream());
                    StreamSource xsltStream = new StreamSource(xsltFile);

                    transform(in, out, xsltStream);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException(String.format("Xslt file not found. Tried %s", xsltFilePath));
            }
        } else {
            //todo: write raw xml to disk?
        }

    }


    void transform(Source in, Result out, Source transform) {
//        TransformerFactory factory = TransformerFactory.newInstance();

        /* {@link http://stackoverflow.com/questions/11314604/how-to-set-saxon-as-the-xslt-processor-in-java one of several ways of declaring wich impl to use}  */
        TransformerFactory factory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", this.getClass().getClassLoader());

        Transformer transformer;
        try {
            transformer = factory.newTransformer(transform);
//spesifiseres heller i xsl tempaten..
//            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
//            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
//            transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
//            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//W3C//DTD XHTML 1.0 Strict//EN");
//            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd");
//            transformer.setOutputProperty(OutputKeys.INDENT, "no");


            transformer.transform(in, out);
        } catch (TransformerConfigurationException e) {
//            e.printStackTrace();
            throw new RuntimeException(String.format("Exception in xslt configuration! Message: %s", e.getMessage()), e);
        } catch (TransformerException e) {
            throw new RuntimeException(String.format("Exception in xslt! Message: %s", e.getMessage()), e);
        }


    }


    // debug tasks... ->

    /**
     * For debugging only!
     * Prints the contents of a {@link Document} to system.out
     */
    public static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(doc),
                new StreamResult(new OutputStreamWriter(out, "UTF-8")));
    }


    static void printDocumentTilSystemOut(Document document) {
        try {
            printDocument(document, System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
