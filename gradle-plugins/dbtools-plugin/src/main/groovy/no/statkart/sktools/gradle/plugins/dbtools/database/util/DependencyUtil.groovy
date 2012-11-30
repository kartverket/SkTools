package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import java.util.jar.JarFile
import java.util.jar.Manifest
import java.util.jar.Attributes
import org.gradle.api.Project

/**
 * @since 1.2
 * @author Leif Lislegård
 */
class DependencyUtil {

    private static Boolean _isClasspathExploded

    synchronized static boolean getIsClasspathExploded() {
        if (_isClasspathExploded == null) {
            String resourceName = "${DependencyUtil.class.name.replaceAll(/\./, '/')}.class"
            _isClasspathExploded = !DependencyUtil.class.classLoader.getResource(resourceName).toString().contains('!') //filer inne i jar-arkiver får utropstegn i URL
        }
        return _isClasspathExploded
    }

    /**
     * Gir deg dependenices avhengig av om det kjøres som test eller ikke.
     * Antakelse om at dersom man kjører ifra jar fil (ikke exploded) så er man i produksjonssammenheng.
     * @param project
     * @return
     */
    public static FileCollection getDatabasePatcherClasspath(Project project) {
        if (isClasspathExploded == false) {
            List<Dependency> dependencies = getDatabasePatcherDependencies(project)
            if (dependencies != null) {
                return project.buildscript.configurations.detachedConfiguration(dependencies.toArray(new Dependency[dependencies.size()]))
            }
            throw new RuntimeException("Feil ved beregning av dependencies for DatabasePatcher")
        } else {
            return findRutimeClasspathForTesting(project)
        }
    }

    static FileCollection findRutimeClasspathForTesting(Project project) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader()
            println 'Warning: Benytter beregnet classpath for testformaal i dbtools'
            return project.files(classLoader.properties['URLs'])
        } catch (IOException e1) {
            throw new RuntimeException("Feil ved beregning av classpath for testformaal");
        }
    }

    public static List<Dependency> getDatabasePatcherDependencies(Project project) {
        def version = getSktoolsVersion(project)
        if (version != null) {
            return [
                    project.buildscript.dependencies.create("no.statkart.sktools:db-tools:${version}"),
                    project.buildscript.dependencies.create("log4j:log4j:1.2.15@jar")   //todo: denne burde vert gitt via konfigurasjon
            ]
        }
        return null
    }

    /**
     * Finner versjon ved å lese manifest informasjon i lastede jar-filer i classloader.
     * @return versjon eller {@code null} dersom ingen relevante jar-filer eksisterer på classpath (IntelliJ)
     */
    public static String getSktoolsVersion(Project project) {
        Enumeration resEnum;
        try {
//            ClassLoader classLoader = Thread.currentThread().getContextClassLoader()
//            ClassLoader classLoader = DependencyUtil.getClass().getClassLoader();
            ClassLoader classLoader = DependencyUtil.getClassLoader();
            resEnum = classLoader.getResources(JarFile.MANIFEST_NAME);
            while (resEnum.hasMoreElements()) {
                try {
                    URL url = (URL) resEnum.nextElement();
                    InputStream is = url.openStream();
                    if (is != null) {
                        Manifest manifest = new Manifest(is);
                        Attributes mainAttributes = manifest.getMainAttributes();
                        if (mainAttributes.getValue("Implementation-Vendor") == 'Statens kartverk') {
                            String version = mainAttributes.getValue("Implementation-Version");
                            if (version != null) {
                                project.logger.info("Found sktools version: ${version}")
                                return version;
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException('Feil ved lesing av mainifest-informasjon', e);
                }
            }
        } catch (IOException e1) {
            throw new RuntimeException('Ukjent feil', e1);
        }
        return null;
    }

}
