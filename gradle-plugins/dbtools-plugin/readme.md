DbTools Gradle Plugin
------------------

Plugin for database schema setup and maintenance.
Supported dialects are Oracle and HyperSQL (HSQLDB).

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.dbtools' version '6.0'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:dbtools-plugin:6.0'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:6.0'
        }

    }
    apply plugin: 'sktools-dbtools-plugin'


Configuration
------------

    configureDatabasePlugin {
        toolset(name:<toolset>, type:<oracle|hsqldb>, prefix:<prefix>) {
            sqlTask('CreateSchema', sqlFile: 'src/sql/createSchema.sql')
            sqlTask('CreateIndexes', sqlFile: 'src/sql/createIndexes.sql', failOnError:false) //continues execution of sql in case of error
            //config for toolset here...

            patch {
                //config for database patcher here...
            }
        }

        taskSequence('createViews') {
            dependsOn dbToolSets[<toolset>].tasks['CreateSchema']
            dependsOn dbToolSets[<toolset>].tasks['CreateIndexes']
            //....
        }
    }


