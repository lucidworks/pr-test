pipeline {
    agent { node { label 'smalljobs' } }
    options {
        timeout(time: 1, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr: '100', artifactDaysToKeepStr: '7'))
        ansiColor('xterm')
    }
    environment {
        DEFAULT_SLACK_CHANNEL='testing123_123'
        GITHUB_CREDENTIAL_ID='github-token-lucid-ci'
        GIT_ORG='lucidworks'
    }
    stages {
        stage("build") {
            steps {
                sh """./gradlew clean build -i"""
            }
        }
        stage("unittests") {
            steps {
                sh """./gradlew test -i && ls -l build/test-results/test/*.xml"""
            }
        }
        stage("Merge") {
            steps {
                script {
                   sh """printenv"""
                }
                // Merge will happen in the docker image
                // Build origin PRs (merged with base branch) this enabled a Jenkins-provided environment variable, $CHANGE_ID that in the case of a pull request, is the pull request number.
                withCredentials([string(credentialsId: GITHUB_CREDENTIAL_ID, variable: 'GITHUB_TOKEN')]) {
                    script {
                        def repo = sh (script: 'basename -s .git `git config --get remote.origin.url`', returnStdout: true).trim()
                        
                        docker.withRegistry('https://qe-docker.ci-artifactory.lucidworks.com', 'ARTIFACTORY_JENKINS'){
                            docker.image('qe-docker.ci-artifactory.lucidworks.com/git_helper:041720191128').inside('--entrypoint ""'){
                                output = sh( script: "python /pr.py ${repo} ${CHANGE_ID}", returnStdout: true).trim()
                        }}
                        echo "${output}"
                        if ( "${output}".contains("MERGE!") ) {
                            echo "Do Merge!"
                        } else {
                            echo "Not merging"
                        }
                    }
                }
             }
        }
        stage('Deploy Artifact') {
            // Always deploy master - SNAPSHOTs of latest development target
            // & Deploy all release branches.
            when {
                anyOf {
                    branch 'master'
                    expression {
                        return (env.GIT_BRANCH ==~ /releases\/.*/)
                    }
                }
            }
            steps {
                sh """./gradlew snapshot publish -i"""
            }
        }
    }
    post {
        success {
            sh """echo 'Success'"""
        }

        failure {
          sh """echo 'Failure'"""
        }
        always {
            archiveArtifacts artifacts:'**/*.jar,build/test-results/**/*.xml,build/reports/*', allowEmptyArchive: true, excludes: '**/gradle-wrapper.jar,**/jacocoagent.jar'
            script {
                try {
                  junit 'build/test-results/**/*.xml'
                } catch (Exception e) {
                    echo "could not find junit reports! ${e.message}"
                    sh """ls -l build/test-results/test/*.xml"""
                }
                jacoco(
                    execPattern: '**/build/jacoco/test.exec',
                    classPattern: '**/build/classes/java/main',
                    sourcePattern: '**/src/main/java',
                    exclusionPattern: '**/src/test*'
                )
            }
            cleanWs()
        }
    }
}
