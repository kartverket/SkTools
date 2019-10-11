package no.statkart.sktools.gradle.testutils;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

/**
 * Felles oppsett av Gradle Test Kit funksjonalitet.
 * Brukes for integrasjonstesting av plugin-logikk.
 *
 */
public abstract class TestKitBase {
    public Path projectPath;
    public File projectDir;

    public static void assertNoFailures(BuildResult buildResult) {
        Assertions.assertThat(buildResult.getTasks())
                .extractingResultOf("getOutcome")
                .doesNotContain(TaskOutcome.FAILED);

    }

    public BuildResult testGradleBuild(String... arguments) {
        @SuppressWarnings("UnnecessaryLocalVariable")
        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(arguments)
                .withPluginClasspath() //krever bruk av 'java-gradle-plugin'
                .withDebug(true)
                .build();
        return result;
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
        projectDir = projectPath.toFile();
    }

    @AfterMethod
    protected void deleteTempDir() throws IOException {
        if (projectPath != null) {
            deleteRecursively(projectPath);
        }
    }

    protected void writeFile(String relativePath, CharSequence... lines) throws IOException {
        Path path = projectPath.resolve(relativePath.replaceAll("/", File.separator.replace("\\", "\\\\"))); //escape backslash on windows
        writeFile(path, lines);

    }
    protected void writeFile(Path destination, CharSequence... lines) throws IOException {
        Files.createDirectories(destination.getParent());
        Files.write(destination, Arrays.asList(lines), StandardCharsets.UTF_8,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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

}
