package no.statkart.sktools.utils.wsdlgen;

import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.databinding.DatabindingConfig;
import com.sun.xml.ws.api.databinding.DatabindingFactory;
import com.sun.xml.ws.api.databinding.WSDLGenInfo;
import com.sun.xml.ws.binding.WebServiceFeatureList;
import com.sun.xml.ws.db.DatabindingImpl;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import jakarta.jws.WebService;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SKGenWSDL {
    /**
     * Environment variabel for lang classpath.
     */
    public static final String WEB_SERVICE_CLASSPATH = "WEB_SERVICE_CLASSPATH";

    public static void main(String... args) throws IOException, ParseException, ClassNotFoundException {
        Options options = new Options()
            .addOption(
                Option.builder("cp")
                    .longOpt("classpath")
                    .hasArg()
                    .desc("Classpath for web services")
                    .required(false)
                    .build()
            )
            .addOption(
                Option.builder("d")
                    .longOpt("destination")
                    .hasArg()
                    .desc("Destination for WSDLs and schemas")
                    .required()
                    .build()
            );

        CommandLine commandLine = new DefaultParser()
            .parse(options, args);

        String classpath = Objects.toString(System.getenv(WEB_SERVICE_CLASSPATH), commandLine.getOptionValue("cp"));
        if (classpath == null) {
            throw new IllegalArgumentException(String.format("Classpath not set. Use parameter -%s <value> or environment variable %s.", "cp", WEB_SERVICE_CLASSPATH));
        }
        Path destination = Paths.get(commandLine.getOptionValue("d"));

        DatabindingFactory databindingFactory = DatabindingFactory.newInstance();
        ClassLoader serviceClassLoader = classLoaderFor(classpath);

        try (ScanResult scanResult = new ClassGraph()
            .enableClasspathEntries(classpath)
            .enableClassInfo()
            .enableAnnotationInfo()
            .enableExternalClasses()
            .scan()) {
            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(WebService.class.getName())) {
                if (classInfo.isStandardClass()) {
                    Class<?> wsClass = Class.forName(classInfo.getName(), false, serviceClassLoader);
                    WSDLResolverAdapter wsdlResolverAdapter = new WSDLResolverAdapter(destination);
                    genWSDL(wsClass, wsdlResolverAdapter, databindingFactory);
                }
            }
        }
    }

    private static void genWSDL(Class<?> endpointClass, WSDLResolverAdapter wsdlResolver, DatabindingFactory databindingFactory) throws IOException {
        DatabindingConfig config = new DatabindingConfig();
        config.setClassLoader(endpointClass.getClassLoader());
        config.setEndpointClass(endpointClass);
        config.setFeatures(new WebServiceFeatureList(endpointClass));
        config.getMappingInfo().setBindingID(BindingID.parse(endpointClass));
        DatabindingImpl runtime = (DatabindingImpl) databindingFactory.createRuntime(config);

        WSDLGenInfo wsdlGenInfo = new WSDLGenInfo();
        wsdlGenInfo.setWsdlResolver(wsdlResolver);
        runtime.generateWSDL(wsdlGenInfo);
        for (OutputStream stream : (wsdlResolver.streams)) {
            stream.close();
        }
    }

    private static ClassLoader classLoaderFor(String classpath) throws IOException {
        List<URL> urls = new ArrayList<>();
        for (String entry : classpath.split(File.pathSeparator)) {
            if (!entry.isEmpty()) {
                urls.add(new File(entry).toURI().toURL());
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), SKGenWSDL.class.getClassLoader());
    }
}
