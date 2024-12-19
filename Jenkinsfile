pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'http://158.160.156.100/:8123/repository/mydockerrepo'  
        IMAGE_NAME = 'geoserver'                             
        IMAGE_TAG = "${IMAGE_TAG:-v1.0.1}" 
        FULL_IMAGE = "${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"              
        REPO_URL = 'https://github.com/Regina117/jenks.git'  
        DEPLOY_SERVER = 'http://84.201.170.10'                
    }

    stages {
        stage('Clone Repository') {
            steps {
                git url: "$REPO_URL", branch: 'main'
            }
        }

          stage('Build Project with Maven') {
            steps {
                script {
                    dir('src') { 
                        sh 'mvn clean package -DskipTests || { echo "Maven build failed"; exit 1; }'
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
                        ssh -o StrictHostKeyChecking=no jenkins@$DEPLOY_SERVER << EOF
                            docker login ${DOCKER_REGISTRY} -u admin -p "Dm59JTErVdXaKaN"
                            docker pull ${FULL_IMAGE}
                            docker stop $IMAGE_NAME || true
                            docker rm $IMAGE_NAME || true
                            docker run -d --name $IMAGE_NAME -p 8082:80 $DOCKER_REGISTRY/$IMAGE_NAME:$IMAGE_TAG
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
