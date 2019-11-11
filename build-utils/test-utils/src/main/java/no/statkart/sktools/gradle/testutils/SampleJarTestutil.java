package no.statkart.sktools.gradle.testutils;

import org.gradle.util.GFileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SampleJarTestutil {
    private final static String PATH_SampleJar = "/sample.jar.bin";
    private final static String PATH_Sample2Jar = "/sample2.jar.bin";

    public static File writeSampleJar(File destinationFile) throws IOException {
        GFileUtils.parentMkdirs(destinationFile);
        Files.copy(SampleJarTestutil.class.getResourceAsStream(PATH_SampleJar), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return destinationFile;
    }

    public static File writeSample2Jar(File destinationFile) throws IOException {
        GFileUtils.parentMkdirs(destinationFile);
        Files.copy(SampleJarTestutil.class.getResourceAsStream(PATH_Sample2Jar), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return destinationFile;
    }

}
