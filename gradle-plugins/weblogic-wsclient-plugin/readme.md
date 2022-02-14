[Weblogic-wsclient Gradle Plugin](src/main/groovy/no/statkart/sktools/gradle/plugins/weblogic/wsclient/WeblogicWsClientPlugin.groovy)
------------------

> PS: This plugin in deprecated and to be removed in version 6.0

Generates client stubs for JAX-WS web services.

See [WeblogicGenClientTask](src/main/groovy/no/statkart/sktools/gradle/plugins/weblogic/wsclient/WeblogicGenClientTask.java)
for code generation.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.weblogic-wsclient' version '5.7'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:weblogic-wsclient-plugin:5.7'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:5.7'
        }

    }
    apply plugin: 'sktools-weblogic-wsclient-plugin'


Changelog
------------
## Unreleased Changes
 * [SKTOOLS-199] Vertkøyet basert på Weblogic tooling utgår til fordel for sktools-wsimport-plugin

## 1.3.0 Release Notes
 * [SKTOOLS-94] Støtte for å angi hvilken wsdl som skal kompileres sist
 * [SKIF-79] `collectSchemaIfNotSpecified` og `genWsClientSource` er ikke up-to-date når den burde
   Medførte liten endring i måten plugin-et konfigureres

## 1.2.0 Release Notes
 * [SKIF-197] Forbedret integrasjon med idea - generert kode blir nå kompilert og lagt i separate kataloger
 * [SKIF-197] Forbedrer også integrasjon med JavaPlugin. Generert kode blir nå lagt til sourceSet.main slik at man kan benytte standard artifakt håndtering mm
 * [SKIF-194] Retter feil med exceptionReusePackage
 * [SKIF-213] Anbefaler ikke bruk av discriminator
 * Kompatibel med Gradle 1.0-rc-2

## 1.1.0 Release Notes
 * Redefinert konfigurasjon, denne er helt omskrevet. Blandt annet er exception konfigurasjonen simplifisert, og man kan dele webservice konfigurasjoner inn grupper.
 * Lagt til exceptionReusePackage felt for enkel redusering av exceptions.
 * Refaktorert plugin til valgfritt å kunne integreres med JavaPlugin. Baserer seg nå på JavaBasePlugin.
 * Forenklet dependency konfigurasjon.
 * Compile og resource tasks får nå satt source og target filer per konvensjon.
 * Tester

## 1.0.0 Release Notes
* Lagt til dokumentasjon



Use
---

Generated source code will be added to `souceSets.main`.

## Dependency management
A configuration `weblogicProvided` is added by this plugin.
Add weblogic classpath to this.

### tools.jar
Weblogic 10.3.5 and newer requires `tools.jar` on the classpath.
When not present then exceptions like `"[ant:clientgen] Exception in thread "main" com.sun.xml.ws.util.ServiceConfigurationError"` can occur.




Configuration
------------

    weblogicWsClient {
        webService {
            baseWar {
                project([path: ':wswar', configuration: 'weblogic'])
            }
            exceptionReusePackage 'reduce.to.this.pkg'
            lastWsdl 'StoreServiceWS.wsdl'
        }
    }

    task('gen').description = "Genererte ressurser"

    afterEvaluate {
        if (project.plugins.hasPlugin('sktools-weblogic-wsclient-plugin')) {
            project.tasks.withType(no.statkart.sktools.gradle.plugins.weblogic.wsclient.WeblogicWsClientCompileTask.class) {
                project.tasks.gen.dependsOn it
            }
        }
    }
