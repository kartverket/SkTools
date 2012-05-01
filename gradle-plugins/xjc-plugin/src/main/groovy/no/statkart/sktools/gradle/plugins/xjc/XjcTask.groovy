package no.statkart.sktools.gradle.plugins.xjc

import org.gradle.api.GradleException

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory

/**
 * Eksekverer XJC task via ant.
 * Kobler inn evt plugin funksjonalitet i hht konfigurasjon av convention. Se {@link no.statkart.sktools.gradle.plugins.xjc.Schema#xjcOptions } for detaljer.
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
    List<Schema> schemas;

    @OutputDirectory
    File outputDirectory;

    @Input
    FileCollection classpath;


    XjcTask() {

    }


    @TaskAction
    def generate() {
        getOutputDirectory().mkdirs()

        ant.taskdef(name: 'xjc', classname: 'com.sun.tools.xjc.XJCTask', classpath: getClasspath().getAsPath())

        getSchemas().each { Schema s ->
            def antTask = ant.xjc(destDir: getOutputDirectory(), extension: !s.xjcOptions.isEmpty()) {
                if (s.xjcOptions.containsKey(Schema.GRUNNBOK_DOC)) {
                    Map params = s.xjcOptions.get(Schema.GRUNNBOK_DOC)
                    def args = params.values().join(' ')
                    arg(line: "-grunnbokDoc ${args}")
                }
                if (s.xjcOptions.containsKey(Schema.LIST_ADAPTER)) {
                    Map params = s.xjcOptions.get(Schema.LIST_ADAPTER)
                    def args = params.entrySet().collect {"${it.key}=${it.value}"}.join(' ')
                    arg(line: "-listgen ${args}")
                }
                schema(dir: s.dir) {
                    s.includes.each {
                        include(name:it)
                    }
                }
            }

            assert true; //debug point
        }

    }
}
