[WsImport Gradle Plugin](src/main/groovy/no/statkart/sktools/gradle/plugins/wsimport/WsImportPlugin.java)
------------------

This plugin generates client WS stubs from WSDL files.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.wsimport' version '7.0'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:wsimport-plugin:7.0'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:7.0'
        }
    }
    apply plugin: 'sktools-wsimport-plugin'


Changelog
------------
## Unreleased Changes

## 1.4.0 Release Notes
* [SKTOOLS-158, SKTOOLS-168] Etablert plugin


Bruk
----
Pluginet forutsetter at WSDL-ene allerede ligger inne som ressurser i main-sourceset.
Dette kan være fordi filene ligger inn som en statisk del av kildekoden, eller fordi filene blir trukket inn på en annen måte.
For å integrere med sktools-wsgen-plugin må man derfor implementere noe lim for å hente over WSDL-filene og tilhørende XSD-filer.

Java-plugin blir automatisk lagt til hvis den ikke allerede er det.
Output fra [wsimport](src/main/groovy/no/statkart/sktools/gradle/plugins/wsimport/WsImportTask.groovy)
tasken blir lagt til som Java sourceDir i `sourceSets.main`.
Katalogen markeres som generert kildekode i idea-plugin hvis det pluginet installeres.


| Type          | Name       | Description                                                                       |
|---------------|------------|-----------------------------------------------------------------------------------|
| WsImportTask  | `wsimport` | Genererer kildekode fra alle WSDL-filer funnet blant ressursene i main-sourceset. |
| Configuration | `jaxws`    | Implementasjon av 'jaxws-tools' verktøy, eller bruk default.                      |



Configuration
------------
    configurations {
        wsdls
    }

    dependencies {
        wsdls project(path: ':wswar', configuration: 'wsdls')
        jaxws 'com.sun.xml.ws:jaxws-tools:2.3.5' //default
    }

    // https://discuss.gradle.org/t/right-way-to-copy-contents-from-dependency-archives/7449/13
    task importWsdls(type: Sync) {
        dependsOn configurations.wsdls

        into ('wsdls') {
            from { configurations.wsdls.collect { zipTree(it) } }
        }

        into "$buildDir/wsdls/"
    }

    sourceSets.main.resources.srcDir importWsdls

    wsimport {
        exceptionReusePackage 'no.statkart.example.wsapi.v1.exception'
        lastWsdl 'wsdls/StoreServiceWS.wsdl'
        encoding = 'UTF-8' // default is utf8
    }

