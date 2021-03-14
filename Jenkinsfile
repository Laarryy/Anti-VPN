pipeline {
    agent none

    node {
        docker.image('maven:3-amazoncorretto-8').inside {
            stage('Build') {
                sh 'mvn -B -T 1C -DskipTests clean package'
                archiveArtifacts artifacts: '**/target/antivpn-*.jar', fingerprint: true
            }
            stage('Test') {
                sh 'mvn -B test'
            }
        }
        docker.image('maven:3-amazoncorretto-11').inside {
            stage('Build') {
                sh 'mvn -B -T 1C -DskipTests clean package'
                archiveArtifacts artifacts: '**/target/antivpn-*.jar', fingerprint: true
            }
            stage('Test') {
                sh 'mvn -B test'
            }
        }
    }
}