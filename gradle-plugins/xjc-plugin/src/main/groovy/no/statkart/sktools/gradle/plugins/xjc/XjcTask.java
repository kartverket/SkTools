package no.statkart.sktools.gradle.plugins.xjc;

import no.statkart.sktools.gradle.plugins.xjc.internal.XjcGenerator;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
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
    private final Property<File> outputDirectory;
    private FileCollection classpath;


    @Inject
    public XjcTask(ObjectFactory objectFactory, ProviderFactory providerFactory) {
        outputDirectory = objectFactory.property(File.class);
        outputDirectory.set(providerFactory.provider(() -> getConfig().genOutputPath.get()));
    }


    @TaskAction
    public void generate() {
        //SKIF-195: cleaner generert source ved endringer
        getProject().delete(getOutputDirectory());

        final XjcGenerator generator = new XjcGenerator(getProject()
                , getConfig() //kandidat for parameter objekt (klasse)
                , getSource()
                , getOutputDirectory().get()
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
    public Provider<File> getOutputDirectory() {
        return outputDirectory;
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
