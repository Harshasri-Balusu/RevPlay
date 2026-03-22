pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Backend') {
            steps {
                dir('revplay_backend') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Build Frontend') {
            steps {
                dir('revplay_frontend') {
                    sh '''
                    export NODE_OPTIONS=--max-old-space-size=512
                    npm install
                    npm run build
                    '''
                }
            }
        }

        stage('Deploy Frontend to S3') {
            steps {
                dir('revplay_frontend') {
                    // Update 'revhire-f' with the actual S3 bucket name if it's different.
                    // Also ensure the Jenkins EC2 has AWS credentials configured (IAM role attached).
                    sh '/snap/bin/aws s3 sync dist/revplay-ui/browser s3://revplay-frontend-harshasri --delete || /snap/bin/aws s3 sync dist s3://revplay-frontend-harshasri --delete'
                }
            }
        }

        stage('Deploy Backend to EC2') {
            steps {
                dir('revplay_backend') {
                    sh '''
                    scp -i /home/ubuntu/revplay-key1.pem -o StrictHostKeyChecking=no target/musicplatform-0.0.1-SNAPSHOT.jar ubuntu@3.7.137.29:/home/ubuntu/
                    ssh -i /home/ubuntu/revplay-key1.pem -o StrictHostKeyChecking=no ubuntu@3.7.137.29 "sudo systemctl restart revplay"
                    '''
                }
            }
        }

        stage('Cleanup') {
            steps {
                cleanWs()
            }
        }
    }
}
