package no.statkart.sktools.gradle.plugins.wsdlcustomizer;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Task som erstatter referanser til genererte XSD-filer med de opprinnelige, samt fjerner referanser til de namespace/
 * skjemaer som ikke skal offentliggjøres.
 *
 * @author Tor Egil R. Strand
 */
public class CustomWsdlTask extends DefaultTask {
    protected static final Logger logger = Logging.getLogger(CustomWsdlTask.class);
    protected static final String xsdNamespace = "http://www.w3.org/2001/XMLSchema";

    private final List<Object> originalSchemaFiles = new ArrayList<Object>();
    private final List<Object> generatedWsdlAndSchemaFiles = new ArrayList<Object>();
    private final Set<String> includedNamespaces = new HashSet<String>();
    private final Set<String> excludedNamespaces = new HashSet<String>();

    private File destinationDir;

    @InputFiles
    public FileCollection getOriginalSchemaFiles() {
        return getProject().files(originalSchemaFiles);
    }

    @InputFiles
    @SkipWhenEmpty
    public FileCollection getGeneratedWsdlAndSchemaFiles() {
        return getProject().files(generatedWsdlAndSchemaFiles);
    }

    public CustomWsdlTask originalSchemaFiles(Object... paths) {
        Collections.addAll(originalSchemaFiles, paths);
        return this;
    }

    public CustomWsdlTask generatedWsdlAndSchemaFiles(Object... paths) {
        Collections.addAll(generatedWsdlAndSchemaFiles, paths);
        return this;
    }

    @OutputDirectory
    public File getDestinationDir() {
        return destinationDir;
    }

    public void setDestinationDir(File destinationDir) {
        this.destinationDir = destinationDir;
    }

    @Input
    public Set<String> getIncludedNamespaces() {
        return includedNamespaces;
    }

    public CustomWsdlTask includeNamespaces(Object... namespaces) {
        for (Object namespace : namespaces) {
            includedNamespaces.add(namespace.toString());
        }
        return this;
    }

    @Input
    public Set<String> getExcludedNamespaces() {
        return excludedNamespaces;
    }

    public CustomWsdlTask excludeNamespaces(Object... namespaces) {
        for (Object namespace : namespaces) {
            excludedNamespaces.add(namespace.toString());
        }
        return this;
    }

    private boolean shouldContainNamespace(String namespace) {
        if (includedNamespaces.isEmpty() || includedNamespaces.contains(namespace)) {
            if (!excludedNamespaces.contains(namespace)) {
                return true;
            }
        }
        return false;
    }

    @TaskAction
    public void generate() throws IOException {
        DocumentBuilder documentBuilder;
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new GradleException("Error creating XML parser", e);
        }

        Transformer transformer;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new GradleException("Error creating XML transformer", e);
        }

        getProject().delete(destinationDir);
        //noinspection ResultOfMethodCallIgnored
        destinationDir.mkdirs();

        final CopySpec copySpec = getProject().copySpec((Action<? super CopySpec>) null);

        Map<String, String> namespaceSchemaFileMap = readOriginalSchemas(documentBuilder, copySpec);

        List<File> wsdls = new ArrayList<File>();
        List<File> generatedSchemas = new ArrayList<File>();

        for (File file1 : getGeneratedWsdlAndSchemaFiles().getFiles()) {
            if (file1.isDirectory()) {
                for (File file2 : getProject().fileTree(file1)) {
                    String name = file2.getName();
                    if (name.endsWith(".xsd")) {
                        generatedSchemas.add(file2);
                    } else if (name.endsWith(".wsdl")) {
                        wsdls.add(file2);
                    }
                }
            } else {
                String name = file1.getName();
                if (name.endsWith(".xsd")) {
                    generatedSchemas.add(file1);
                } else if (name.endsWith(".wsdl")) {
                    wsdls.add(file1);
                }
            }
        }

        Map<String, File> generatedSchemaFileMap = readGeneratedSchemas(documentBuilder, generatedSchemas);

        for (File wsdl : wsdls) {
            getLogger().info("Processing " + wsdl);
            processWsdl(documentBuilder, transformer, wsdl, namespaceSchemaFileMap, generatedSchemaFileMap, copySpec);
        }

        getProject().copy(new Closure(getProject()) {
            @SuppressWarnings("UnusedDeclaration")
            public void doCall(CopySpec spec) {
                spec.with(copySpec);
                spec.into(destinationDir);
            }
        });
    }

    private void processWsdl(DocumentBuilder documentBuilder, Transformer transformer, File wsdl, Map<String, String> namespaceSchemaFileMap, Map<String, File> generatedSchemaFileMap, CopySpec copySpec) {
        File destinationFile = new File(destinationDir, wsdl.getName());

        try {
            Document document = documentBuilder.parse(wsdl);

            Element root = document.getDocumentElement();

            String serviceNamespace = root.getAttribute("targetNamespace");

            NodeList schemas = root.getElementsByTagNameNS(xsdNamespace, "schema");
            List<Node> nodesToKill = new ArrayList<Node>();
            for (int i = 0; i < schemas.getLength(); ++i) {
                Element schema = (Element) schemas.item(i);
                NodeList imports = schema.getElementsByTagNameNS(xsdNamespace, "import");
                Element imp = (Element) imports.item(0);
                String importNamespace = imp.getAttributeNS(null, "namespace");
                if (importNamespace.equals(serviceNamespace)) {
                    getLogger().info("Processing " + importNamespace + " as service namespace");
                    processServiceSchema(documentBuilder, transformer, namespaceSchemaFileMap, generatedSchemaFileMap.get(importNamespace));
                } else if (shouldContainNamespace(importNamespace)) {
                    String schemaFile = namespaceSchemaFileMap.get(importNamespace);
                    if (schemaFile != null) {
                        getLogger().info("Using original schema " + schemaFile + " for " + importNamespace);
                        imp.setAttributeNS(null, "schemaLocation", schemaFile);
                    } else {
                        File file = generatedSchemaFileMap.get(importNamespace);
                        getLogger().info("Using generated schema " + file + " for " + importNamespace);
                        copySpec.from(file);
                    }
                } else {
                    getLogger().lifecycle("Filtering out namespace " + importNamespace);
                    nodesToKill.add(schema); // Kan ikke slette mens det itereres
                }
            }
            for (Node node : nodesToKill) {
                Node previousSibling = node.getPreviousSibling();
                if (previousSibling.getNodeType() == Node.TEXT_NODE) {
                    previousSibling.getParentNode().removeChild(previousSibling);
                }
                node.getParentNode().removeChild(node);
            }

            Result output = new StreamResult(destinationFile);
            Source input = new DOMSource(document);
            transformer.transform(input, output);

        } catch (SAXException e) {
            throw new GradleException("Error parsing " + wsdl);
        } catch (IOException e) {
            throw new GradleException("Error parsing " + wsdl);
        } catch (TransformerException e) {
            throw new GradleException("Error writing " + destinationFile);
        }
    }

    private void processServiceSchema(DocumentBuilder documentBuilder, Transformer transformer, Map<String, String> namespaceSchemaFileMap, File serviceSchema) {
        File destinationFile = new File(destinationDir, serviceSchema.getName());

        try {
            Document document = documentBuilder.parse(serviceSchema);

            NodeList imports = document.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "import");
            for (int i = 0; i < imports.getLength(); ++i) {
                Element imp = (Element) imports.item(i);
                String ns = imp.getAttributeNS(null, "namespace");
                imp.setAttributeNS(null, "schemaLocation", namespaceSchemaFileMap.get(ns));
            }

            Result output = new StreamResult(destinationFile);
            Source input = new DOMSource(document);
            transformer.transform(input, output);
        } catch (SAXException e) {
            throw new GradleException("Error parsing " + serviceSchema);
        } catch (IOException e) {
            throw new GradleException("Error parsing " + serviceSchema);
        } catch (TransformerException e) {
            throw new GradleException("Error writing " + destinationFile);
        }
    }

    private HashMap<String, String> readOriginalSchemas(DocumentBuilder documentBuilder, CopySpec copySpec) {
        HashMap<String, String> namespaceSchemaFileMap = new HashMap<String, String>();

        XPathExpression xPathExpression;
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            xPath.setNamespaceContext(new XsdNamespaceContext());
            xPathExpression = xPath.compile("/xsd:schema/@targetNamespace");
        } catch (XPathExpressionException e) {
            throw new GradleException("Error creating XPath", e);
        }

        Set<File> filesAndDirs = getOriginalSchemaFiles().getFiles();
        Set<File> files = new LinkedHashSet<File>(filesAndDirs.size());
        for (File fileOrDir : filesAndDirs) {
            if (fileOrDir.isDirectory()) {
                files.addAll(getProject().fileTree(fileOrDir).getFiles());
            } else {
                files.add(fileOrDir);
            }
        }

        for (File schemaFile : files) {
            try {
                Document document = documentBuilder.parse(schemaFile);
                String targetNamespace = xPathExpression.evaluate(document);
                assert targetNamespace != null;
                if (shouldContainNamespace(targetNamespace)) {
                    copySpec.from(schemaFile);
                    namespaceSchemaFileMap.put(targetNamespace, schemaFile.getName());
                }
            } catch (SAXException e) {
                throw new GradleException("Error parsing " + schemaFile, e);
            } catch (IOException e) {
                throw new GradleException("Error parsing " + schemaFile, e);
            } catch (XPathExpressionException e) {
                throw new GradleException("Error parsing " + schemaFile, e);
            }
        }

        return namespaceSchemaFileMap;
    }

    private HashMap<String, File> readGeneratedSchemas(DocumentBuilder documentBuilder, Collection<File> schemaFiles) {
        HashMap<String, File> namespaceSchemaFileMap = new HashMap<String, File>();

        XPathExpression xPathExpression;
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            xPath.setNamespaceContext(new XsdNamespaceContext());
            xPathExpression = xPath.compile("/xsd:schema/@targetNamespace");
        } catch (XPathExpressionException e) {
            throw new GradleException("Error creating XPath", e);
        }

        for (File schemaFile : schemaFiles) {
            try {
                Document document = documentBuilder.parse(schemaFile);
                String targetNamespace = xPathExpression.evaluate(document);
                namespaceSchemaFileMap.put(targetNamespace, schemaFile);
            } catch (SAXException e) {
                throw new GradleException("Error parsing " + schemaFile, e);
            } catch (IOException e) {
                throw new GradleException("Error parsing " + schemaFile, e);
            } catch (XPathExpressionException e) {
                throw new GradleException("Error parsing " + schemaFile, e);
            }
        }

        return namespaceSchemaFileMap;
    }

    private static class XsdNamespaceContext implements NamespaceContext {
        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix.equals("xsd")) {
                return xsdNamespace;
            } else {
                return null;
            }
        }

        @Override
        public String getPrefix(String namespaceURI) {
            if (namespaceURI.equals(xsdNamespace)) {
                return "xsd";
            } else {
                return "";
            }
        }

        @Override
        public Iterator getPrefixes(String namespaceURI) {
            return Collections.singletonList(getPrefix(namespaceURI)).iterator();
        }
    }

    public Logger getLogger() {
        return logger;
    }

}
