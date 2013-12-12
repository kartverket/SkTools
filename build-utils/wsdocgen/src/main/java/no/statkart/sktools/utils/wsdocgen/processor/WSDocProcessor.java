package no.statkart.sktools.utils.wsdocgen.processor;

import no.statkart.sktools.utils.wsdocgen.processor.util.WSUtils;
import no.statkart.sktools.utils.wsdocgen.processor.xml.XMLBuilder;
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
import java.util.Set;


/**
 * Processor implemented in {@code Pluggabable Annotation Processing API} (JSR 269)
 *
 * <br/>
 * <b>XML-structure for XSLT processing built by {@link XMLBuilder}: <b/><pre> {@code

<services>
  <service name="" namespace="" description="">
    <methods>
      <method name="" description="">
        <parameters>
          <parameter name="" description="">
            <type name="" namespace="" javadocPath="">description</type>
          </parameter>
        </parameters>
        <returns>
          <!-- empty list when void -->
          <parameter name="" description="">
            <type name="" namespace="" javadocPath="">description</type>
          </parameter>
        </returns>
        <exceptions>
          <!-- might be empty -->
          <exception name="" description="" >
            <type name="" namespace="" javadocPath="">description</type>
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
@SupportedAnnotationTypes(value= {"javax.jws.WebMethod"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedOptions(value = {"xslt", "javaDocLookupPath"})
public class WSDocProcessor extends AbstractProcessor {


    public WSDocProcessor() {
        System.out.println(String.format("Constructing class %s", this.getClass().getSimpleName()));
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (Element element : roundEnv.getElementsAnnotatedWith(WebService.class)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("Processing class: %s ", element));



            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder;
            try {
                docBuilder = docFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("Feil ved opprettelse av DOM: %s ", e.getMessage()));
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                return false;
            }

            final Document document = docBuilder.newDocument();
            final XMLBuilder xmlBuilder = new XMLBuilder(document, processingEnv);
            xmlBuilder.appendService(element);


//            final Filer filer = processingEnv.getFiler();
//            final Elements elementUtils = processingEnv.getElementUtils();
//            printDocumentTilSystemOut(document);


            final String fileName = String.format("%s.html", WSUtils.findWebServicePortTypeName(element));
            FileObject outputFile = null;

            try {
                outputFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", fileName);
                writeToFile(document, outputFile);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("Error creating target-file %s", fileName));
            } catch (RuntimeException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("%s", e.getMessage()), element);
            }


            int debug = 0;

        }

        return false;
    }




    void writeToFile(Document document, FileObject outputFile) {

        final String xsltFilePath = processingEnv.getOptions().get("xslt");
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
            throw new RuntimeException(String.format("No xslt file defined! Configure javac with argument -Axslt=<file>"));
        }

    }


    void transform(Source in, Result out, Source transform) {
        TransformerFactory factory = TransformerFactory.newInstance();
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
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


}
