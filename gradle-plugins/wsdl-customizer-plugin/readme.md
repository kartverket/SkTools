[Wsdl Customizer Gradle Plugin](src/main/java/no/statkart/sktools/gradle/plugins/wsdlcustomizer/WsdlCustomizerPlugin.java)
------------------

This plugin merges wsdl's with xsd files from domain model.
See [CustomWsdlTask](src/main/java/no/statkart/sktools/gradle/plugins/wsdlcustomizer/CustomWsdlTask.java)

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.wsdl-customizer' version '5.7'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:wsdl-customizer-plugin:5.7'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:5.7'
        }
    }
    apply plugin: 'sktools-wsdl-customizer-plugin'


Changelog
------------
## Unreleased Changes

## 1.4.0 Release Notes
* SKTOOLS-171: WSDL-customizer plugin håndterer ikke xsd-filer for flere webservices i samme namespace

## 1.3.1 Release Notes
* SKTOOLS-139 bugfix: Kall til feil metode i WsdlCustomizerPlugin.configureWsdlExctractionTask

## 1.3.0 Release Notes
* Plugin opprettet i SKTOOLS-115


Use
---

| Type           | Name                 | Description                                                                                          |
|----------------|----------------------|------------------------------------------------------------------------------------------------------|
| Configuration  | `originalSchemas`    | Inneholder zip-filer med håndskrevne XML-skjemafiler.                                                |
| Copy task      | `extractSchemas`     | Pakker ut zip-filene i `originalSchemas`.                                                            |
| Configuration  | `generatedSchemas`   | Inneholder zip-filer (war-filer) med genererte WSDL-er og tilhørende XML-skjemafiler.                |
| Copy task      | `extractWsdls`       | Pakker ut zip-filene i `generatedSchemas`.                                                           |
| CustomWsdlTask | `customizeWsdls`     | Bytter ut referanser i WSDL-er med håndskrevne skjemaer der det finnes.                              |
| Zip task       | `zipCustomizedWsdls` | Pakker tilpassede WSDL-er, håndskrevne skjemafiler og de nødvendige genererte skjemafilene i en zip. |


Configuration
------------

### Using wsgen

    apply plugin: 'sktools-wsgen-plugin'
    apply plugin: 'sktools-wsdl-customizer-plugin'

    customizeWsdls {
        dependsOn genWsdls
        generatedWsdlAndSchemaFiles files(tasks.genWsdls.destinationDir).asFileTree.matching {
            include '**/*.wsdl', '**/*.xsd'
            exclude '**/*Internal*ServiceWS*'
        }

        includeNamespaces(
              'http://matrikkel.statkart.no/matrikkelapi/wsapi/v1/exception',
              'http://matrikkel.statkart.no/matrikkelapi/wsapi/v1/domain',
              ...
        )
        excludeNamespaces(
            // Angir namespaces som ikke skal med (av de som er included)
        )
    }



### Using weblogic-wswar
Når man bruker denne i samme modul som sktools-weblogic-wswar-plugin,
så trenger man ikke bruke generatedSchemas-konfigurasjonen.
Merk at man i dette eksempelet må ha satt opp `example-v1-wsschema`
til å publisere et zip-artefakt med skjemafilene i en schemas-konfigurasjon.

    apply plugin: 'sktools-weblogic-wswar-plugin'
    apply plugin: 'sktools-wsdl-customizer-plugin'

    dependencies {
        originalSchemas project(path: ':example-v1-wsschema', configuration: 'schemas')
    }

    customizeWsdls {
        dependsOn genWeblogic
        generatedWsdlAndSchemaFiles files(tasks.genWeblogic.destinationDir).asFileTree.matching {
            include '**/*.wsdl', '**/*.xsd'
            exclude '**/TestdataServiceWS*'
        }
        includeNamespaces(
                'http://example.com/wsapi/v1/exception',
                ...
        )
    }