Provided Gradle Plugin
------------------

Managing dependencies like mavens "provided" scope.

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

