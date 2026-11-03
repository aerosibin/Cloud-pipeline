# ☁️ Cloud Pipeline Demo App

[![Protected by Gitleaks](https://img.shields.io/badge/protected%20by-gitleaks-blue)](https://github.com/gitleaks/gitleaks)

> **End-to-End CI/CD Pipeline** — Jenkins · Microsoft Azure (ACR & AKS) · Maven · Docker · Kubernetes · Ansible

---

## 🏗 Architecture

```
Developer
    │
    │  git push (main)
    ▼
┌─────────────────────────────────────────────────────────────┐
│                      Jenkins Server                          │
│                   (Azure Ubuntu VM / Host)                  │
│                                                              │
│  Stage 1: Checkout         ← git clone from repository       │
│  Stage 2: Compile          ← mvn clean compile               │
│  Stage 3: Test             ← mvn test → JUnit XML reports    │
│  Stage 4: Package          ← mvn package → fat JAR artifact  │
│  Stage 5: Docker Build     ← docker build (multi-stage)      │
│  Stage 6: Push to ACR      ← az acr login + docker push      │
│  Stage 7: Deploy to AKS    ← az aks get-credentials + k8s   │
│  Stage 8: Verify           ← rollout status + smoke test     │
└───────────────────────────┬─────────────────────────────────┘
                            │
          ┌─────────────────▼──────────────────┐
          │   Azure Container Registry (ACR)   │
          │   cloudpipelineacr.azurecr.io      │
          └─────────────────┬──────────────────┘
                            │  image pull (AcrPull role)
          ┌─────────────────▼──────────────────┐
          │   Azure Kubernetes Service (AKS)   │
          │   Cluster: cloud-pipeline-aks      │
          │   Namespace: cloud-pipeline        │
          │                                    │
          │   ┌────────────┐  ┌────────────┐   │
          │   │  Pod 1     │  │  Pod 2     │   │
          │   │ Spring Boot│  │ Spring Boot│   │
          │   │ :8080      │  │ :8080      │   │
          │   └─────┬──────┘  └─────┬──────┘   │
          │         └───────┬────────┘         │
          │        ┌────────▼──────────┐       │
          │        │ Azure LoadBalancer│       │
          │        │ Port 80 → 8080    │       │
          │        └────────┬──────────┘       │
          └─────────────────┼──────────────────┘
                            │
               Public IP (Azure Load Balancer)
                            │
                   http://<EXTERNAL_IP>
                    Browser / End User
```

---

## 🛠 Tech Stack

| Layer             | Technology                              | Reason                                                    |
|-------------------|-----------------------------------------|-----------------------------------------------------------|
| Language          | Java 21 (LTS)                           | High performance, pattern matching, virtual threads       |
| Framework         | Spring Boot 3.2.0                       | Embedded Tomcat, Actuator health endpoints, REST APIs     |
| Build Tool        | Maven 3.9+                              | Dependency management, automated compilation & packaging  |
| Unit Testing      | JUnit 5 + Spring MockMvc                | Automated unit tests with Surefire XML reporting          |
| CI/CD Engine      | **Jenkins (Declarative Pipeline)**      | Enterprise standard, automated triggers, step monitoring |
| Containerisation  | Docker (Multi-Stage)                    | Lightweight Alpine JRE runtime, non-root user security   |
| Container Registry| **Azure Container Registry (ACR)**      | Azure Student Offer compatible, fast geo-replicated pull  |
| Orchestration     | **Azure Kubernetes Service (AKS)**      | Managed K8s, rolling zero-downtime updates, auto-healing  |
| Autoscaling       | Horizontal Pod Autoscaler (HPA)         | 2–5 pods dynamic scaling based on CPU & Memory metrics    |
| Config Management | Ansible                                 | Automated node provisioning and manifest application      |
| Cloud Provider    | **Microsoft Azure**                     | Azure for Students offer ($100 credit + free services)   |

---

## 📁 Project Structure

```
Cloud-Pipeline/
├── Jenkinsfile                          # Declarative 8-stage Azure CI/CD pipeline
├── Dockerfile                           # Multi-stage: Maven builder → Alpine JRE runtime
├── .dockerignore                        # Build context exclusions
├── pom.xml                              # Spring Boot 3.2.0, Java 21, Lombok, Surefire
├── README.md                            # Complete architecture and runbook
├── .gitignore                           # Git exclusions (target, keys, credentials)
│
├── src/
│   ├── main/
│   │   ├── java/com/cloudpipeline/app/
│   │   │   ├── CloudPipelineApplication.java
│   │   │   ├── controller/
│   │   │   │   ├── HomeController.java   # GET / (styled Bootstrap UI), /health, /info
│   │   │   │   └── ApiController.java    # GET /api/greet, /api/status
│   │   │   └── model/
│   │   │       ├── GreetingResponse.java
│   │   │       └── StatusResponse.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-production.properties
│   └── test/
│       └── java/com/cloudpipeline/app/
│           ├── CloudPipelineApplicationTests.java
│           ├── controller/HomeControllerTest.java
│           └── controller/ApiControllerTest.java
│
├── k8s/
│   ├── namespace.yaml                   # Namespace: cloud-pipeline
│   ├── configmap.yaml                   # Externalised production configuration
│   ├── deployment.yaml                  # 2 replicas, rolling updates, health probes
│   ├── service.yaml                     # Azure Public LoadBalancer (Port 80 → 8080)
│   ├── hpa.yaml                         # HPA: 2–5 pods targeting 70% CPU
│   └── ingress.yaml                     # Ingress definition for TLS / NGINX
│
└── ansible/
    ├── inventory.ini                    # Azure VM hosts and AKS environment variables
    ├── playbook.yml                     # Playbook: Azure CLI, Kubectl, Helm, K8s deploy
    └── roles/
        └── common/tasks/main.yml        # Base configuration: sysctl, swapoff, tools
```

---

## 🚀 Jenkins Pipeline Stages

The declarative [`Jenkinsfile`](file:///c:/SNUC/DevOps/CIA2/Cloud-Pipeline/Jenkinsfile) executes 8 automated stages:

| # | Stage                | Purpose                                                                               |
|---|----------------------|---------------------------------------------------------------------------------------|
| 1 | **Checkout**         | Clones repository and extracts Git short commit SHA (`GIT_COMMIT_SHORT`).            |
| 2 | **Compile**          | Runs `mvn clean compile` to check code compilation and resolve dependencies.          |
| 3 | **Test**             | Runs `mvn test` and publishes JUnit reports to Jenkins via `junit` post-action.       |
| 4 | **Package**          | Builds executable JAR via `mvn package -DskipTests` and archives build artifact.      |
| 5 | **Docker Build**     | Builds multi-stage Docker image tagged with `latest` and `${BUILD_NUMBER}-${SHA}`.    |
| 6 | **Push to ACR**      | Authenticates via Azure Service Principal and pushes images to Azure Container Reg.   |
| 7 | **Deploy to AKS**    | Fetches AKS credentials and updates the deployment image using zero-downtime rollout. |
| 8 | **Verify Deployment**| Checks Pod/Service statuses, resolves Azure Load Balancer IP, and curls `/health`.   |

---

## ☁️ Microsoft Azure Setup (Azure for Students)

### 1. Azure CLI Authentication
```bash
az login
az account list --output table
# Set default subscription
az account set --subscription "<YOUR_SUBSCRIPTION_ID>"
```

### 2. Create Resource Group
```bash
az group create \
    --name cloud-pipeline-rg \
    --location centralindia
```

### 3. Create Azure Container Registry (ACR)
```bash
az acr create \
    --resource-group cloud-pipeline-rg \
    --name cloudpipelineacr \
    --sku Basic \
    --admin-enabled true
```

### 4. Create Azure Kubernetes Service (AKS) Cluster
> [!TIP]
> The `--attach-acr` flag automatically grants AKS permission (`AcrPull`) to pull images from your ACR.
```bash
az aks create \
    --resource-group cloud-pipeline-rg \
    --name cloud-pipeline-aks \
    --node-count 2 \
    --node-vm-size Standard_B2s \
    --enable-managed-identity \
    --attach-acr cloudpipelineacr \
    --generate-ssh-keys
```

### 5. Create Azure Service Principal for Jenkins CI/CD
```bash
az ad sp create-for-rbac \
    --name "jenkins-cicd-sp" \
    --role "Contributor" \
    --scopes "/subscriptions/<YOUR_SUBSCRIPTION_ID>/resourceGroups/cloud-pipeline-rg" \
    --output json
```
This command outputs:
```json
{
  "appId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "displayName": "jenkins-cicd-sp",
  "password": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "tenant": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

---

## 🔑 Jenkins Credential Configuration

In Jenkins → **Manage Jenkins → Credentials → System → Global credentials**:

| Credential ID           | Kind        | Value                                          |
|-------------------------|-------------|------------------------------------------------|
| `azure-client-id`       | Secret text | Service Principal `appId`                      |
| `azure-client-secret`   | Secret text | Service Principal `password`                   |
| `azure-tenant-id`       | Secret text | Azure `tenant`                                 |
| `azure-subscription-id` | Secret text | Your Azure Subscription ID                     |

---

## 💻 Local Testing & Verification

### 1. Run Unit Tests locally
```bash
mvn clean test
```
All 9 unit tests pass covering application context, JSON APIs, and UI routes.

### 2. Run Application locally
```bash
mvn spring-boot:run
# Open http://localhost:8080 in your browser
```

### 3. Test Docker Container locally
```bash
docker build -t cloud-pipeline-app:local .
docker run -d -p 8080:8080 --name cloud-app cloud-pipeline-app:local
curl http://localhost:8080/health
curl http://localhost:8080/api/status
```

---

## 📡 Application Endpoints

| Method | Endpoint                | Purpose                                      | Content Type |
|--------|-------------------------|----------------------------------------------|--------------|
| GET    | `/`                     | Interactive Web Dashboard with system metrics | `text/html`  |
| GET    | `/health`               | Readiness & Liveness probe endpoint          | `application/json` |
| GET    | `/info`                 | App version, runtime JVM, and host details   | `application/json` |
| GET    | `/api/greet?name=Azure` | Dynamic Greeting API                         | `application/json` |
| GET    | `/api/status`           | Deployment environment and health timestamp  | `application/json` |
| GET    | `/actuator/health`      | Spring Boot Actuator deep health endpoint    | `application/json` |

---

## 🔄 Demonstrating Automated Redeployment (Deliverable 4)

1. **Make a code change:**
   Edit [`HomeController.java`](file:///c:/SNUC/DevOps/CIA2/Cloud-Pipeline/src/main/java/com/cloudpipeline/app/controller/HomeController.java) or [`application.properties`](file:///c:/SNUC/DevOps/CIA2/Cloud-Pipeline/src/main/resources/application.properties) (e.g., increment `app.version=1.0.1` or modify the welcome message).
2. **Commit and push:**
   ```bash
   git add .
   git commit -m "feat: release v1.0.1 with updated dashboard UI"
   git push origin main
   ```
3. **Automated Pipeline execution:**
   - Jenkins detects the push via Git webhook / polling.
   - Builds and passes automated tests.
   - Packages and creates Docker image: `cloudpipelineacr.azurecr.io/cloud-pipeline-app:12-a1b2c3d`.
   - Pushes image to ACR.
   - Executes rolling update in AKS: `kubectl set image deployment/cloud-pipeline-app cloud-pipeline-app=...`.
4. **Endpoint verification:**
   Refresh `http://<EXTERNAL_IP>` to see the updated version and message without any downtime.
