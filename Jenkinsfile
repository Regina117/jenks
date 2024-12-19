pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = '158.160.156.100:8123/repository/mydockerrepo'  
        IMAGE_NAME = 'geoserver'
        IMAGE_TAG = 'v1.0.1'
        FULL_IMAGE = "${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
        REPO_URL = 'https://github.com/Regina117/jenks.git'
        DEPLOY_SERVER = '84.201.170.10'
        SSH_KEY_PATH = '/root/.ssh/id_rsa' 
    }

    stages {
        stage('Initialize Environment') {
            steps {
                script {
                    env.IMAGE_TAG = env.IMAGE_TAG ?: 'v1.0.1'
                    env.FULL_IMAGE = "${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.IMAGE_TAG}"
                }
            }
        }

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
                    echo "Dm59JTErVdXaKaN" | docker login ${DOCKER_REGISTRY} -u admin --password-stdin || { echo "Docker login failed"; exit 1; }
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

        stage('Deploy to Tomcat') {
            steps {
                script {
                    deploy adapters: [tomcat9(credentialsId: 'dd79bb68-ab91-4d6a-9530-bd0f093c8020', path: '', url: 'http://84.201.170.10:8080')], contextPath: 'geoserver', war: '**/*.war'
                }
            }
        }

        stage('Run Docker on slave') {
            steps {
                script {
                    sh '''
                    ssh-keyscan -H ${DEPLOY_SERVER} >> ~/.ssh/known_hosts
                    ssh root@${DEPLOY_SERVER} << EOF
                        sudo docker login ${DOCKER_REGISTRY} -u admin -p "Dm59JTErVdXaKaN"
                        sudo docker pull ${FULL_IMAGE}
                        sudo docker stop ${IMAGE_NAME} || true
                        sudo docker rm ${IMAGE_NAME} || true
                        sudo docker run -d --name ${IMAGE_NAME} -p 8080:80 ${FULL_IMAGE}
                    EOF
                    '''
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

