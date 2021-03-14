node {
    docker.image('maven:3-amazoncorretto-8').inside {
        stage('Build Java 8') {
            sh 'mvn -B -DskipTests clean package'
            sh 'for f in **/target/antivpn-*.jar; do mv "$f" "${f%.jar}-j8.jar"; done'
            archiveArtifacts artifacts: '**/target/antivpn-*-j8.jar', fingerprint: true
        }
        stage('Test Java 8') {
            sh 'mvn -B test'
        }
    }
    docker.image('maven:3-amazoncorretto-11').inside {
        stage('Build Java 11') {
            sh 'mvn -B -DskipTests clean package'
            sh 'for f in **/target/antivpn-*.jar; do mv "$f" "${f%.jar}-j11.jar"; done'
            archiveArtifacts artifacts: '**/target/antivpn-*-j11.jar', fingerprint: true
        }
        stage('Test Java 11') {
            sh 'mvn -B test'
        }
    }
}