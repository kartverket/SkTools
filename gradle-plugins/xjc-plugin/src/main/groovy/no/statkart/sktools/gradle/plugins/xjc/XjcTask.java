package no.statkart.sktools.gradle.plugins.xjc;

import no.statkart.sktools.gradle.plugins.xjc.internal.XjcGenerator;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

/**
 * Eksekverer XJC task via ant.
 * Kobler inn evt plugin funksjonalitet i hht konfigurasjon av sourceSet. Se {@link XjcConfig } for detaljer.
 *
 * <p>
 * Følgende plugin funksjonalitet er implementert:
 * <ul>
 *     <li>com.sun.tools.xjc.addon.statkart.GrunnbokDocPlugin
 *     <li>com.sun.tools.xjc.addon.statkart.ListGenPluginTest
 * </ul>
 *
 *
 * Funksjonalitet implementeres i :build-utils:xjc-plugins modul
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class XjcTask extends SourceTask {
    private XjcConfig config;
    private File outputDirectory;
    private FileCollection classpath;


    /**
     * Gradle 1.2/2.0 - no arg constructor or @Inject annotated constructor
     */
    public XjcTask() {
    }


    @TaskAction
    public void generate() {
        //SKIF-195: cleaner generert source ved endringer
        getProject().delete(getOutputDirectory());

        final XjcGenerator generator = new XjcGenerator(getProject()
                , getConfig() //kandidat for parameter objekt (klasse)
                , getSource()
                , getOutputDirectory()
                , getClasspath()
        );

        generator.gen();
    }


    @Nested //SKTOOLS-128: kun interessert i enkelte felter
    public XjcConfig getConfig() {
        return config;
    }

    public void setConfig(XjcConfig config) {
        this.config = config;
    }

    @OutputDirectory
    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @CompileClasspath
    public FileCollection getClasspath() {
        return classpath;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setClasspath(FileCollection classpath) {
        this.classpath = classpath;
    }

}
