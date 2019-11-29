Weblogic-deploy Gradle Plugin
------------------

Deploying artifacts and applications to Weblogic application containers.

Installation
------------

Build script snippet for new plugin DSL syntax:

    plugins {
        id 'sktools.weblogic-deploy' version '5.0'
    }

Build script snippet for use in all versions:

    buildscript {
        repositories {
            maven { url 'https://nexus.statkart.no/repository/public/' }
        }
        dependencies {
            classpath 'no.statkart.sktools.gradle:weblogic-deploy-plugin:5.0'
            // or
            classpath 'no.statkart.sktools.gradle:gradle-plugins:5.0'
        }

    }
    apply plugin: 'sktools-weblogic-deploy-plugin'


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

