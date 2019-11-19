Properties Gradle Plugin
------------------

Tool for convenient handling of properties. 

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.properties' version '1.5'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:properties-plugin:1.5'
            // or 
            classpath 'no.statkart.sktools.gradle:gradle-plugins:1.5'
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
          
