Weblogic-wswar Gradle Plugin
------------------

Generates web service modules for Weblogic Server.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.weblogic-wswar' version '1.5'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:weblogic-wswar-plugin:1.5'
            // or 
            classpath 'no.statkart.sktools.gradle:gradle-plugins:1.5'
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
          
