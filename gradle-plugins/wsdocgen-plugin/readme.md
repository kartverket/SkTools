WsDoc Gradle Plugin
------------------

This plugin generates documentation from java sources implementing JAX-WS web services.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.wsdoc' version '6.0'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:wsdocgen-plugin:6.0'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:6.0'
        }
    }
    apply plugin: 'sktools-wsdocgen-plugin'


Configuration
------------
    sourceSets {
        main {
            wsdoc {
                targetPath 'build/main/docs/wsdoc' //default
                include '**/*WSBean.java'          //default
                lookupPath '../../domain/javadoc'  //optional
                serviceXslt 'src/main/resources/wsdoc/service.xsl'  //stilsett for generering av service dokumentasjon
                indexXslt 'src/main/resources/wsdoc/index.xsl'      //valgfri generering av index.html
            }
        }
    }
