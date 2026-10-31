# ============================================================
# Multi-Stage Dockerfile for Cloud Pipeline Demo App
# Stage 1: Build the fat JAR using Maven
# Stage 2: Minimal JRE runtime image with non-root user
# ============================================================

# ---- Stage 1: Build ----
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copy dependency manifest first for layer caching
COPY pom.xml .

# Download all Maven dependencies (cached as a layer when pom.xml unchanged)
RUN mvn dependency:go-offline -B

# Copy source code and build the fat JAR (skip tests — tests run in CI)
COPY src ./src
RUN mvn package -DskipTests -B && \
    mv target/cloud-pipeline-app.jar target/app.jar

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine

# Metadata labels
LABEL maintainer="devops-team@cloudpipeline.com" \
      version="1.0.0" \
      description="Cloud Pipeline Demo App" \
      org.opencontainers.image.source="https://github.com/your-org/cloud-pipeline-app"

# Create a non-root user and group for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the fat JAR from the builder stage
COPY --from=builder --chown=appuser:appgroup /workspace/target/app.jar app.jar

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Health check — Docker will mark the container unhealthy if this fails
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# JVM tuning for containers: respect cgroup memory limits
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Start the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
