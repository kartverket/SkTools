package no.statkart.sktools.gradle.plugins.filterresources;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;

public class FilterPropertiesTestutil {

    /**
     * Oppretter to ressursfiler med navn <code>simpleResource1.txt</code> og <code>simpleResource2.txt</code>
     *
     * <p><code>simpleResource1.txt</code> inneholder
     * <ul>
     *     <li>name=@name@
     *     <li>version=@version@
     *     <li>myProperty1=@myProperty1@
     * </ul>
     *
     * <p><code>simpleResource2.txt</code> inneholder
     * <ul>
     *     <li>myProperty1=@myProperty1@
     *     <li>myProperty2=@myProperty2@
     *     <li>myEmail=@myEmail@@statkart.no
     * </ul>
     *
     */
    public static Collection<File> writeTwoSimpleResources(Path rootPath, String targetPath) throws IOException {
        Path targetDir = rootPath.resolve(targetPath);
        targetDir.toFile().mkdirs();

        Path resource1 = targetDir.resolve("simpleResource1.txt");
        Path resource2 = targetDir.resolve("simpleResource2.txt");

        Files.write(resource1, Arrays.asList("name=@name@", "version=@version@", "myProperty1=@myProperty1@"),
                StandardCharsets.UTF_8,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        Files.write(resource2, Arrays.asList("myProperty1=@myProperty1@", "myProperty2=@myProperty2@", "myEmail=@myEmail@@statkart.no"),
                StandardCharsets.UTF_8,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        return Arrays.asList(resource1.toFile(), resource2.toFile());
    }
}
