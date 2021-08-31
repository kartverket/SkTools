Xjc Gradle Plugin
------------------

This plugin generates java model from .xsd schema files.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.xjc' version '6.0'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:xjc-plugin:6.0'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:6.0'
        }
    }
    apply plugin: 'sktools-xjc-plugin'


Configuration
------------
    sourceSets {
        main.xjc {
            schema {
                srcDir 'src/main/xsd'
            }
        }
    }

    dependencies {
        jaxb 'org.glassfish.jaxb:jaxb-xjc:2.3.5' //default
        compileOnly 'org.glassfish.jaxb:jaxb-runtime:2.3.5' //default
    }

