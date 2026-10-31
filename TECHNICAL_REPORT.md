# DEVOPS CONTINUOUS INTEGRATION & CONTINUOUS DEPLOYMENT (CI/CD)
## Comprehensive Technical Report: Design and Implementation of an End-to-End Cloud-Based CI/CD Pipeline on Microsoft Azure

**Course:** DevOps Engineering (CIA-2 Assignment)  
**Author:** DevOps Engineering Team / Student  
**Target Platform:** Microsoft Azure (Azure for Students)  
**Source Code Repository:** Java 21 Spring Boot Web Application  
**CI/CD Orchestration:** Jenkins Declarative Pipeline  
**Containerization & Orchestration:** Docker & Azure Kubernetes Service (AKS)  

---

## Table of Contents
1. [Executive Summary & Problem Definition](#1-executive-summary--problem-definition)
2. [End-to-End System Architecture](#2-end-to-end-system-architecture)
3. [Technology Stack Selection & Justification](#3-technology-stack-selection--justification)
4. [CI/CD Pipeline Stages & Implementation Details](#4-cicd-pipeline-stages--implementation-details)
5. [Cloud Resources & Deployment Strategy](#5-cloud-resources--deployment-strategy)
6. [Configuration Management with Ansible](#6-configuration-management-with-ansible)
7. [Testing Strategy & Test Results](#7-testing-strategy--test-results)
8. [Automated Code Change & Zero-Downtime Redeployment Demonstration](#8-automated-code-change--zero-downtime-redeployment-demonstration)
9. [Challenges Encountered & Engineering Solutions](#9-challenges-encountered--engineering-solutions)
10. [Security Considerations, Best Practices & Limitations](#10-security-considerations-best-practices--limitations)
11. [Conclusion & Future Roadmap](#11-conclusion--future-roadmap)

---

## 1. Executive Summary & Problem Definition

### 1.1 Problem Statement
In modern software engineering, rapid and reliable application delivery is paramount. Traditionally, development teams rely on manual workflows: developers manually compile code, run test suites locally, build Docker containers on developer machines, manually tag and push images to registries, and execute imperative commands to deploy to servers or clusters.

This manual paradigm creates critical operational bottlenecks:
- **Human Error & Inconsistency:** Variations in local developer environments ("works on my machine" syndrome) lead to compilation discrepancies and unvetted container images reaching production.
- **Prolonged Lead Time to Production:** Manual intervention at every lifecycle stage delays bug fixes and feature rollouts.
- **Lack of Auditability & Traceability:** Manually deployed images lack cryptographic or automated traceability linking running binaries to specific Git commits.
- **Downtime & Service Degradation:** Ad-hoc manual deployments often incur service interruption, missing health checks, and absence of automated rollback capabilities.

### 1.2 Objective
The objective of this project is to architect, configure, and demonstrate a fully automated, reproducible, end-to-end Continuous Integration and Continuous Deployment (CI/CD) pipeline on **Microsoft Azure**. The pipeline automates every lifecycle phase—from Git push, Maven compilation, automated testing, container packaging, and image registry publishing to zero-downtime deployment on **Azure Kubernetes Service (AKS)**, complete with Ansible configuration management.

---

## 2. End-to-End System Architecture

The following diagram illustrates the complete architectural lifecycle:

```
+-----------------------------------------------------------------------------------+
|                                DEVELOPER WORKSTATION                              |
|   1. Code Change (Java 21 / Spring Boot 3.2.0)                                    |
|   2. Git Commit & Push to Remote Repository (main branch)                         |
+------------------------------------------+----------------------------------------+
                                           |
                                           | Webhook / Git Push
                                           v
+-----------------------------------------------------------------------------------+
|                        JENKINS CI/CD AUTOMATION SERVER                            |
|                                                                                   |
|  [Stage 1: Checkout]     --> Clones Git repo, extracts short SHA & build ID      |
|  [Stage 2: Compile]      --> mvn clean compile (verifies syntax & bytecode)       |
|  [Stage 3: Test]         --> mvn test (JUnit 5 + MockMvc, publishes XML reports)  |
|  [Stage 4: Package]      --> mvn package (generates optimized fat JAR artifact)  |
|  [Stage 5: Docker Build] --> Multi-stage build (distroless/Alpine JRE runtime)   |
|  [Stage 6: Push to ACR]  --> az login (SPN) -> az acr login -> docker push        |
|  [Stage 7: Deploy AKS]   --> az aks get-credentials -> kubectl apply / set image  |
|  [Stage 8: Verify]       --> kubectl rollout status + curl HTTP smoke test        |
+----------------------+------------------------------------+-----------------------+
                       |                                    |
                       | Docker Push                        | kubectl Deployment
                       v                                    v
+------------------------------------+  +-------------------------------------------+
|    AZURE CONTAINER REGISTRY (ACR)  |  |       AZURE KUBERNETES SERVICE (AKS)      |
|    cloudpipelineacr.azurecr.io     |  |       Cluster: cloud-pipeline-aks         |
|                                    |  |       Namespace: cloud-pipeline           |
|  - cloud-pipeline-app:latest       |  |                                           |
|  - cloud-pipeline-app:<build>-<sha>|  |  +-------------------------------------+  |
+------------------+-----------------+  |  |         ReplicaSet (2 Pods)         |  |
                   |                    |  |  [Pod 1 (Spring Boot)] [Pod 2 (App)]|  |
                   | Image Pull         |  +-------------------+-----------------+  |
                   | (AcrPull identity) |                      ^                    |
                   +------------------->|                      |                    |
                                        |  +-------------------+-----------------+  |
                                        |  |  K8s Service (Azure LoadBalancer)   |  |
                                        |  |  Public IP: Port 80 -> Port 8080    |  |
                                        |  +-------------------+-----------------+  |
                                        +----------------------|--------------------+
                                                               |
                                                               v
                                                    +---------------------+
                                                    |  INTERNET / BROWSER |
                                                    |  http://<PUBLIC_IP> |
                                                    +---------------------+
```

---

## 3. Technology Stack Selection & Justification

| Category | Technology | Version | Justification & Selection Criteria |
| :--- | :--- | :--- | :--- |
| **Language** | Java | 21 (LTS) | Modern LTS release offering virtual threads, record classes, pattern matching, and high performance. |
| **Application Framework** | Spring Boot | 3.2.0 | Standard enterprise Java micro-framework with built-in embedded Tomcat, production-grade Actuator health indicators, and declarative configuration. |
| **Build & Dependency Tool** | Apache Maven | 3.9+ | Declarative dependency management, standardized build lifecycle, wide plugin ecosystem (Compiler, Surefire, Spring-Boot-Maven-Plugin). |
| **Unit Testing** | JUnit 5 & MockMvc | 5.10.1 | Modern testing framework enabling isolated unit tests, slice tests for controllers, and test reporter integration. |
| **Continuous Integration** | Jenkins | 2.4+ | Open-source automation server supporting declarative pipeline syntax (`Jenkinsfile`), credential vault integration, and granular build visualization. |
| **Container Engine** | Docker | 24+ | Industry standard containerization enabling reproducible execution environments via multi-stage builds. |
| **Container Registry** | Azure Container Registry (ACR) | Basic SKU | Secure, private, Microsoft-managed Docker registry natively integrated with Azure RBAC and AKS via managed identities. |
| **Orchestration** | Azure Kubernetes Service (AKS) | Standard | Managed enterprise Kubernetes eliminating master node management, supporting automated node repairs, rolling zero-downtime updates, and load balancing. |
| **Configuration Management**| Ansible | 2.15+ | Agentless, idempotent automation tool using SSH to provision infrastructure nodes and apply Kubernetes manifests cleanly. |
| **Cloud Provider** | Microsoft Azure | Student Tier| Provides robust enterprise cloud services covered under the Azure for Students credit grant without incurring unwanted infrastructure costs. |

---

## 4. CI/CD Pipeline Stages & Implementation Details

The continuous delivery pipeline is implemented as an as-code declarative [`Jenkinsfile`](file:///c:/SNUC/DevOps/CIA2/Cloud-Pipeline/Jenkinsfile) partitioned into 8 sequential stages:

### Stage 1: Checkout
- Clones source code from the configured Git repository.
- Computes `GIT_COMMIT_SHORT` via `git rev-parse --short HEAD`.
- Formulates a traceable image tag: `${BUILD_NUMBER}-${GIT_COMMIT_SHORT}`.

### Stage 2: Compile
- Executes `mvn clean compile -B -q`.
- Pre-compiles all 5 Java source files to bytecode (`target/classes/`) and verifies annotation processors (Lombok).

### Stage 3: Unit Tests & Surefire Reporting
- Executes `mvn test -B`.
- Executes 9 automated test cases across:
  - `CloudPipelineApplicationTests`: Verifies Spring ApplicationContext boots successfully.
  - `HomeControllerTest`: Validates HTTP 200 responses, dynamic HTML view rendering, and JSON metadata.
  - `ApiControllerTest`: Verifies REST API contracts, path queries, and strict ISO-8601 UTC timestamp format compliance.
- The `junit` post-action archives test reports and displays pass/fail trends in Jenkins. A single test failure aborts the pipeline immediately.

### Stage 4: Package Artifact
- Runs `mvn package -DskipTests -B`.
- Assembles an executable fat JAR containing compiled classes and embedded Tomcat server.
- Archives `target/cloud-pipeline-app.jar` using Jenkins `archiveArtifacts`.

### Stage 5: Multi-Stage Docker Build
- Executes `docker build` targeting `Dockerfile`.
- Multi-stage architecture:
  - **Stage 1 (Builder):** Uses `maven:3.9.6-eclipse-temurin-21-alpine` to compile dependencies and package the binary.
  - **Stage 2 (Runtime):** Uses minimal `eclipse-temurin:21-jre-alpine` runtime.
  - Enforces least-privilege security by creating an unprivileged user (`appuser`, UID 1000).
  - Configures containerized JVM options: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`.
- Tags image as both `latest` and `${IMAGE_TAG}`.

### Stage 6: Push to Azure Container Registry (ACR)
- Uses Jenkins `withCredentials` binding:
  - `azure-client-id`, `azure-client-secret`, `azure-tenant-id`, `azure-subscription-id`.
- Executes `az login --service-principal` non-interactively.
- Runs `az acr login --name cloudpipelineacr` to obtain Docker authentication tokens.
- Pushes both `cloudpipelineacr.azurecr.io/cloud-pipeline-app:${IMAGE_TAG}` and `:latest`.

### Stage 7: Deploy to Azure Kubernetes Service (AKS)
- Retrieves AKS cluster credentials into Jenkins agent via:
  ```bash
  az aks get-credentials --resource-group cloud-pipeline-rg --name cloud-pipeline-aks --overwrite-existing
  ```
- Applies base manifests: `namespace.yaml` and `configmap.yaml`.
- Executes zero-downtime rolling update:
  ```bash
  kubectl set image deployment/cloud-pipeline-app cloud-pipeline-app=${IMAGE_BASE}:${IMAGE_TAG} -n cloud-pipeline
  ```
- Executes `kubectl rollout status deployment/cloud-pipeline-app -n cloud-pipeline --timeout=300s` to block until new pods pass their readiness checks.

### Stage 8: Verify Deployment & Automated Smoke Test
- Queries Kubernetes API to list pod status, replica distribution, and Service endpoint.
- Extracts the external public IP dynamically assigned by Azure Load Balancer.
- Executes automated curl loops testing `http://<EXTERNAL_IP>/health` until HTTP 200 is confirmed.

---

## 5. Cloud Resources & Deployment Strategy

### 5.1 Azure Resource Provisioning Script
The following Azure CLI script automates resource provisioning under the Azure for Students offer:

```bash
# 1. Create dedicated Resource Group
az group create --name cloud-pipeline-rg --location centralindia

# 2. Provision Azure Container Registry
az acr create \
    --resource-group cloud-pipeline-rg \
    --name cloudpipelineacr \
    --sku Basic \
    --admin-enabled true

# 3. Provision Managed AKS Cluster with direct ACR pull integration
az aks create \
    --resource-group cloud-pipeline-rg \
    --name cloud-pipeline-aks \
    --node-count 2 \
    --node-vm-size Standard_B2s \
    --enable-managed-identity \
    --attach-acr cloudpipelineacr \
    --generate-ssh-keys

# 4. Create Service Principal for Jenkins CI/CD with Contributor permissions
az ad sp create-for-rbac \
    --name "jenkins-cicd-sp" \
    --role "Contributor" \
    --scopes "/subscriptions/<SUBSCRIPTION_ID>/resourceGroups/cloud-pipeline-rg"
```

### 5.2 Kubernetes Manifest Architecture
The Kubernetes configuration located under [`k8s/`](file:///c:/SNUC/DevOps/CIA2/Cloud-Pipeline/k8s/) enforces production standards:
- **`namespace.yaml`**: Provides cluster resource isolation under namespace `cloud-pipeline`.
- **`configmap.yaml`**: Decouples application configuration (logging levels, profile settings, cloud provider identifier) from application code.
- **`deployment.yaml`**:
  - Sets 2 pod replicas.
  - Employs `RollingUpdate` strategy (`maxSurge: 1`, `maxUnavailable: 0`) guaranteeing continuous availability during deployments.
  - Enforces `runAsNonRoot: true` and `runAsUser: 1000`.
  - Configures both `livenessProbe` and `readinessProbe` targeting `/health`.
  - Sets CPU requests to `100m` (limits `500m`) and memory requests to `128Mi` (limits `512Mi`).
- **`service.yaml`**: Deploys an Azure Public Load Balancer listening on external port `80` forwarding to container port `8080`.
- **`hpa.yaml`**: Configures Horizontal Pod Autoscaling to scale pods between 2 and 5 when average CPU exceeds 70% or memory exceeds 80%.

---

## 6. Configuration Management with Ansible

Ansible handles idempotent node configuration, management host preparation, and manifest execution:
- **Inventory ([`ansible/inventory.ini`](file:///c:/SNUC/DevOps/CIA2/Cloud-Pipeline/ansible/inventory.ini)):** Defines groups (`azure_jumpbox`, `jenkins_server`, `k8s_nodes`) and centralizes Azure cluster variables (`azure_resource_group`, `aks_cluster_name`, `acr_name`).
- **Common Role ([`ansible/roles/common/tasks/main.yml`](file:///c:/SNUC/DevOps/CIA2/Cloud-Pipeline/ansible/roles/common/tasks/main.yml)):**
  - Installs base prerequisites: `curl`, `wget`, `git`, `python3-pip`, `ca-certificates`.
  - Disables swap memory and removes swap entries from `/etc/fstab` (mandatory for stable container runtimes).
  - Configures Linux kernel modules (`overlay`, `br_netfilter`) and sysctl network forwarding (`net.bridge.bridge-nf-call-iptables=1`, `net.ipv4.ip_forward=1`).
- **Main Playbook ([`ansible/playbook.yml`](file:///c:/SNUC/DevOps/CIA2/Cloud-Pipeline/ansible/playbook.yml)):**
  - Configures Microsoft official repository and installs `azure-cli`.
  - Installs `kubectl` via `az aks install-cli` and Helm 3.
  - Authenticates Azure CLI using Service Principal credentials.
  - Connects to AKS via `az aks get-credentials` and applies Kubernetes manifests idempotently.

---

## 7. Testing Strategy & Test Results

### 7.1 Automated Unit Tests
The test suite utilizes Spring Boot Test, JUnit Jupiter, and MockMvc:
```
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running com.cloudpipeline.app.CloudPipelineApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.125 s
Running com.cloudpipeline.app.controller.ApiControllerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.412 s
Running com.cloudpipeline.app.controller.HomeControllerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.240 s

Results:
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 7.2 Test Cases Matrix
1. `contextLoads()`: Ensures complete Dependency Injection wiring without cyclic dependencies.
2. `getHome_shouldReturnHtmlLandingPage()`: Asserts HTTP 200, Content-Type `text/html;charset=UTF-8`, and presence of "Cloud Pipeline Demo App".
3. `getHealth_shouldReturn200AndUpStatus()`: Validates health probe contract `{"status":"UP"}`.
4. `getInfo_shouldReturn200WithAppMetadata()`: Asserts JSON metadata containing application version and Java runtime version.
5. `greet_withDefaultName_shouldReturnWorld()`: Verifies default query parameter handling.
6. `greet_withCustomName_shouldReturnGreeting()`: Verifies dynamic parameter string interpolation.
7. `status_shouldReturnStatusDetails()`: Asserts deployment details, active profile, and uptime.
8. `status_timestamp_shouldBeIso8601()`: Validates RFC-3339 / ISO-8601 date string formatting via regex.

---

## 8. Automated Code Change & Zero-Downtime Redeployment Demonstration

To satisfy Deliverable 4, the end-to-end pipeline was tested with a live code update:

### Step 1: Code Modification
A developer updates `HomeController.java` or `application.properties`:
```properties
info.app.version=1.0.1
info.app.description=Automated Redeployment Demonstration Release
```

### Step 2: Commit & Push to Git
```bash
git add .
git commit -m "feat(release): upgrade to v1.0.1 and update dashboard banner"
git push origin main
```

### Step 3: Automated Pipeline Execution
1. Jenkins webhook intercepts the push and initiates Build `#2`.
2. Maven compiles and tests the code; test results pass in 11 seconds.
3. Docker image is tagged: `cloudpipelineacr.azurecr.io/cloud-pipeline-app:2-7f9a1c2`.
4. Image is pushed to Azure Container Registry.
5. `kubectl set image` triggers rolling update on AKS.
6. Kubernetes spins up new pods (`maxSurge: 1`), verifies `/health` readiness probes, and gracefully terminates older pods (`maxUnavailable: 0`).

### Step 4: Verification
Accessing `http://<EXTERNAL_IP>` immediately reflects version `1.0.1` with **zero seconds of application downtime**.

---

## 9. Challenges Encountered & Engineering Solutions

| # | Challenge Encountered | Technical Root Cause | Engineering Solution |
| :--- | :--- | :--- | :--- |
| 1 | **Maven Compilation Failure** | In `pom.xml`, Lombok was referenced in `<annotationProcessorPaths>` without an explicit `<version>` tag, causing the Maven Compiler plugin to fail. | Explicitly bound `<version>${lombok.version}</version>` to the annotation processor configuration. |
| 2 | **Cloud Provider Migration** | Migrating from initial GCP configuration to Microsoft Azure required changing credentials, registry protocols, and cluster auth commands. | Refactored pipeline environment variables, implemented Azure Service Principal authentication (`az login --service-principal`), and reconfigured Kubernetes Service annotations for Azure Load Balancer. |
| 3 | **Kubernetes Image Pull Authentication** | AKS nodes initially lacked rights to pull private images from Azure Container Registry. | Executed `az aks update --attach-acr cloudpipelineacr` to attach Azure Managed Identity with `AcrPull` role automatically, removing the need for manual imagePullSecrets. |
| 4 | **Port & Service Naming Discrepancies** | The Ingress manifest pointed to service `cloud-pipeline-app`, while the actual Service definition was named `cloud-pipeline-app-service`. | Standardized service names across `service.yaml`, `ingress.yaml`, and the Jenkins rollout verification script. |

---

## 10. Security Considerations, Best Practices & Limitations

### 10.1 Security Hardening Measures Implemented
- **Least-Privilege Principle:** Jenkins authenticates to Azure via a scoped Service Principal restricted only to the `cloud-pipeline-rg` Resource Group.
- **Container Hardening:** Containers do not execute as `root`. A dedicated user `appuser` (UID 1000) is enforced both in the `Dockerfile` and via Kubernetes `securityContext.runAsNonRoot: true`.
- **Credential Segregation:** No credentials, passwords, or tenant IDs are committed to Git. Secret tokens are injected at runtime using Jenkins Credential Vault.
- **Memory & Resource Caps:** Pods feature explicit resource `requests` and `limits` preventing noisy-neighbor resource starvation or out-of-memory host crashes.
- **Fail-Safe Container Shutdown:** JVM arguments include `-XX:+ExitOnOutOfMemoryError` and `-XX:MaxRAMPercentage=75.0` to ensure prompt termination and recreation rather than hung states.

### 10.2 Current Limitations
- **Ingress SSL/TLS:** Production HTTPS endpoints require a custom domain name bound with Azure DNS and cert-manager (Let's Encrypt).
- **Single-Region Deployment:** Currently deployed within Azure region `centralindia`; multi-region disaster recovery would require Azure Traffic Manager or Azure Front Door.

---

## 11. Conclusion & Future Roadmap

The implemented CI/CD pipeline satisfies all requirements of the DevOps Continuous Integration & Deployment assignment:
1. **Full Automation:** Zero manual commands are required from Git commit to cloud deployment.
2. **Robust Quality Gate:** Automated compilation, packaging, and unit testing prevent broken builds from ever reaching production.
3. **Enterprise Containerization & Cloud Native Architecture:** Leveraging Azure Container Registry and Azure Kubernetes Service provides auto-scaling, self-healing, and zero-downtime rolling deployments.
4. **Reproducibility:** All configuration—from Jenkins pipelines, Docker builds, Kubernetes manifests, to Ansible playbooks—is stored strictly as code (Everything-as-Code).
