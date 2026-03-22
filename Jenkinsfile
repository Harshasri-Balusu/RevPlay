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
                    sh 'aws s3 sync dist/revplay_frontend/browser s3://revhire-f --delete || aws s3 sync dist s3://revhire-f --delete'
                }
            }
        }

        stage('Deploy Backend to EC2') {
            steps {
                dir('revplay_backend') {
                    // 1. Replace BACKEND_IP with the actual IP address of your backend EC2 instance!
                    // 2. Adjust 'ubuntu' username if your backend is Amazon Linux ('ec2-user').
                    // 3. Ensure the Jenkins server's SSH key is copied to the backend EC2 server (~/.ssh/authorized_keys).
                    sh '''
                    scp -o StrictHostKeyChecking=no target/musicplatform-0.0.1-SNAPSHOT.jar ubuntu@BACKEND_IP:/home/ubuntu/
                    ssh -o StrictHostKeyChecking=no ubuntu@BACKEND_IP "pkill -f 'musicplatform.*.jar' || true"
                    ssh -o StrictHostKeyChecking=no ubuntu@BACKEND_IP "nohup java -jar /home/ubuntu/musicplatform-0.0.1-SNAPSHOT.jar > app.log 2>&1 &"
                    '''
                }
            }
        }
    }
}
