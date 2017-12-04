package no.statkart.sktools.gradle.plugins.wsdlcustomizer;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.util.PatternSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author Tor Egil R. Strand
 * @since 1.3
 */
public class WsdlCustomizerPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        Configuration originalSchemas = project.getConfigurations().create("originalSchemas");
        originalSchemas.setDescription("Configuration for consuming original schemas");
        originalSchemas.setVisible(false);

        Configuration generatedSchemas = project.getConfigurations().create("generatedSchemas");
        generatedSchemas.setDescription("Configuration for consuming generated schemas and WSDLs");
        generatedSchemas.setVisible(false);

        Configuration wsdls = project.getConfigurations().create("wsdls");
        wsdls.setDescription("Configuration for publishing processed WSDLs");
        wsdls.setVisible(true);

        Task schemasTask = configureSchemaExtractionTask(project, originalSchemas);
        Task wsdlsTask = configureWsdlExctractionTask(project, generatedSchemas);

        CustomWsdlTask customWsdlTask = configureWsdlCustomizerTask(project, schemasTask, wsdlsTask);

        Zip zipTask = configureZipTask(project, customWsdlTask);

        project.getArtifacts().add(wsdls.getName(), zipTask);
    }

    private Zip configureZipTask(Project project, CustomWsdlTask customWsdlTask) {
        Zip zipTask = project.getTasks().replace("zipCustomizedWsdls", Zip.class);
        zipTask.setClassifier("wsdls");
        zipTask.from(customWsdlTask);
        return zipTask;
    }

    private Copy configureSchemaExtractionTask(final Project project, final Configuration originalSchemas) {
        // Bruker replace() siden add() er depracated og create() ikke fantes før
        Copy extractSchemas = project.getTasks().replace("extractSchemas", Copy.class);
        extractSchemas.setDestinationDir(new File(project.getBuildDir(), extractSchemas.getName()));
        extractSchemas.dependsOn(originalSchemas);
        extractSchemas.from(new Callable<Collection<FileCollection>>() {
            @Override
            public Collection<FileCollection> call() throws Exception {
                Set<File> files = originalSchemas.getFiles();
                List<FileCollection> fileCollections = new ArrayList<FileCollection>(files.size());

                for (File file : files) {
                    fileCollections.add(project.zipTree(file));
                }

                return fileCollections;
            }
        });

        return extractSchemas;
    }

    private Copy configureWsdlExctractionTask(final Project project, final Configuration generatedSchemas) {
        final PatternSet wsdlAndXsdPattern = new PatternSet();
        wsdlAndXsdPattern.include("**/*.wsdl", "**/*.xsd");

        // Bruker replace() siden add() er depracated og create() ikke fantes før
        Copy extractWsdls = project.getTasks().replace("extractWsdls", Copy.class);
        extractWsdls.setDestinationDir(new File(project.getBuildDir(), extractWsdls.getName()));
        extractWsdls.dependsOn(generatedSchemas);
        extractWsdls.from(new Callable<Collection<FileCollection>>() {
            @Override
            public Collection<FileCollection> call() throws Exception {
                Set<File> files = generatedSchemas.getFiles();
                List<FileCollection> fileCollections = new ArrayList<FileCollection>(files.size());

                for (File file : files) {
                    fileCollections.add(project.zipTree(file).matching(wsdlAndXsdPattern));
                }

                return fileCollections;
            }
        });

        return extractWsdls;
    }

    private CustomWsdlTask configureWsdlCustomizerTask(Project project, Task schemaTask, Task wsdlTask) {
        CustomWsdlTask customWsdlTask = project.getTasks().replace("customizeWsdls", CustomWsdlTask.class);
        customWsdlTask.setDestinationDir(new File(project.getBuildDir(), customWsdlTask.getName()));
        customWsdlTask.originalSchemaFiles(schemaTask);
        customWsdlTask.generatedWsdlAndSchemaFiles(wsdlTask);
        return customWsdlTask;
    }
}
