[Weblogic-deploy Gradle Plugin](src/main/groovy/no/statkart/sktools/gradle/plugins/weblogic/deploy/WeblogicDeployPlugin.java)
------------------

Deploying artifacts and applications to Weblogic application containers.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.weblogic-deploy' version '1.5'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:weblogic-deploy-plugin:1.5'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:1.5'
        }

    }
    apply plugin: 'sktools-weblogic-deploy-plugin'


Changelog
------------
## Unreleased Changes

## 1.4.0 Release Notes
 * SKTOOLS-147 - Timeout verdi for Deploy til WebLogic 12.1.3

## 1.3.0 Release Notes
 * Nye tasker for start og stopp
 * Støtte for versjonering
 * Støtte for biblioteker

## 1.2.0 Release Notes
* Plugin opprettet


Use
---

Either use the tasks by conventional [extention](src/main/groovy/no/statkart/sktools/gradle/plugins/weblogic/deploy/WeblogicDeployConfiguration.groovy)
or standalone setup up the following tasks:

* [WeblogicDeployTask](src/main/groovy/no/statkart/sktools/gradle/plugins/weblogic/deploy/WeblogicDeployTask.groovy)
    - Deployer en applikasjon eller et bibliotek til WebLogic.
* [WeblogicUndeployTask](src/main/groovy/no/statkart/sktools/gradle/plugins/weblogic/deploy/WeblogicUndeployTask.groovy)
    - Undeployer en applikasjon eller et bibliotek fra WebLogic.
* [WeblogicStartTask](src/main/groovy/no/statkart/sktools/gradle/plugins/weblogic/deploy/WeblogicStartTask.groovy)
    - Starter en stoppet deployet applikasjon i WebLogic.
* [WeblogicStopTask](src/main/groovy/no/statkart/sktools/gradle/plugins/weblogic/deploy/WeblogicStopTask.groovy)
    - Stopper en kjørende applikasjon i WebLogic.

Configuration
------------

    weblogicDeploy {
        classpath = libraries.weblogic
        url(host: matrikkel_server_deploy_ip, port: matrikkel_server_deploy_port, protocol: matrikkel_server_deploy_protocol)
        targets = matrikkel_server_deploy_targets
        username = matrikkel_server_deploy_username
        password = matrikkel_server_deploy_password

        file = ear //using ear plugin
        name = 'matrikkelear'

        undeployTask('undeploy', description: "Undeployer ${name} til ${url}")

        deployTask('deploy', dependsOn: [undeployTask], description: "Deployer ${name} til ${url}") {
            failOnError = true
            timeout = 1200000
        }
    }

