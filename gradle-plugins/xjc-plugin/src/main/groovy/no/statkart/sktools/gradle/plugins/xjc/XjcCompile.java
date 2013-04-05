package no.statkart.sktools.gradle.plugins.xjc;


/**
 * Task for kompilerings-steg.
 *
 * Dette interfacet kan benyttes for å huke inn xjc funksjonalitet slik at:
 *
 * <pre><code>
 task('gen').description = "Genererte ressurser for alle sourceSets"

 //integrasjon med xjc plugin
 afterEvaluate {
   if (project.plugins.hasPlugin('sktools-xjc-plugin')) {
     project.tasks.withType(no.statkart.sktools.gradle.plugins.xjc.XjcCompile.class) {
       project.tasks.gen.dependsOn it
     }
   }
 }

 * </code></pre>
 *
 * }
 *
 *
 * @since 1.2
 * @author Leif Lislegård
 */
public interface XjcCompile {
}
