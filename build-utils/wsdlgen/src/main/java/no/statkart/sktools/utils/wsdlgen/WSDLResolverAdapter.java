package no.statkart.sktools.utils.wsdlgen;

import com.oracle.webservices.api.databinding.WSDLResolver;
import jakarta.xml.ws.Holder;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

class WSDLResolverAdapter implements WSDLResolver {
    final ArrayList<OutputStream> streams = new ArrayList<>();
    private final Path destinationDirectory;

    WSDLResolverAdapter(Path destinationDirectory) {
        this.destinationDirectory = destinationDirectory;
    }

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
        return getAbstractWSDL(filename.value);
    }

    public Result getAbstractWSDL(String filename) {
        try {
            Path path = destinationDirectory.resolve(filename);
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
        return getSchemaOutput(namespace, filename.value);
    }

    public Result getSchemaOutput(String namespace, String filename) {
        try {
            Path path = destinationDirectory.resolve(filename);
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
}
