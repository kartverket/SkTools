#!groovy

/*
 Multibranch Pipeline for Continuous integration (CI) prosess i Jenkins.
 Her branch har denne innsjekket på fast plassering: Jenkinsfile

 CI prosessen har følgende parameteriserbare dimensjoner:

   WEBLOGIC_VERSION : WEBLOGIC_HOME blir utledet via denne. Det kreves tilhørende env variabel "WEBLOGIC_HOME_${env.WEBLOGIC_VERSION}" peker til weblogic lib-katalog på byggenode
   sktools_versjon : versjon for publisering til nexus maven repo

 For jenkins pipeline documentation https://jenkins.io/doc/book/pipeline/
 TIP: Import DSL into your IntelliJ from http://jenkins.statkart.no:8021/jenkins/pipeline-syntax/gdsl

 Branch navn delegeres av Multibranch Pipeline som variabel BRANCH_NAME

 Polling skjer via oppsett av multibranch-jobb og defineres derfor ikke her i denne filen.

 Multibranch oppsett sjekker ut kodebasen automatisk. Denne havner som i roten til hvert workspace (som default).
*/

pipeline { //declarative pipeline syntax
    // agent defines where the pipeline will run.
    agent {
        // This also could have been 'agent any' - that has the same meaning as: label "".
        label ""
    }

    parameters {
        string(name: 'WEBLOGIC_VERSION', defaultValue: '12.1.3.0', description: 'Weblogic versjon. Det kreves tilhørende env variabel "WEBLOGIC_HOME_${env.WEBLOGIC_VERSION}" peker til weblogic lib-katalog på byggenode.')
        string(name: 'sktools_versjon', defaultValue: "${env.BRANCH_NAME ?: 'trunk'}-build${env.BUILD_NUMBER}", description: 'Versjon for publisert artefakt.')
        string(name: 'BRANCH_NAME', defaultValue: "${env.BRANCH_NAME ?: 'trunk'}", description: 'Branch for kildekode.')
    }

    tools {
        gradle 'Gradle 4.2' //kompilerer artefakter til denne versjonen
        jdk 'Java 7 Latest' //spesifisert java versjon for bygging av release
    }

    environment {
        //legger gradle til byggenodens workspace - dette forhindrer kollisjoner i tilfeller der man har ibruk flyktige snapshot versjoner slik at to jobber kan komme i konflikt.
        //PS: erstatter '\' med '/' via char verdier da jenkins parser og kompilerer regex uttrykk på en håpløs måte...
        GRADLE_USER_HOME = "${WORKSPACE.replace(0x5c as char, 0x2f as char)}/gradle"
        WEBLOGIC_VERSION = "${params.WEBLOGIC_VERSION}"
        WEBLOGIC_HOME = "${WEBLOGIC_HOME("${params.WEBLOGIC_VERSION}", env)}"
        ORG_GRADLE_PROJECT_sktools_versjon = "${params.sktools_versjon}"
        GRADLE_OPTS = "-XX:MaxPermSize=512m" //java 7 trenger litt mere permGen space
        BRANCH_NAME = "${params.BRANCH_NAME}"
    }

    stages {
        stage('Prepare') {
            steps {
                bat "gradle clean --refresh-dependencies ${gradleOptions(params, env)}"
            }
        }
        stage('Build') {
            steps {
                bat "gradle assemble install ${gradleOptions(params, env)}"
            }
        }

        stage('Unit test stage') {
            parallel {
                stage('Test gradle baseline') {
                    steps {
                        bat "gradle --version"
                        bat "gradle testGradle4.2 -DignoreFailures=true ${gradleOptions(params, env)}"
                        junit '**/test-results/testGradle4.2/*.xml'
                        //                            step([$class: 'Publisher', reportFilenamePattern: '**/build/reports/tests/testng-results.xml'])
                    }
                }
                stage('Test gradle latest') {
                    tools {
                        gradle 'Gradle 4.7' //latest og greatest (kan også være neste major versjon)
                    }
                    steps {
                        bat "gradle --version"
                        bat "gradle testGradle4.7 -DignoreFailures=true ${gradleOptions(params, env)} -DbuildDirName=build/gradle4.7" //buildDirName for å kjøre flere bygg med forskjellige gradle versjoner
                        junit '**/test-results/testGradle4.7/*.xml'
                        //                            step([$class: 'Publisher', reportFilenamePattern: '**/build/gradle4.7/reports/tests/testng-results.xml'])
                    }
                }
            }
        }

        stage('Integration test stage') {
            parallel {
                stage('Integration Test Baseline') {
                    tools {
                        gradle 'Gradle 4.2' //spesifisert minstekrav
                    }
                    steps {
                        withEnv(['WEBLOGIC_VERSION=10.3.5.0', "WEBLOGIC_HOME=${WEBLOGIC_HOME('10.3.5.0', env)}"]) {
                            bat "gradle --version"
                            bat "gradle runDemos ${gradleOptions(params, env)}"
                        }
                    }
                }
                stage('Integration Test Latest') {
                    tools {
                        gradle 'Gradle 4.7' //latest og greatest (kan også være neste major versjon)
                        jdk 'Java 8 Latest' //weblogic krever denne major versjonen av java
                    }
                    steps {
                        sleep 5 //sleep time in seconds - helps seed randomness in choosing port# in database demos
                        withEnv(['WEBLOGIC_VERSION=12.1.3.0', "WEBLOGIC_HOME=${WEBLOGIC_HOME('12.1.3.0', env)}"]) {
                            bat "gradle --version"
                            bat "gradle runDemos ${gradleOptions(params, env)} -DbuildDirName=gradle4.7"
                        }
                    }
                }
            }
        }

        stage('Publish') {
            steps {
                bat "gradle uploadArchives ${gradleOptions(params, env)}"
            }
        }
    }

    post {
        always {
            echo 'pelle **always**'

            //for mulig substituert innhold se https://github.com/jenkinsci/email-ext-plugin/tree/master/src/main/java/hudson/plugins/emailext/plugins/content
            emailext to: 'lislei@kartverket.no',
                    subject: '$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!',
                    replyTo: 'noreply@kartverket.no',
                    mimeType: 'text/html',
                    body: '''
<html>
<body>
You are receiving this email because <a href="$BUILD_URL">Build $BUILD_NUMBER $BUILD_CAUSE has been set to: $BUILD_STATUS</a>

<br>
<br>
<b>Test Results :</b><ul>
<li>Failed : ${TEST_COUNTS,var="fail"} / ${TEST_COUNTS}</li>
<li>Skipped: ${TEST_COUNTS,var="skip"} / ${TEST_COUNTS}</li>
<li>Passed : ${TEST_COUNTS,var="pass"} / ${TEST_COUNTS}</li>
</ul>
<pre>${FAILED_TESTS,showStack=false,maxTests=8}</pre>

<br>
<br>
<b>Changes since last success:</b> <br>
<pre>${CHANGES_SINCE_LAST_SUCCESS}</pre>

<br>
<br>
Build : $BUILD_URL <br>

</body>
</html>


''',
                    recipientProviders: [[$class: 'FirstFailingBuildSuspectsRecipientProvider'], [$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']]
        }

        changed {
            echo 'build status changed pelle'
        }
        failure {
            echo 'build Failed pelle'
        }
        success {
            echo 'build is a success pelle'
        }
        unstable {
            echo 'build is not good AKA unstable'
        }
    }

    // The options directive is for configuration that applies to the whole job.
    options {

        // Vi ønsker ikke å fylle opp jenkins master med logger og artefakter av gamle bygg
        buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '30', daysToKeepStr: '180', numToKeepStr: '90'))

        // Skip stages once the build status has gone to UNSTABLE
        skipStagesAfterUnstable()

        // And we'd really like to be sure that this build doesn't hang forever, so
        // let's time it out after an hour.
        timeout(time: 60, unit: 'MINUTES')

        // Prepend all console output generated by the Pipeline run with the time at which the line was emitted
        timestamps()
    }

}


static def gradleOptions(params, env) {
    return [
            "-PWEBLOGIC_VERSION=${env.WEBLOGIC_VERSION}",
            "-PWEBLOGIC_HOME=${WEBLOGIC_HOME(env.WEBLOGIC_VERSION, env)}",
            "-Dmaven.repo.local=${env.BASE}/.m2", //publiserer midlertidigt artefakt til mavenLocal for kjøring av releasetester
            '-Dorg.gradle.daemon=false',
            '--stacktrace'
    ].join(' ')
}

/**
 * Krever at byggenoder er satt opp med konvensjonell environment variabel knyttet til WEBLOGIC_VERSION
 */
static def WEBLOGIC_HOME(version, env) {
    def path = env."WEBLOGIC_HOME_${version}"
    Objects.requireNonNull(path, "Missing env var for '${'WEBLOGIC_HOME_' + version}' on Jenkins node!")
    return path
}

/**
 * Substituerer inn env verdier for vilkårlig streng med placeholdere på formen ${VAR}
 */
static def envExpand(value, env) {
//    def schema = env.environment.expand(value)
    value = value.replace('${EXECUTOR_NUMBER}', env.EXECUTOR_NUMBER)
    value = value.replace('${COMPUTERNAME}', env.COMPUTERNAME)
    value = value.replace('${NODE_NAME}', env.NODE_NAME)
    value = value.replace('${USERNAME}', env.USERNAME)
    return value
}
