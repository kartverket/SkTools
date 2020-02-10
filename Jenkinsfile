#!groovy

/*
 Multibranch Pipeline for Continuous integration (CI) prosess i Jenkins.
 Hver branch har denne innsjekket på fast plassering: Jenkinsfile

 CI prosessen har følgende parameteriserbare dimensjoner:

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
        label 'sktools||matrikkel'
    }

    parameters {
        string(name: 'sktools_versjon', defaultValue: "${env.BRANCH_NAME ?: 'trunk'}-build${env.BUILD_NUMBER}", description: 'Versjon for publisert artefakt.')
        string(name: 'BRANCH_NAME', defaultValue: "${env.BRANCH_NAME ?: 'trunk'}", description: 'Branch for kildekode.')
    }

    tools {
        gradle 'Gradle 6.0' //kompilerer artefakter mot denne API-versjonen
        jdk 'Java 12 Latest' //spesifisert java versjon for bygging av release
    }

    environment {
        //legger gradle til byggenodens workspace - dette forhindrer kollisjoner i tilfeller der man har ibruk flyktige snapshot versjoner slik at to jobber kan komme i konflikt.
        //PS: erstatter '\' med '/' via char verdier da jenkins parser og kompilerer regex uttrykk på en håpløs måte...
        GRADLE_USER_HOME = "${WORKSPACE.replace(0x5c as char, 0x2f as char)}/gradle"
        ORG_GRADLE_PROJECT_sktools_versjon = "${params.sktools_versjon}"
        BRANCH_NAME = "${params.BRANCH_NAME}"

        //for publisering til sentralt maven repo bindes opp via jenkins credential (secret text)
        MAVEN_PUBLISH = credentials('MAVEN_DEPLOY_RELEASES')
    }

    stages {
        stage('Prepare') {
            steps {
                sh "gradle clean --refresh-dependencies ${gradleOptions(this)}"
            }
        }
        stage('Build') {
            steps {
                sh "gradle assemble ${gradleOptions(this)}"
            }
        }

        stage('Unit tests') {
            parallel {
                stage('Test gradle baseline') {
                    tools {
                        jdk 'Java 8 Latest'  //tester med spesifiserte minstekrav
                    }
                    steps {
                        sh "gradle --version"
                        sh "gradle testGradle6.0 -DignoreFailures=true ${gradleOptions(this)}"
                    }
                    post {
                        always {
                            junit '**/test-results/testGradle6.0/*.xml'
                        }
                    }
                }
                stage('Test gradle latest') {
                    tools {
                        gradle 'Gradle 6.0.1' //latest og greatest (kan også være neste major versjon)
                    }
                    steps {
                        sh "gradle --version"
                        sh "gradle testGradle6.0.1 -DignoreFailures=true ${gradleOptions(this)} -DbuildDirName=build/gradle6.0.1" //buildDirName for å kjøre flere bygg med forskjellige gradle versjoner
                    }
                    post {
                        always {
                            junit '**/test-results/testGradle6.0.1/*.xml'
                        }
                    }
                }
            }
        }

        stage('Publish') {
            steps {
                sh "gradle publish ${gradleOptions(this)} --init-script config/gradle/scripts/mavenPublish.gradle"
            }
        }
    }

    post {
        always {
            //for mulig substituert innhold se https://github.com/jenkinsci/email-ext-plugin/tree/master/src/main/java/hudson/plugins/emailext/plugins/content
            emailext to: 'lislei@kartverket.no',
                    subject: '$JOB_NAME - build# $BUILD_NUMBER - $BUILD_STATUS',
                    replyTo: 'noreply@kartverket.no',
                    mimeType: 'text/html',
                    body: '''
<html>
<body>
You are receiving this email because $PROJECT_NAME build <a href="$BUILD_URL">$JOB_NAME #$BUILD_NUMBER</a> has been set to: $BUILD_STATUS

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

        disableConcurrentBuilds()

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
            '-Dorg.gradle.daemon=false',
            "-Djava.io.tmpdir=${script.pwd(tmp: true)}", //temp dir settes til samme mappe som jenkins (<workspace name>@tmp)
            '--stacktrace'
    ].join(' ')
}

