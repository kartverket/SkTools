Filter Resources Gradle Plugin
------------------

Resources filtering substituting ant style @keys@ with values (gradle properties).

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.filter-resources' version '6.0'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:filter-resources-plugin:6.0'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:6.0'
        }

    }
    apply plugin: 'sktools-filter-resources-plugin'


Configuration
------------

    //optional configuration...
    filterResources {
      properties = propertyUtils.projectProperties()   //default is propertyUtils.projectProperties()
      properties myproperty:'myvalue'    //adds myproperty set of filtered properties
    }


