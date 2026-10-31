// ============================================================
// Jenkinsfile — Cloud Pipeline Demo App
// CI/CD: Maven Build → Test → Docker → ACR → AKS
//
// Required Jenkins Credentials:
//   - azure-client-id      : Azure Service Principal App ID     (Secret Text)
//   - azure-client-secret  : Azure Service Principal Password   (Secret Text)
//   - azure-tenant-id      : Azure Tenant ID                    (Secret Text)
//   - azure-subscription-id: Azure Subscription ID              (Secret Text)
//
// Required Jenkins Plugins:
//   - Pipeline, Git, Docker Pipeline, Credentials Binding,
//     JUnit, Workspace Cleanup, Azure Credentials (optional)
// ============================================================

pipeline {

    agent any

    // ── Environment Variables ────────────────────────────────
    environment {
        // Azure Resource Group
        AZURE_RESOURCE_GROUP    = 'cloud-pipeline-rg'

        // Azure Container Registry
        ACR_NAME                = 'cloudpipelineacr'
        ACR_LOGIN_SERVER        = "${ACR_NAME}.azurecr.io"
        IMAGE_NAME              = 'cloud-pipeline-app'
        IMAGE_BASE              = "${ACR_LOGIN_SERVER}/${IMAGE_NAME}"

        // Azure Kubernetes Service
        AKS_CLUSTER             = 'cloud-pipeline-aks'
        AKS_RESOURCE_GROUP      = 'cloud-pipeline-rg'
        K8S_NAMESPACE           = 'cloud-pipeline'

        // Azure region (free tier / student offer regions)
        AZURE_REGION            = 'centralindia'

        // Maven
        MAVEN_OPTS              = '-Xmx512m'
    }

    // ── Build Options ────────────────────────────────────────
    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    // ── Pipeline Stages ──────────────────────────────────────
    stages {

        // ── Stage 1: Checkout ────────────────────────────────
        stage('Checkout') {
            steps {
                echo '========== Stage 1: Checkout Source Code =========='
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                    env.IMAGE_TAG = "${BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                    echo "Build tag: ${env.IMAGE_TAG}"
                }
            }
        }

        // ── Stage 2: Compile ─────────────────────────────────
        stage('Compile') {
            steps {
                echo '========== Stage 2: Maven Compile =========='
                sh 'mvn clean compile -B -q'
            }
        }

        // ── Stage 3: Unit Tests ──────────────────────────────
        stage('Test') {
            steps {
                echo '========== Stage 3: Run Unit Tests =========='
                sh 'mvn test -B'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml',
                          allowEmptyResults: false
                    echo 'JUnit test reports published.'
                }
                failure {
                    echo 'Tests FAILED — pipeline will not proceed.'
                }
            }
        }

        // ── Stage 4: Package ─────────────────────────────────
        stage('Package') {
            steps {
                echo '========== Stage 4: Maven Package (fat JAR) =========='
                sh 'mvn package -DskipTests -B'
                archiveArtifacts artifacts: 'target/cloud-pipeline-app.jar',
                                 fingerprint: true
                echo 'JAR archived: target/cloud-pipeline-app.jar'
            }
        }

        // ── Stage 5: Docker Build ────────────────────────────
        stage('Docker Build') {
            steps {
                echo '========== Stage 5: Build Docker Image =========='
                sh """
                    docker build \\
                        --tag ${IMAGE_BASE}:${IMAGE_TAG} \\
                        --tag ${IMAGE_BASE}:latest \\
                        --label "build.number=${BUILD_NUMBER}" \\
                        --label "git.commit=${GIT_COMMIT_SHORT}" \\
                        --label "build.url=${BUILD_URL}" \\
                        .
                """
                echo "Image built: ${IMAGE_BASE}:${IMAGE_TAG}"
            }
        }

        // ── Stage 6: Push to Azure Container Registry ────────
        stage('Push to ACR') {
            steps {
                echo '========== Stage 6: Push Docker Image to ACR =========='
                withCredentials([
                    string(credentialsId: 'azure-client-id',       variable: 'AZURE_CLIENT_ID'),
                    string(credentialsId: 'azure-client-secret',   variable: 'AZURE_CLIENT_SECRET'),
                    string(credentialsId: 'azure-tenant-id',       variable: 'AZURE_TENANT_ID'),
                    string(credentialsId: 'azure-subscription-id', variable: 'AZURE_SUBSCRIPTION_ID')
                ]) {
                    sh """
                        # Login to Azure using service principal
                        az login --service-principal \\
                            --username  "\$AZURE_CLIENT_ID" \\
                            --password  "\$AZURE_CLIENT_SECRET" \\
                            --tenant    "\$AZURE_TENANT_ID" \\
                            --output    none

                        az account set --subscription "\$AZURE_SUBSCRIPTION_ID"

                        # Authenticate Docker to ACR
                        az acr login --name ${ACR_NAME}

                        # Push both tags
                        docker push ${IMAGE_BASE}:${IMAGE_TAG}
                        docker push ${IMAGE_BASE}:latest

                        echo "Pushed:"
                        echo "  → ${IMAGE_BASE}:${IMAGE_TAG}"
                        echo "  → ${IMAGE_BASE}:latest"
                    """
                }
            }
        }

        // ── Stage 7: Deploy to AKS ───────────────────────────
        stage('Deploy to AKS') {
            steps {
                echo '========== Stage 7: Deploy to Azure Kubernetes Service =========='
                withCredentials([
                    string(credentialsId: 'azure-client-id',       variable: 'AZURE_CLIENT_ID'),
                    string(credentialsId: 'azure-client-secret',   variable: 'AZURE_CLIENT_SECRET'),
                    string(credentialsId: 'azure-tenant-id',       variable: 'AZURE_TENANT_ID'),
                    string(credentialsId: 'azure-subscription-id', variable: 'AZURE_SUBSCRIPTION_ID')
                ]) {
                    sh """
                        # Login to Azure
                        az login --service-principal \\
                            --username  "\$AZURE_CLIENT_ID" \\
                            --password  "\$AZURE_CLIENT_SECRET" \\
                            --tenant    "\$AZURE_TENANT_ID" \\
                            --output    none

                        az account set --subscription "\$AZURE_SUBSCRIPTION_ID"

                        # Fetch AKS credentials → writes ~/.kube/config
                        az aks get-credentials \\
                            --resource-group ${AKS_RESOURCE_GROUP} \\
                            --name           ${AKS_CLUSTER} \\
                            --overwrite-existing

                        # Ensure namespace exists
                        kubectl apply -f k8s/namespace.yaml

                        # Apply ConfigMap
                        kubectl apply -f k8s/configmap.yaml -n ${K8S_NAMESPACE}

                        # Update running deployment to the new image tag
                        kubectl set image deployment/cloud-pipeline-app \\
                            cloud-pipeline-app=${IMAGE_BASE}:${IMAGE_TAG} \\
                            -n ${K8S_NAMESPACE}

                        # Wait for rolling update to complete (max 5 min)
                        kubectl rollout status deployment/cloud-pipeline-app \\
                            -n ${K8S_NAMESPACE} \\
                            --timeout=300s
                    """
                }
            }
        }

        // ── Stage 8: Verify Deployment ───────────────────────
        stage('Verify Deployment') {
            steps {
                echo '========== Stage 8: Verify Deployment =========='
                withCredentials([
                    string(credentialsId: 'azure-client-id',       variable: 'AZURE_CLIENT_ID'),
                    string(credentialsId: 'azure-client-secret',   variable: 'AZURE_CLIENT_SECRET'),
                    string(credentialsId: 'azure-tenant-id',       variable: 'AZURE_TENANT_ID'),
                    string(credentialsId: 'azure-subscription-id', variable: 'AZURE_SUBSCRIPTION_ID')
                ]) {
                    sh """
                        az login --service-principal \\
                            --username "\$AZURE_CLIENT_ID" \\
                            --password "\$AZURE_CLIENT_SECRET" \\
                            --tenant   "\$AZURE_TENANT_ID" \\
                            --output   none

                        az aks get-credentials \\
                            --resource-group ${AKS_RESOURCE_GROUP} \\
                            --name           ${AKS_CLUSTER} \\
                            --overwrite-existing

                        echo "--- Pods ---"
                        kubectl get pods -n ${K8S_NAMESPACE} -l app=cloud-pipeline-app -o wide

                        echo "--- Service ---"
                        kubectl get svc -n ${K8S_NAMESPACE}

                        echo "--- Deployment ---"
                        kubectl get deployment cloud-pipeline-app -n ${K8S_NAMESPACE}

                        # Get the external IP assigned by Azure Load Balancer
                        EXTERNAL_IP=\$(kubectl get svc cloud-pipeline-app-service \\
                            -n ${K8S_NAMESPACE} \\
                            -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

                        if [ -n "\$EXTERNAL_IP" ]; then
                            echo "=============================================="
                            echo "  Application URL : http://\$EXTERNAL_IP"
                            echo "  Image Tag       : ${IMAGE_TAG}"
                            echo "  Namespace       : ${K8S_NAMESPACE}"
                            echo "  AKS Cluster     : ${AKS_CLUSTER}"
                            echo "=============================================="

                            # Smoke test — retry up to 5 times
                            for i in 1 2 3 4 5; do
                                STATUS=\$(curl -s -o /dev/null -w "%{http_code}" http://\$EXTERNAL_IP/health || true)
                                if [ "\$STATUS" = "200" ]; then
                                    echo "  Smoke test PASSED — /health returned 200 ✔"
                                    break
                                fi
                                echo "  Waiting for app (\$i/5)..."
                                sleep 10
                            done
                        else
                            echo "External IP not yet assigned — Azure LB is provisioning."
                            echo "Run: kubectl get svc -n ${K8S_NAMESPACE} --watch"
                        fi
                    """
                }
            }
        }
    }

    // ── Post-Pipeline Actions ────────────────────────────────
    post {
        success {
            echo """
╔══════════════════════════════════════════════╗
║   Pipeline SUCCESS  ✔                        ║
║   Build    : #${BUILD_NUMBER}                ║
║   Image tag: ${IMAGE_TAG}                    ║
║   Registry : ${ACR_LOGIN_SERVER}             ║
║   Cluster  : ${AKS_CLUSTER}                  ║
╚══════════════════════════════════════════════╝
            """
        }
        failure {
            echo """
╔══════════════════════════════════════════════╗
║   Pipeline FAILED ✘ — check logs above       ║
║   Build: #${BUILD_NUMBER}                    ║
╚══════════════════════════════════════════════╝
            """
        }
        always {
            sh 'docker image prune -f || true'
            cleanWs()
        }
    }
}
