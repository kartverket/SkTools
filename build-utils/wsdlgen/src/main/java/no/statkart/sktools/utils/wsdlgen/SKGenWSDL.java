package no.statkart.sktools.utils.wsdlgen;

import com.oracle.webservices.api.databinding.WSDLResolver;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.databinding.DatabindingConfig;
import com.sun.xml.ws.api.databinding.DatabindingFactory;
import com.sun.xml.ws.api.databinding.WSDLGenInfo;
import com.sun.xml.ws.binding.WebServiceFeatureList;
import com.sun.xml.ws.db.DatabindingImpl;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.Holder;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

public class SKGenWSDL {
    /**
     * Environment variabel for lang classpath.
     */
    public static final String WEB_SERVICE_CLASSPATH = "WEB_SERVICE_CLASSPATH";

    public static void main(String... args) throws IOException, ParseException {
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

        try (ScanResult scanResult = new ClassGraph()
            .overrideClasspath(classpath)
            .enableClassInfo()
            .enableAnnotationInfo()
            .enableExternalClasses()
            .scan()) {
            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation("javax.jws.WebService")) { //jws-api
                if (classInfo.isStandardClass()) {
                    Class<?> wsClass = classInfo.loadClass();
                    genWSDL(wsClass, destination, databindingFactory);
                }
            }
        }
    }

    private static void genWSDL(Class<?> endpointClass, Path destinationDirectory, DatabindingFactory databindingFactory) throws IOException {
        DatabindingConfig config = new DatabindingConfig();
        config.setClassLoader(endpointClass.getClassLoader());
        config.setEndpointClass(endpointClass);
        config.setFeatures(new WebServiceFeatureList(endpointClass));
        config.getMappingInfo().setBindingID(BindingID.parse(endpointClass));
        DatabindingImpl runtime = (DatabindingImpl) databindingFactory.createRuntime(config);

        ArrayList<OutputStream> streams = new ArrayList<>();
        WSDLGenInfo wsdlGenInfo = new WSDLGenInfo();
        wsdlGenInfo.setWsdlResolver(new WSDLResolver() {
            @Override
            public Result getWSDL(String suggestedFilename) {
                try {
                    Path path = destinationDirectory.resolve(suggestedFilename);
                    OutputStream outputStream = Files.newOutputStream(path);
                    streams.add(outputStream);
                    StreamResult result = new StreamResult(outputStream);
                    result.setSystemId(path.toFile());
                    return result;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public Result getAbstractWSDL(Holder<String> filename) {
                try {
                    Path path = destinationDirectory.resolve(filename.value);
                    OutputStream outputStream = Files.newOutputStream(path);
                    streams.add(outputStream);
                    StreamResult result = new StreamResult(outputStream);
                    result.setSystemId(path.toFile());
                    return result;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public Result getSchemaOutput(String namespace, Holder<String> filename) {
                try {
                    Path path = destinationDirectory.resolve(filename.value);
                    OutputStream outputStream = Files.newOutputStream(path);
                    streams.add(outputStream);
                    StreamResult result = new StreamResult(outputStream);
                    result.setSystemId(path.toFile());
                    return result;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
        runtime.generateWSDL(wsdlGenInfo);
        for (OutputStream stream : streams) {
            stream.close();
        }
    }
}
