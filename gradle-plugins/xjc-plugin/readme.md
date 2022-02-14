[Xjc Gradle Plugin](src/main/groovy/no/statkart/sktools/gradle/plugins/xjc/XjcPlugin.groovy)
------------------

This plugin generates java model from `.xsd` schema files.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.xjc' version '1.5'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:xjc-plugin:1.5'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:1.5'
        }
    }
    apply plugin: 'sktools-xjc-plugin'

Changelog
------------
## Unreleased Changes

## 1.4.0 Release Notes
* [SKTOOLS-136] Oppdatert jaxb-xjc versjon til 2.2.11

## 1.3.0 Release Notes
* [SKTOOLS-92] Støtte for Gradle 1.12
* [SKTOOLS-10] Spesifisere path for generert output

## 1.2.0 Release Notes
* [SKTOOLS-28] Forbedret integrasjon med idea. Pluginen kompilerer nå bla. kildekoden selv
* [SKTOOLS-28] Baserer seg nå på JavaBasePlugin
* Kompatibel med Gradle 1.0

## 1.0.0 Release Notes
* Forbedret konfigurasjon og fleksibilitet. En har nå mulighet til å samle schema filer og konfigurasjon inn i schema {...} grupper mm.
* Lagt til dokumentasjon
* Tester


Bruk
----


Configuration
------------
    sourceSets {
        main.xjc {
            schema {
                srcDir 'src/main/xsd'
                genOutputPath = 'generated' //optional
                genTaskName = 'genXsd'  //optional
                compileTaskName = 'compileXsd' //optional
            }
        }
    }

    dependencies {
        jaxb 'org.glassfish.jaxb:jaxb-xjc:2.2.11', //default
             'org.glassfish.jaxb:jaxb-runtime:2.2.11' //default
             'com.sun.activation:javax.activation:1.2.0' //default
    }

### List adapter
Schema can be augmented with list adapter of choice:

        schema {
            ...
            withListAdapter 'custom.implemented.ListAdapter'
        }

### Grunnbok Doc

        schema {
            ...
            withGrunnbokDoc
        }