Wsdl Customizer Gradle Plugin
------------------

This plugin merges wsdl's with xsd files from domain model.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.wsdl-customizer' version '6.0'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:wsdl-customizer-plugin:6.0'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:6.0'
        }
    }
    apply plugin: 'sktools-wsdl-customizer-plugin'


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
    }



### Using weblogic-wswar
Når man bruker denne i samme modul som sktools-weblogic-wswar-plugin,
så tenger man ikke bruke generatedSchemas-konfigurasjonen.
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