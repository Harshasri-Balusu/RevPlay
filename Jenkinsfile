pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build Spring Boot Backend') {
            steps {
                dir('revplay_backend') {
                    // Make sure maven is installed on your EC2 instance
                    sh 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Deploy Spring Boot Backend') {
            steps {
                dir('revplay_backend') {
                    sh '''
                    # Find and kill the currently running backend application gracefully
                    pkill -f 'musicplatform.*.jar' || true
                    
                    # Run the newly built backend jar file
                    JENKINS_NODE_COOKIE=dontKillMe nohup java -jar target/musicplatform-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
                    '''
                }
            }
        }

        stage('Build Angular Frontend') {
            steps {
                dir('revplay_frontend') {
                    // Make sure Node.js and npm are installed on your EC2 instance
                    sh 'npm install'
                    sh 'npm run build'
                }
            }
        }

        stage('Deploy Angular Frontend') {
            steps {
                dir('revplay_frontend') {
                    // Deploys the built files to the Nginx web directory.
                    // Note: Your 'jenkins' user must have permission to write to /var/www/html/
                    // Or you must add 'jenkins' to the sudoers file to run 'sudo cp' without a password.
                    sh 'sudo cp -r dist/revplay_frontend/browser/* /var/www/html/ || sudo cp -r dist/revplay_frontend/* /var/www/html/'
                }
            }
        }
    }
}
