[Filter Resources Gradle Plugin](src/main/java/no/statkart/sktools/gradle/plugins/filterresources/FilterResourcesPlugin.java)
------------------

Resources filtering substituting ANT style `@keys@` tokens with values (gradle properties).

Pluginen baserer seg på JavaBasePlugin og utvider [SourceSet](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceSet.html)
med mulighet til filtrering av ressursfiler.

Det er mulig å konfigurere hvilke properties som blir filtrert. Se properties felt i konfigurasjonen.
Dersom ingen properties angis benyttes Project.properties som default.


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


Changelog
------------
## Unreleased Changes

## 1.3.0 Release Notes
 * [SKTOOLS-44] Nytt forenklet plugin (splittet ut fra tidligere 'filterproperties-plugin')



Configuration
------------

```groovy
sourceSets {
    main {
        //optional configuration...
        filterResources {
            properties = propertyUtils.projectProperties()   //default is propertyUtils.projectProperties()
            properties myproperty:'myvalue'    //adds myproperty to set of filtered properties
        }
        output.filterResourcesOutput 'gen/main/resources' //optional hardcoded placement of output files
    }
}
```

