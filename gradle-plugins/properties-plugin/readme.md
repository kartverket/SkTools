[Properties Gradle Plugin](src/main/java/no/statkart/sktools/gradle/plugins/properties/PropertiesPlugin.java)
------------------

Tool for convenient handling of properties.

Activating this plugin adds an [`propertyUtils` project extension](src/main/java/no/statkart/sktools/gradle/plugins/properties/extension/PropertyUtils.java).

Changelog
------------
## Unreleased Changes

## 1.3.0 Release Notes
 * [SKTOOLS-44] Nytt forenklet plugin (splittet ut fra tidligere 'filterproperties-plugin')


Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.properties' version '6.0'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:properties-plugin:6.0'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:6.0'
        }

    }
    apply plugin: 'sktools-properties-plugin'



Use
----
    propertyUtils.assignPropertiesToProject fromFile('build-user.properties') << gradle.startParameter.projectProperties

    //konfigurerer andre property verdier før ekspandering her...

    //husk at alle verdier som skal substitueres ved ekspansion må være satt!
    propertyUtils.propertyUtils.expandProjectProperties()

    subprojects {
      ...
    }

