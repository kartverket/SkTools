Webstart Gradle Plugin
------------------

Generering av webstart klienter og java applikasjons-distribusjoner.
Pluginen har funksjonalitet for generering av jnlp-filer, jar-ressurser, signering og enkel war distribuering.
Det forutsetter en Webstart med versjon 1.6 eller nyere.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.webstart' version '1.5'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:webstart-plugin:1.5'
            // or 
            classpath 'no.statkart.sktools.gradle:gradle-plugins:1.5'
        }
    }
    apply plugin: 'sktools-webstart-plugin'


Configuration
------------

    webstart {
        client {
            mainJar 'matrikkelklient'
            jarDependencies configurations.webstartLibs
            sign(keystore: keystore, alias: keystore_alias, password: keystore_password)
            jnlp {
                jnlpFilename 'matrikkeljava.jnlp'
                title "Matrikkelen ver. ${matrikkel_tjener_versjon}"
                version rootProject.getVersion()
                vendor 'Statens kartverk'
                homepage 'matrikkel.html'
                description 'Nasjonalt system for matrikkelføring i Norge.'
                resources {
                    javaRuntime '1.8.* 1.8+', '64m', '256m', 'http://java.sun.com/products/autodl/j2se'
                }
                application.mainClass 'no.statkart.matrikkel.presentasjon.mainframe.MainFrameLauncher'
                addServerURLArgument true
            }
        }
    }
          
