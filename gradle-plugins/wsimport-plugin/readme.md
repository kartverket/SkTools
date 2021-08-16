WsImport Gradle Plugin
------------------

This plugin generates client WS stubs from WSDL files.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.wsimport' version '6.0'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:wsimport-plugin:6.0'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:6.0'
        }
    }
    apply plugin: 'sktools-wsimport-plugin'


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
    }

