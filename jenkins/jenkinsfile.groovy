pipeline {
    agent { node { label 'smalljobs' } }
    options {
        timeout(time: 1, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr: '100', artifactDaysToKeepStr: '7'))
        ansiColor('xterm')
    }
    environment {
        DEFAULT_SLACK_CHANNEL='testing123_123'
    }
    stages {
        stage("build") {
            steps {
                sh """./gradlew clean build"""
            }
        }
        stage("unittests") {
            steps {
                sh """./gradlew test"""
            }
        }
        stage("Merge") {
            steps {
                sh """echo 'Merge here'"""
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
                sh """echo Deply artifact to artifactory"""
            }
        }
        stage("Cleanup") {
            steps {
                cleanWs()
            }
        }
    }
    post {
        always {
          junit '**/build/**/*.xml'
          jacoco(
              execPattern: '**/build/jacoco/test.exec',
              classPattern: '**/build/classes/java/main',
              sourcePattern: '**/src/main/java',
              exclusionPattern: '**/src/test*'
          )
        }

        success {
          archiveArtifacts '**/build/libs/**/*.jar,build/reports/**'
        }

        failure {
          sh """echo 'Failure'"""
        }
    }
}
