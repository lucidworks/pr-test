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
        stage("Archive") {
            steps {
                archiveArtifacts artifacts: 'build/test-results/**/*.xml', onlyIfSuccessful: false
                junit 'build/test-results/**/*.xml'
            }
        }
        stage("Cleanup") {
            steps {
                cleanWs()
            }
        }
    }
}
