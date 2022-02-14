[WsDoc Gradle Plugin](src/main/groovy/no/statkart/sktools/gradle/plugins/wsdocgen/WsDocGenPlugin.java)
------------------

This plugin generates documentation from java sources implementing JAX-WS web services.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.wsdoc' version '5.7'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:wsdocgen-plugin:5.7'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:5.7'
        }
    }
    apply plugin: 'sktools-wsdocgen-plugin'


Changelog
------------
## Unreleased Changes

## 1.4.0 Release Notes
* [SKTOOLS-104] Fleksibel integrasjon med SourceSet

## 1.3.1 Release Notes
* [SKTOOLS-132] Dokumentasjon for webtjenester hentes nå også fra WSI interface

## 1.3.0 Release Notes
* [SKTOOLS-105] Støtte for generering av index.html
* [SKTOOLS-101] Omskrevet plugin til å benytte javac for annotasjonsprosessering. (Krever nå Java 6)

## 1.2.0 Release Notes
* Kompatibel med Gradle 1.0-rc-2

## 1.1.0 Release Notes
* Skrevet om plugin for bedre konfigurerbarhet og integrasjon. Pluginen kan nå gruppere webservicer samt velge sourcesett. Baserer seg nå på JavaBasePlugin.
* Legger nå automatisk med css stilsett
* Tester

## 1.0.0 Release Notes
* Lagt til dokumentasjon


Use
---
See [WsDocCompileTask](src/main/groovy/no/statkart/sktools/gradle/plugins/wsdocgen/WsDocCompileTask.java).

See an [XSLT example for html setup](src/test/resources/DefaultTransform.xsl).


Configuration
------------
    sourceSets {
        main {
            wsdoc.group {
                targetPath 'build/main/docs/wsdoc' //default
                include '**/*WSBean.java'          //default
                lookupPath '../../domain/javadoc'  //optional
                serviceXslt 'src/main/resources/wsdoc/service.xsl'  //stilsett for generering av service dokumentasjon
                indexXslt 'src/main/resources/wsdoc/index.xsl'      //valgfri generering av index.html
            }
        }
    }
