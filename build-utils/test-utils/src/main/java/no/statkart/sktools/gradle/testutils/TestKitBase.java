package no.statkart.sktools.gradle.testutils;

import org.assertj.core.api.Assertions;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * Felles oppsett av Gradle Test Kit funksjonalitet.
 * Brukes for integrasjonstesting av plugin-logikk.
 */
public abstract class TestKitBase {
    public Path projectPath;
    public File projectDir;

    public static void assertNoFailures(BuildResult buildResult) {
        Assertions.assertThat(buildResult.getTasks())
            .extractingResultOf("getOutcome")
            .describedAs("Unexpected build error: %s", buildResult.getOutput())
            .doesNotContain(TaskOutcome.FAILED);
    }

    public BuildResult testGradleBuild(String... arguments) {
        @SuppressWarnings("UnnecessaryLocalVariable")
        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(arguments)
            .withPluginClasspath() //krever bruk av 'java-gradle-plugin'
            .withDebug(true) //debug av plugin-implementasjon
            .build();
        return result;
    }

    /**
     * @return ProjectBuilder koblet til {@link #projectDir}
     */
    public ProjectBuilder projectBuilder() {
        return ProjectBuilder.builder().withProjectDir(projectDir);
    }

    public File file(String relativePath) {
        return projectPath.resolve(relativePath).toFile();
    }

    public String rootProjectName() {
        return projectPath.getFileName().toString();
    }

    @BeforeMethod
    protected void createTempDir() throws IOException {
        projectPath = Files.createTempDirectory("sktoolsTest");
        projectPath = jenkinsWorkaroundForTempSetting(projectPath);
        projectDir = projectPath.toFile();
    }

    /**
     * Ved kjøring på jenkins 2.172.2 på Windows_Server_2008_R2 ble stasjonsnavnet i lowercase for temp-mappe.
     * Dette skaper problemer ved tekstlig sammenligning av gradle output som inneholder filstier.
     */
    private Path jenkinsWorkaroundForTempSetting(Path projectPath) {
        String path = projectPath.toString();
        path = Character.toUpperCase(path.charAt(0)) + path.substring(1).replaceAll("\\\\", "\\\\\\\\");
        return Paths.get(path);
    }

    @AfterMethod
    protected void deleteTempDir(ITestResult testResult) throws IOException {
        if (projectPath != null) {
            if (testResult.isSuccess()) {
                deleteRecursively(projectPath);
            } else {
                System.err.println("Test failed! Leaving generated files in directory " + file(""));
            }
        }
    }

    protected File writeFile(String relativePath, CharSequence... lines) throws IOException {
        return writeFile(toPath(relativePath), lines);
    }

    protected File writeFile(Path destination, CharSequence... lines) throws IOException {
        Files.createDirectories(destination.getParent());
        Files.write(destination, Arrays.asList(lines), StandardCharsets.UTF_8,
            StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return destination.toFile();
    }

    protected File writeFile(String relativePath, InputStream is) throws IOException {
        return writeFile(toPath(relativePath), is);
    }

    protected File writeFile(Path destination, InputStream is) throws IOException {
        Files.createDirectories(destination.getParent());
        Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
        return destination.toFile();
    }


    protected void writeGradleProperties(Map<Object, Object> properties) throws IOException {
        final File file = file("gradle.properties");
        file.createNewFile();
        try (final FileOutputStream os = new FileOutputStream(file)) {
            final Properties p = new Properties();
            p.putAll(properties);
            p.store(os, "");
        }
    }


    private Path toPath(String relativePath) {
        return projectPath.resolve(relativePath.replaceAll("/", File.separator.replace("\\", "\\\\")));  //escape backslash on windows
    }

    //kan erstattes med guava MoreFiles.deleteRecursively
    private static void deleteRecursively(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static final Map<Object, Object> testProperties;
    static {
        Properties properties = new Properties();
        try {
            properties.load(TestKitBase.class.getResourceAsStream("/no/statkart/sktools/test.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        testProperties = Collections.unmodifiableMap(properties);
    }

    //Workaround until Gradle 5 / Groovy 2.5
    static {
        try {
            Class.forName("no.statkart.sktools.gradle.Workaround");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
