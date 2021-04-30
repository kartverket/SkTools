package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import groovy.transform.PackageScope
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Task for patching av schema over JDBC.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
@SuppressWarnings("UnnecessaryQualifiedReference")
class PatchTask extends DatabasePatchTask {
    protected static final Logger logger = Logging.getLogger(PatchTask.class);

    @InputFile
    File sqlFile

    Boolean singlestep
    void setSinglestep(def value) {
        if (value != null) {
            singlestep = Boolean.parseBoolean("${value}")
        } else {
            singlestep = null
        }
    }

    @Internal
    boolean getSinglestep() {
        if (singlestep != null) {
            singlestep
        } else {
            project.gradle.startParameter.systemPropertiesArgs['singlestep']
        }
    }

    @TaskAction
    def exec() {
        File sqlFile = mappedSqlFile(getSqlFile())

        project.javaexec { JavaExecSpec spec ->

            /** {@link no.statkart.sktools.utils.databasepatcher.DatabasePatcher#main } */
            spec.setArgs(['patch', sqlFile.absolutePath])

            configureDefaultSpec(spec)

            spec.systemProperties.put('singlestep', getSinglestep())

            if (logger.isDebugEnabled()) {
                logger.debug('Executing databasepatcher with command: ' + (spec.getArgs() + spec.getAllJvmArgs()).join('\n\t'))
            }

        }
    }

    /**
     * Transforming file content by substituting tokens with properties.
     *
     * @see #fillInnProperties(java.lang.Iterable) for substitution rules.
     * @return a unique temp file with filtered content
     */
    @PackageScope
    File mappedSqlFile(File file = getSqlFile()) {
        if (file == null) return null;

        Charset charset = Charset.forName(getEncoding())
        logger.info('parsing statements from file: {}', file);
        List<String> transformedLines = fillInnProperties(Files.readAllLines(file.toPath(), charset))

        Path dir = Paths.get(getProject().buildDir.toString(), 'dbtools')
        Files.createDirectories(dir)
        Path tempFile = Files.createTempFile(dir, 'patch', '.sql')
        Files.write(tempFile, transformedLines, charset)

        return tempFile.toFile()
    }

    /**
     * Substitutes tokens with syntax {@code @property@} with property value.
     */
    @PackageScope
    List<String> fillInnProperties(Iterable<String> lines) {
        List<String> transformedLines = new ArrayList<>()
        for (String line : lines) {
            eachProperty({ key, value ->
                line = line.replace("@${key}@", value.toString())
            });
            transformedLines.add(line)
        }
        return transformedLines;
    }

    @Override
    void validate() {
        super.validate();

        if (getSqlFile() == null) {
            throw new Exception("sqlFile må angis!")
        }

        if (!getSqlFile().exists()) {
            throw new Exception("File does not exist! sqlFile=${project.relativePath(getSqlFile())}")
        }
    }


    public Logger getLogger() {
        return logger;
    }

}

