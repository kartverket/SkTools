package no.statkart.sktools.gradle.plugins.wsdocgen

import org.gradle.api.tasks.SourceSet
import org.gradle.util.GUtil

/**
 * Dokumentasjon av en logisk samling webservices.
 *
 * @since 1.1
 */
public class WsDocGroup {
   private final transient SourceSet sourceSet
   private final transient WsDocGenConvention convention

   /**
    * Navn for gruppe - blir automatisk tildelt dersom ikke spesifisert
    * @since 1.3
    */
   protected String name;

   protected Collection<String> includes;

   /**
    * Hvilket dir det skal legges til
    */
   protected Object targetPath

   protected String lookupPath

   protected String encoding

   protected def serviceXsltPath
   protected def indexXsltPath


   WsDocGroup(String name, SourceSet sourceSet, WsDocGenConvention convention) {
      this.name = name
      this.sourceSet = sourceSet
      this.convention = convention
   }

   protected WsDocGroup configure(Closure closure) {
      closure.setDelegate(this)
      closure.resolveStrategy = Closure.DELEGATE_FIRST
      closure()
      return this
   }

   /**
    * @since 1.1
    */
   WsDocGroup include(String... patterns) {
      if (includes == null) {
         includes = new ArrayList<String>();
      }
      includes.addAll(patterns);
      return this
   }

   /**
    * @since 1.1
    */
   WsDocGroup targetPath(Object path) {
      targetPath = path;
      return this;
   }

   /**
    * @since 1.1
    */
   WsDocGroup lookupPath(String relativePath) {
      lookupPath = relativePath;
      return this;
   }

   /**
    * @since 1.3
    */
   WsDocGroup xslt(Object path) {
      serviceXsltPath = path;
      return this;
   }

   /**
    * SKTOOLS-105
    * @see #xslt(Object)
    * @since 1.3
    */
   WsDocGroup serviceXslt(Object path) {
      return xslt(path);
   }

   /**
    * SKTOOLS-105
    * @since 1.3
    */
   WsDocGroup indexXslt(Object path) {
      indexXsltPath = path;
      return this;
   }

   WsDocGroup encoding(String encoding) {
      this.encoding = encoding;
      return this;
   }

   /**
    * @see WsDocGenConvention#GEN_TASK_NAME_PATTERN
    * @return {@link WsDocGenConvention#GEN_TASK_NAME_PATTERN}
    */
   public String getWsdocTaskName() {
      return "gen" + GUtil.toCamelCase(sourceSet.getName() + " Wsdoc " + name)
   }

}
