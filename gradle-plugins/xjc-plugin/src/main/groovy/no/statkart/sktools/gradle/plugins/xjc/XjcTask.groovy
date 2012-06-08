package no.statkart.sktools.gradle.plugins.xjc

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory

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

    @Input
    XjcConfig config

    @OutputDirectory
    File outputDirectory;

    @Input
    FileCollection classpath;


    XjcTask() {

    }


    @TaskAction
    def generate() {
        //SKIF-195: cleaner generert source ved endringer
        getProject().delete(getOutputDirectory());

        getOutputDirectory().mkdirs()

        ant.taskdef(name: 'xjc', classname: 'com.sun.tools.xjc.XJCTask', classpath: getClasspath().getAsPath())

        getConfig().with { XjcConfig s ->
            def antTask = ant.xjc(destDir: getOutputDirectory(), extension: !s.xjcOptions.isEmpty()) {
                if (s.xjcOptions.containsKey(XjcConfig.GRUNNBOK_DOC)) {
                    Map params = s.xjcOptions.get(XjcConfig.GRUNNBOK_DOC)
                    def args = params.values().join(' ')
                    arg(line: "-grunnbokDoc ${args}")
                }
                if (s.xjcOptions.containsKey(XjcConfig.LIST_ADAPTER)) {
                    Map params = s.xjcOptions.get(XjcConfig.LIST_ADAPTER)
                    def args = params.entrySet().collect {"${it.key}=${it.value}"}.join(' ')
                    arg(line: "-listgen ${args}")
                }
                getSource().addToAntBuilder(ant, "schema", FileCollection.AntType.FileSet)
            }

            assert true; //debug point
        }

    }
}
