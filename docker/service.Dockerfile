# Multi-stage build for OpenGnosis services
# Build stage
FROM gradle:8.5-jdk17 AS builder

WORKDIR /build

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY shared/ ./shared/

# Copy service source
ARG SERVICE_NAME
COPY services/${SERVICE_NAME}/ ./services/${SERVICE_NAME}/

# Build the service
RUN gradle :services:${SERVICE_NAME}:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Install required packages
RUN apk add --no-cache curl bash

# Create application user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy built jar from builder
ARG SERVICE_NAME
COPY --from=builder /build/services/${SERVICE_NAME}/build/libs/*.jar app.jar

# Create directories and set permissions
RUN mkdir -p /app/config /app/logs && \
    chown -R appuser:appgroup /app

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
