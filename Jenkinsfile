pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = '158.160.140.97:8123/repository/mydockerrepo'
        IMAGE_NAME = 'geoserver'
        IMAGE_TAG = 'v1.0.1'
        FULL_IMAGE = "${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
        REPO_URL = 'https://github.com/Regina117/jenks.git'
        DEPLOY_SERVER = '158.160.156.175'
        NEXUS_CREDENTIALS_ID = 'nexusadmin'
        PROD_CREDENTIALS_ID = 'prod'
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
                    withCredentials([usernamePassword(credentialsId: NEXUS_CREDENTIALS_ID, usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                        sh '''
                        echo "${NEXUS_PASS}" | docker login ${DOCKER_REGISTRY} -u ${NEXUS_USER} --password-stdin || { echo "Docker login failed"; exit 1; }
                        docker build -t ${FULL_IMAGE} . || { echo "Docker build failed"; exit 1; }
                        '''
                    }
                }
            }
        }

        stage('Push Docker Image to Nexus') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: NEXUS_CREDENTIALS_ID, usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                        sh '''
                        echo "${NEXUS_PASS}" | docker login ${DOCKER_REGISTRY} -u ${NEXUS_USER} --password-stdin || { echo "Docker push failed"; exit 1; }
                        docker push ${FULL_IMAGE} || { echo "Docker push failed"; exit 1; }
                        '''
                    }
                }
            }
        }

        stage('Deploy to Tomcat') {
            steps {
                script {
                    deploy adapters: [tomcat9(credentialsId: 'prod', path: '', url: 'http://158.160.156.175:8080/')], contextPath: 'geoserver', war: '**/*.war'
                }
            }
        }

        stage('Run Docker on slave') {
            steps {
                script {
                    sh 'ssh-keyscan -H 158.160.156.175 >> ~/.ssh/known_hosts'
                    withCredentials([usernamePassword(credentialsId: NEXUS_CREDENTIALS_ID, usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                        sh """
                         ssh -o StrictHostKeyChecking=no -i /var/lib/jenkins/.ssh/id_rsa jenkins@158.160.156.175 << EOF
                set -ex
                sudo docker login 158.160.140.97:8123/repository/mydockerrepo -u ${NEXUS_USER} -p ${NEXUS_PASS}
                sudo docker pull ${FULL_IMAGE}
                sudo docker stop geoserver || true
                sudo docker rm geoserver || true
                sudo docker run -d --name geoserver -p 8080:80 ${FULL_IMAGE}
                        EOF
                        """
                    }
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
