WsGen Gradle Plugin
------------------

This plugin generates WSDL files from java sources implementing JAX-WS web services.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.wsgen' version '6.0'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:wsgen-plugin:6.0'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:6.0'
        }
    }
    apply plugin: 'sktools-wsgen-plugin'


Configuration
------------
    dependencies {
        jaxws 'com.sun.xml.ws:jaxws-tools:2.3.5' //default
        jaxws 'com.sun.xml.ws:wscompile:2.3.5' //old (not recommended)
    }

    war {
        into ('WEB-INF') {
            from tasks.genWsdls
        }
    }

