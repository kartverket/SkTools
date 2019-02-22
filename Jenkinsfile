#!groovy

/*
 Multibranch Pipeline for Continuous integration (CI) prosess i Jenkins.
 Hver branch har denne innsjekket på fast plassering: Jenkinsfile

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
        gradle 'Gradle 4.10.2' //kompilerer artefakter til denne versjonen
        jdk 'Java 8 Latest' //spesifisert java versjon for bygging av release
    }

    environment {
        //legger gradle til byggenodens workspace - dette forhindrer kollisjoner i tilfeller der man har ibruk flyktige snapshot versjoner slik at to jobber kan komme i konflikt.
        //PS: erstatter '\' med '/' via char verdier da jenkins parser og kompilerer regex uttrykk på en håpløs måte...
        GRADLE_USER_HOME = "${WORKSPACE.replace(0x5c as char, 0x2f as char)}/gradle"
        WEBLOGIC_VERSION = "${params.WEBLOGIC_VERSION}"
        WEBLOGIC_HOME = "${WEBLOGIC_HOME("${params.WEBLOGIC_VERSION}", env)}"
        ORG_GRADLE_PROJECT_sktools_versjon = "${params.sktools_versjon}"
        BRANCH_NAME = "${params.BRANCH_NAME}"

        //for publisering til sentralt maven repo bines opp via jenkins credential (secret text)
        MAVEN_PUBLISH = credentials('MAVEN_DEPLOY_RELEASES')
    }

    stages {
        stage('Prepare') {
            steps {
                bat "gradle clean --refresh-dependencies ${gradleOptions(this)}"
            }
        }
        stage('Build') {
            steps {
                bat "gradle assemble publishToMavenLocal ${gradleOptions(this)}"
            }
        }

        stage('Unit tests') {
            parallel {
                stage('Test gradle baseline') {
                    steps {
                        bat "gradle --version"
                        bat "gradle testGradle4.10.2 -DignoreFailures=true ${gradleOptions(this)}"
                    }
                    post {
                        always {
                            junit '**/test-results/testGradle4.10.2/*.xml'
                        }
                    }
                }
                stage('Test gradle latest') {
                    tools {
                        gradle 'Gradle 4.10.2' //latest og greatest (kan også være neste major versjon)
                    }
                    steps {
                        bat "gradle --version"
                        bat "gradle testGradle4.10.2 -DignoreFailures=true ${gradleOptions(this)} -DbuildDirName=build/gradle4.10.2" //buildDirName for å kjøre flere bygg med forskjellige gradle versjoner
                    }
                    post {
                        always {
                            junit '**/test-results/testGradle4.10.2/*.xml'
                        }
                    }
                }
            }
        }

        stage('Integration tests') {
            parallel {
                stage('Integration Test Baseline') {
                    tools {
                        gradle 'Gradle 4.10.2' //spesifisert minstekrav
                    }
                    steps {
                        withEnv(['WEBLOGIC_VERSION=12.1.3.0', "WEBLOGIC_HOME=${WEBLOGIC_HOME('12.1.3.0', env)}"]) {
                            bat "gradle --version"
                            bat "gradle runDemos ${gradleOptions(this)}"
                        }
                    }
                }
                stage('Integration Test Latest') {
                    tools {
                        gradle 'Gradle 4.10.2' //latest og greatest (kan også være neste major versjon)
                        jdk 'Java 8 Latest' //weblogic krever denne major versjonen av java
                    }
                    steps {
                        sleep 5 //sleep time in seconds - helps seed randomness in choosing port# in database demos
                        withEnv(['WEBLOGIC_VERSION=12.1.3.0', "WEBLOGIC_HOME=${WEBLOGIC_HOME('12.1.3.0', env)}"]) {
                            bat "gradle --version"
                            bat "gradle runDemos ${gradleOptions(this)} -DbuildDirName=gradle4.10.2"
                        }
                    }
                }
            }
        }

        stage('Publish') {
            steps {
                bat "gradle publish ${gradleOptions(this)} --init-script config/gradle/scripts/mavenPublish.gradle"
            }
        }
    }

    post {
        always {
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
            echo 'build status changed'
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


static def gradleOptions(script) {
    return [
            "-PWEBLOGIC_VERSION=${script.env.WEBLOGIC_VERSION}",
            "-PWEBLOGIC_HOME=${WEBLOGIC_HOME(script.env.WEBLOGIC_VERSION, script.env)}",
            "-Dmaven.repo.local=${script.env.BASE}/.m2", //publiserer midlertidigt artefakt til mavenLocal for kjøring av releasetester
            '-Dorg.gradle.daemon=false',
            "-Djava.io.tmpdir=${script.pwd(tmp: true)}", //temp dir settes til samme mappe som jenkins (<workspace name>@tmp)
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
