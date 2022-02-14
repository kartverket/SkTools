[Provided Gradle Plugin](src/main/java/no/statkart/sktools/gradle/plugins/provided/ProvidedPlugin.java)
------------------

Managing dependencies analogue to "provided" scope in Maven.

Adds a `configurations.provided` that extends `compileOnly` and `testImplementation`.


Changelog
------------
## Unreleased Changes

## 1.3.1 Release Notes
 * SKTOOLS-137 Configuration for single-vm

## 1.2.0 Release Notes
* [SKTOOLS-54] Etablert plugin


Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.provided' version '6.0'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:provided-plugin:6.0'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:6.0'
        }

    }
    apply plugin: 'sktools-provided-plugin'


Use
----
    dependencies {
        provided 'javax.mail:mail:1.4.4' //part of JEE
    }

