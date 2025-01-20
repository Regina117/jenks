pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = '51.250.41.36:8123/repository/mydockerrepo'  
        IMAGE_NAME = 'geoserver'
        IMAGE_TAG = 'v1.0.1'
        FULL_IMAGE = "${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
        REPO_URL = 'https://github.com/Regina117/jenks.git'
        DEPLOY_SERVER = '84.252.133.58'
        SSH_KEY_PATH = '/root/.ssh/id_rsa' 
        NEXUS_CREDENTIALS_ID = 'nexusadmin'
        NEXUS_SERVER = 'nexus'
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
                    deploy adapters: [tomcat9(credentialsId: 'prod', path: '', url: 'http://84.252.133.58:8080/')], contextPath: 'geoserver', war: '**/*.war'
                }
            }
        }

        stage('Run Docker on slave') {
            steps {
                script {
                    sshKeyScan = sh(script: "ssh-keyscan -H ${DEPLOY_SERVER}", returnStdout: true).trim()
                    writeFile file: "${HOME}/.ssh/id_rsa", text: sshKeyScan
                    
                    withCredentials([usernamePassword(credentialsId: NEXUS_CREDENTIALS_ID, usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                        sh '''
                        ssh ${DEPLOY_SERVER} << EOF
                            sudo docker login ${DOCKER_REGISTRY} -u ${NEXUS_USER} -p ${NEXUS_PASS}
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

