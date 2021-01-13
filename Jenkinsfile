pipeline {
    agent {
        docker {
            image 'maven:3-amazoncorretto-8'
            args '-v /root/.m2:/root/.m2'
        }
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }
        stage('Test') {
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