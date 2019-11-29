Ideaextensions Gradle Plugin
------------------

Customizing idea project files.
This plugin controls how the ipr, iws and iml files are generated.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.ideaextensions' version '5.0'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:ideaextensions-plugin:5.0'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:5.0'
        }

    }
    apply plugin: 'sktools-ideaextensions-plugin'


Configuration
------------
    ideaExtensions {
        vcs 'Perforce'
        inspectionProfiles += "sktools-idea-inspections.xml" //default inspection for java code
        codeStyle = "codestyle.xml" //project code style
        ignorePaths += '.gradle/'
    }

