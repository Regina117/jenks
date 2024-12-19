pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'http://158.160.156.100/:8123/repository/mydockerrepo'  
        IMAGE_NAME = 'geoserver'                             
        IMAGE_TAG = "${IMAGE_TAG:-v1.0.1}"               
        REPO_URL = 'https://github.com/Regina117/jenks.git'  
        DEPLOY_SERVER = 'http://84.201.170.10'                
    }

    stages {
        stage('Clone Repository') {
            steps {
                git url: "$REPO_URL", branch: 'main'
                git credentialsId: '3d487e3f-cfbc-4f73-9569-0f7caa18afea'
            }
        }

          stage('Build Project with Maven') {
            steps {
                script {
                    dir('src') { 
                        sh 'mvn clean install || { echo "Maven build failed"; exit 1; }'
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh '''
                    echo "Dm59JTErVdXaKaN" | docker login ${REGISTRY_URL} -u admin --password-stdin || { echo "Docker login failed"; exit 1; }
                    docker build -t ${FULL_IMAGE} . || { echo "Docker build failed"; exit 1; }
                    '''
                }
            }
        }

        
        stage('Push Docker Image to Nexus') {
            steps {
                script {
                    sh '''
                    docker push ${FULL_IMAGE} || { echo "Docker push failed"; exit 1; }
                    '''
                }
            }
        }

        stage('Deploy to Server') {
            steps {
                script {
                    sh """
                        ssh jenkins@$DEPLOY_SERVER << EOF
                            docker pull $DOCKER_REGISTRY/$IMAGE_NAME:$IMAGE_TAG
                            docker stop $IMAGE_NAME || true
                            docker rm $IMAGE_NAME || true
                            docker run -d --name $IMAGE_NAME -p 8080:80 $DOCKER_REGISTRY/$IMAGE_NAME:$IMAGE_TAG
                        EOF
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'Deployment succeeded!'
        }
        failure {
            echo 'Deployment failed!'
        }
    }
}
