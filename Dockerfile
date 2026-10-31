# ============================================================
# Dockerfile for Cloud Pipeline Demo App
# Minimal JRE runtime image using pre-built fat JAR from Jenkins
# ============================================================

FROM eclipse-temurin:21-jre-alpine

# Metadata labels
LABEL maintainer="devops-team@cloudpipeline.com" \
      description="Cloud Pipeline Demo App" \
      org.opencontainers.image.source="https://github.com/aerosibin/Cloud-pipeline"

# Create a non-root user and group for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the fresh fat JAR compiled by Jenkins in Stage 4
COPY --chown=appuser:appgroup target/cloud-pipeline-app.jar app.jar

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Health check — Docker will mark container unhealthy if this fails
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# JVM tuning for containers: respect cgroup memory limits
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Start the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]