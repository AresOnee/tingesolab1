// ============================================
// JENKINS PIPELINE - TOOLRENT APPLICATION
// ============================================
// Pipeline de CI/CD para Backend y Frontend

pipeline {
    agent any

    environment {
        // Docker Hub credentials (configurar en Jenkins)
        DOCKER_CREDENTIALS_ID = 'dockerhub-credentials'
        DOCKER_USERNAME = credentials('dockerhub-username')

        // Nombres de las imágenes
        BACKEND_IMAGE = "${DOCKER_USERNAME}/toolrent-backend"
        FRONTEND_IMAGE = "${DOCKER_USERNAME}/toolrent-frontend"

        // Tag de la imagen (usa el número de build de Jenkins)
        IMAGE_TAG = "${BUILD_NUMBER}"

        // Directorios
        BACKEND_DIR = 'backend-toolrent'
        FRONTEND_DIR = 'toolrent-frontend'
    }

    stages {
        // ====== STAGE 1: CHECKOUT ======
        stage('Checkout') {
            steps {
                script {
                    echo '================================================'
                    echo '           CHECKOUT CÓDIGO DESDE GITHUB         '
                    echo '================================================'
                }

                // Checkout del código desde GitHub
                checkout scm

                script {
                    echo "✅ Checkout completado"
                    echo "Branch: ${env.GIT_BRANCH}"
                    echo "Commit: ${env.GIT_COMMIT}"
                }
            }
        }

        // ====== STAGE 2: TESTS BACKEND ======
        stage('Test Backend') {
            steps {
                script {
                    echo '================================================'
                    echo '         EJECUTANDO TESTS UNITARIOS BACKEND    '
                    echo '================================================'
                }

                dir("${BACKEND_DIR}") {
                    // Ejecutar tests con Maven
                    sh '''
                        chmod +x mvnw
                        ./mvnw clean test
                    '''

                    // Generar reporte de cobertura con Jacoco
                    sh './mvnw jacoco:report'

                    echo "✅ Tests ejecutados correctamente"
                }
            }

            post {
                always {
                    // Publicar resultados de tests
                    junit "**/target/surefire-reports/*.xml"

                    // Publicar reporte de cobertura Jacoco
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java',
                        exclusionPattern: '**/test/**'
                    )
                }
            }
        }

        // ====== STAGE 3: BUILD DOCKER IMAGES ======
        stage('Build Docker Images') {
            parallel {
                // Backend Image
                stage('Build Backend Image') {
                    steps {
                        script {
                            echo '================================================'
                            echo '         CONSTRUYENDO IMAGEN DOCKER BACKEND    '
                            echo '================================================'
                        }

                        dir("${BACKEND_DIR}") {
                            script {
                                // Build de la imagen Docker
                                def backendImage = docker.build("${BACKEND_IMAGE}:${IMAGE_TAG}")

                                // Tag latest
                                sh "docker tag ${BACKEND_IMAGE}:${IMAGE_TAG} ${BACKEND_IMAGE}:latest"

                                echo "✅ Imagen backend construida: ${BACKEND_IMAGE}:${IMAGE_TAG}"
                            }
                        }
                    }
                }

                // Frontend Image
                stage('Build Frontend Image') {
                    steps {
                        script {
                            echo '================================================'
                            echo '        CONSTRUYENDO IMAGEN DOCKER FRONTEND    '
                            echo '================================================'
                        }

                        dir("${FRONTEND_DIR}") {
                            script {
                                // Build de la imagen Docker
                                def frontendImage = docker.build("${FRONTEND_IMAGE}:${IMAGE_TAG}")

                                // Tag latest
                                sh "docker tag ${FRONTEND_IMAGE}:${IMAGE_TAG} ${FRONTEND_IMAGE}:latest"

                                echo "✅ Imagen frontend construida: ${FRONTEND_IMAGE}:${IMAGE_TAG}"
                            }
                        }
                    }
                }
            }
        }

        // ====== STAGE 4: PUSH TO DOCKER HUB ======
        stage('Push to DockerHub') {
            steps {
                script {
                    echo '================================================'
                    echo '          SUBIENDO IMÁGENES A DOCKER HUB       '
                    echo '================================================'
                }

                // Login a Docker Hub
                withCredentials([usernamePassword(
                    credentialsId: "${DOCKER_CREDENTIALS_ID}",
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
                }

                // Push Backend
                sh """
                    docker push ${BACKEND_IMAGE}:${IMAGE_TAG}
                    docker push ${BACKEND_IMAGE}:latest
                """
                echo "✅ Backend image pushed: ${BACKEND_IMAGE}:${IMAGE_TAG}"

                // Push Frontend
                sh """
                    docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}
                    docker push ${FRONTEND_IMAGE}:latest
                """
                echo "✅ Frontend image pushed: ${FRONTEND_IMAGE}:${IMAGE_TAG}"
            }
        }

        // ====== STAGE 5: CLEANUP ======
        stage('Cleanup') {
            steps {
                script {
                    echo '================================================'
                    echo '              LIMPIANDO IMÁGENES LOCALES       '
                    echo '================================================'
                }

                // Limpiar imágenes locales para ahorrar espacio
                sh """
                    docker rmi ${BACKEND_IMAGE}:${IMAGE_TAG} || true
                    docker rmi ${FRONTEND_IMAGE}:${IMAGE_TAG} || true
                """

                echo "✅ Cleanup completado"
            }
        }
    }

    // ====== POST ACTIONS ======
    post {
        success {
            script {
                echo '================================================'
                echo '          ✅ PIPELINE EJECUTADO EXITOSAMENTE   '
                echo '================================================'
                echo "Backend Image: ${BACKEND_IMAGE}:${IMAGE_TAG}"
                echo "Frontend Image: ${FRONTEND_IMAGE}:${IMAGE_TAG}"
                echo '================================================'
            }
        }

        failure {
            script {
                echo '================================================'
                echo '          ❌ PIPELINE FALLÓ                    '
                echo '================================================'
            }
        }

        always {
            // Limpiar workspace
            cleanWs()
        }
    }
}
