[WsGen Gradle Plugin](src/main/groovy/no/statkart/sktools/gradle/plugins/wsgen/WsdlGenPlugin.java)
------------------

This plugin generates WSDL files from java sources implementing JAX-WS web services.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.wsgen' version '7.0'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:wsgen-plugin:7.0'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:7.0'
        }
    }
    apply plugin: 'sktools-wsgen-plugin'


Changelog
------------
## Unreleased Changes

## 7.0 Release Notes
* [SKTOOLS-247] Jakarta EE8+ kompatibilitet
* [SKTOOLS-247] Endret WsdlGenTask.`destinationDirectory` til provider (het før `destinationDir`)
* [SKTOOLS-246] Bumper standard jaxws versjon til `2.3.6`

## 5.3 Release Notes
* [SKTOOLS-210] java.lang.ClassNotFoundException: com.sun.tools.ws.WsGen
* Gradle 6 compatibility
* JDK 11 compatibility

## 1.4.0 Release Notes
* [SKTOOLS-156, SKTOOLS-159] Etablert plugin


Bruk
----
Pluginet krever ingen konfigurasjon i seg selv utover at en JAX-WS tools-implementasjon må ligge i `jaxws`-configuration,
men gjør heller ikke noen nytte av seg selv heller.
Det krever dog at alle tjenestene den skal prosessere har navn som ender med WSBean.
Dersom noen klasser med slik navn ikke skal prosesseres, så kan man legge på exclude på genWsdl-tasken.

War-pluginet blir trukket inn av dette pluginet, og det er den som gjør mesteparten av jobben.
I likhet med andre tilfeller hvor vi lager war-filer direkte, så må vi, dersom war-filen skal inn i en ear-fil,
huske å gjøre de krumspringene som skal til for at war-filen ikke inneholder alle avhengigheter som vi senere legger i ear-fil.
I tillegg må war-fil eksponeres gjennom en konfigurasjon ear-prosjektet kan trekke inn.

Man må selv gjøre det som trengs for sende WSDL-ene fra `genWsdls` dit man har bruk for dem.

## Dependency håndtering
Pluginen legger til en konfigurasjon med navn `jaxws`.
Her må det ligge noe som inneholder implementasjon av Ant-tasken wsgen. Dersom ikke noe deklareres så legges det
ved en nyere versjon av `jaxws-tools`.


Configuration
------------
    dependencies {
        jaxws 'com.sun.xml.ws:jaxws-tools:2.3.6' //default
        jaxws 'com.sun.xml.ws:wscompile:2.3.6' //old (not recommended)
    }

    war {
        into ('WEB-INF') {
            from tasks.genWsdls
        }
    }

