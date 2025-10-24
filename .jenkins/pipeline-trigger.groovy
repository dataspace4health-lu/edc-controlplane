pipeline {
    agent {label 'worker'}
    stages {
        stage('Trigger edc-controlplane pipeline') {
            steps {
                script {
                    build job: 'DS4H/Microservices/edc-controlplane', parameters: [
                    ]
                }
            }
        }
    }
}