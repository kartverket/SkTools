package no.statkart.sktools.gradle.plugins.xjc.internal

import no.statkart.sktools.gradle.plugins.xjc.XjcConfig
import org.gradle.api.AntBuilder
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree

class XjcGenerator {
   final Project project;
   final XjcConfig config;
   final FileTree source;
   final File outputDirectory;
   final FileCollection classpath;

   XjcGenerator(Project project, XjcConfig config, FileTree source, File outputDirectory, FileCollection classpath) {
      this.project = project
      this.config = config
      this.source = source
      this.outputDirectory = outputDirectory
      this.classpath = classpath
   }

   void gen() {
      final AntBuilder ant = project.getAnt()
      ant.taskdef(name: 'xjc', classname: 'com.sun.tools.xjc.XJCTask', classpath: getClasspath().getAsPath())

      getOutputDirectory().mkdirs()

      getConfig().with { XjcConfig s ->
         def antTask = ant.xjc(destDir: getOutputDirectory(), extension: !s.xjcOptions.isEmpty()) {
            arg(line: "-no-header") //SKTOOLS-172: fjerner timestamp og JAXB versjon brukt fra generert kildekode
            s.xjcOptions.each { k, v ->
               switch (k) {
                  case GRUNNBOK_DOC:
                     Map params = s.xjcOptions.get(GRUNNBOK_DOC)
                     def args = params.values().join(' ')
                     arg(line: "-grunnbokDoc ${args}")
                     break
                  case LIST_ADAPTER:
                     Map params = s.xjcOptions.get(LIST_ADAPTER)
                     def args = params.entrySet().collect { "${it.key}=${it.value}" }.join(' ')
                     arg(line: "-listgen ${args}")
                     break
                  default:
                     arg(line: "-${k}")
                     if (!v.isEmpty()) {
                        arg(line: v)
                     }
                     break
               }
            }
            getSource().addToAntBuilder(ant, "schema", FileCollection.AntType.FileSet)
         }
         assert true; //debug point
      }
   }

}
