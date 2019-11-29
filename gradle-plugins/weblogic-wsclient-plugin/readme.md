Weblogic-wsclient Gradle Plugin
------------------

> PS: This plugin in deprecated and to be removed in version 6.0

Generates client stubs for JAX-WS web services.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.weblogic-wsclient' version '5.0'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:weblogic-wsclient-plugin:5.0'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:5.0'
        }

    }
    apply plugin: 'sktools-weblogic-wsclient-plugin'


Configuration
------------

    weblogicWsClient {
        webService {
            baseWar {
                project([path: ':wswar', configuration: 'weblogic'])
            }
            exceptionReusePackage 'reduce.to.this.pkg'
            lastWsdl 'StoreServiceWS.wsdl'
        }
    }

