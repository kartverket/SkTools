Weblogic-wswar Gradle Plugin
------------------

> PS: This plugin in deprecated and to be removed in version 7.0
>     Please migrate your scripts to wsgen-plugin.

Generates web service modules for Weblogic Server.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.weblogic-wswar' version '6.0'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:weblogic-wswar-plugin:6.0'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:6.0'
        }
    }
    apply plugin: 'sktools-weblogic-wswar-plugin'


Configuration
------------

    dependencies {
        weblogicCompile project(':server-prosjekt')
    }

    sourceSets {
        weblogic {
            java.srcDir 'src/java'
        }
    }

    warWeblogic {
        from 'src/webapp'
    }

