pipeline {
    agent {
        docker {
            image 'docker:20.10.12'
            args '--privileged'
        }
    }

    environment {
        DOCKER_BUILDKIT = '1'
        REGISTRY_URL = 'http://51.250.41.36:8123/repository/mydockerrepo'
        IMAGE_NAME = 'geoserver'
        IMAGE_TAG = 'v1.0'
        FULL_IMAGE = "${REGISTRY_URL}/${IMAGE_NAME}:${IMAGE_TAG}"
        GITHUB_REPO_URL = 'https://github.com/Regina117/jenks.git' 
        NEXUS_CREDENTIALS_ID = 'nexusadmin'
        BRANCH_NAME = 'main'
    }

    stages {
        stage('Clone Repository') {
            steps {
                checkout scm: [
                    $class: 'GitSCM',
                    branches: [[name: "*/${BRANCH_NAME}"]],
                    userRemoteConfigs: [[url: "${GITHUB_REPO_URL}"]]
                ]
            }
        }

        stage('Login to Docker Registry') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: "${NEXUS_CREDENTIALS_ID}", passwordVariable: 'NEXUS_PASSWORD', usernameVariable: 'NEXUS_USERNAME')]) {
                        sh '''
                        echo $NEXUS_PASSWORD | docker login http://51.250.41.36:8123 --username $NEXUS_USERNAME --password-stdin
                        '''
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${FULL_IMAGE}", "-f ./Dockerfile .")
                }
            }
        }

        stage('Tag Docker Image') {
            steps {
                script {
                    sh '''
                        docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${FULL_IMAGE}
                    '''
                }
            }
        }

        stage('Push to Nexus') {
            steps {
                script {
                    sh '''
                        docker push ${FULL_IMAGE}
                    '''
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo "Pipeline executed successfully! Image ${FULL_IMAGE} pushed to Nexus."
        }
        failure {
            echo "Pipeline failed!"
        }
    }
}

