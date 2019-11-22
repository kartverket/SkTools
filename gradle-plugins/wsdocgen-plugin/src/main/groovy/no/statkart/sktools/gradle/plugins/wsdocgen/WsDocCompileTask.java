package no.statkart.sktools.gradle.plugins.wsdocgen;

import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.util.List;

/**
 * @author Leif Lislegård
 * @since 2.0
 */
public class WsDocCompileTask extends JavaCompile {
   protected static final Logger logger = Logging.getLogger(WsDocCompileTask.class);

   private WsDocGroup docGroup;

   /**
    * Gradle 1.2/2.0 - no arg constructor or @Inject annotated constructor
    */
   public WsDocCompileTask() {
      super();
       getLogging().captureStandardError(LogLevel.LIFECYCLE);
       getLogging().captureStandardOutput(LogLevel.DEBUG);
       getOptions().setWarnings(false);
   }

   /**
    * Initial input values - no mutation of state in {@code @TaskAction} [SKTOOLS-131]
    */
   public void init(WsDocGroup docGroup) {
      setDocGroup(docGroup);
      initCompilerArgs(getOptions().getCompilerArgs());
      initEncoding();

      if (getDocGroup().includes != null) {
         setIncludes(getDocGroup().includes); //up to date affects getSource()
      }
   }

   private void initEncoding() {
      String encoding = getEncoding();
      if (encoding != null && !encoding.isEmpty()) {
         getOptions().setEncoding(encoding);
      }
   }

   private void initCompilerArgs(final List<String> compilerArgs) {
      compilerArgs.add("-proc:only"); //only annotation processing is done, without any subsequent compilation.
      compilerArgs.add("-processor");
      compilerArgs.add("no.statkart.sktools.utils.wsdocgen.processor.WSDocProcessor"); //Names of the annotation processors to run. This bypasses the default discovery process.

      //-processorpath settes via CompileOptions#annotationProcessorPath (Gradle 3.4)

      final File xsl = getServiceXsltFile();
      if (!xsl.exists()) {
         throw new RuntimeException("xslt file not found: " + getProject().relativePath(xsl));
      }

      compilerArgs.add("-Axslt=" + xsl.getPath()); //xslt file

      if (getLookupPath() != null) {
         compilerArgs.add("-AjavaDocLookupPath=" + getLookupPath()); //lookup path
      }

      if (getIndexXsltFile() != null) {
         compilerArgs.add("-AindexXslt=" + getIndexXsltFile().getPath()); //SKTOOLS-105
      }
   }


   @TaskAction
   @Override
   protected void compile() {
      logger.info("args: {}", getOptions().getCompilerArgs());
      if (logger.isDebugEnabled()) {
         logger.debug("Classpath for generating WsDoc: {}", getClasspath().getFiles());
      }
      super.compile();
   }

   @Optional
   @Input //not up to date when changed
   public String getLookupPath() {
      return getDocGroup().lookupPath;
   }

   @Optional
   @Input
   public String getEncoding() {
      return getDocGroup().encoding;
   }

   @Override
   public CompileOptions getOptions() {
      final CompileOptions options = super.getOptions();
      options.setListFiles(logger.isDebugEnabled());
      options.setVerbose(logger.isInfoEnabled());

      return options;
   }

   @InputFile
   public File getServiceXsltFile() {
      if (getDocGroup().serviceXsltPath != null) {
         return getProject().file(getDocGroup().serviceXsltPath);
      } else {
         logger.warn("WARNING: no xslt file specified - using template for TESTING purposes..");
         WsDocGenConvention convention = (WsDocGenConvention) getProject().getConvention().getPlugins().get(WsDocGenPlugin.CONVENTION_NAME);
         return convention.generateTestFile(new File(getProject().getBuildDir(), "Transform.xsl")); //can't write to output dir because it gets wiped when not up to date...
      }
   }

   @Optional
   @InputFile
      //not up to date when change in file
   File getIndexXsltFile() {
      if (getDocGroup().indexXsltPath != null) {
         return getProject().file(getDocGroup().indexXsltPath);
      } else {
         return null; //optional null
      }
   }

   private WsDocGroup getDocGroup() {
      return docGroup;
   }

   private void setDocGroup(WsDocGroup docGroup) {
      this.docGroup = docGroup;
   }

   @Override
   public Logger getLogger() {
      return logger;
   }

}
