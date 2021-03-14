pipeline {
    agent none
    stages {
        stage('Build-8') {
            agent {
                docker {
                    image 'maven:3-amazoncorretto-8'
                }
            }
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }
        stage('Test-8') {
            agent {
                docker {
                    image 'maven:3-amazoncorretto-8'
                }
            }
            steps {
                sh 'mvn test'
            }
        }
        stage('Build-11') {
            agent {
                docker {
                    image 'maven:3-amazoncorretto-11'
                }
            }
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }
        stage('Test-11') {
            agent {
                docker {
                    image 'maven:3-amazoncorretto-11'
                }
            }
            steps {
                sh 'mvn test'
            }
        }
    }
	post {
        always {
            archiveArtifacts artifacts: '**/target/antivpn-*.jar', fingerprint: true
        }
    }
}