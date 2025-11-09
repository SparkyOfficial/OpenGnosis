# OpenGnosis: Enterprise Education Platform

OpenGnosis is an enterprise-grade education platform built with microservices architecture, Domain-Driven Design (DDD), Event-Driven Architecture (EDA), and CQRS patterns.

## Architecture Overview

The platform consists of the following microservices:

- **gnosis-iam**: Identity and Access Management Service
- **gnosis-api-gateway**: API Gateway for external clients
- **gnosis-structure**: Academic Structure Service (schools, classes, subjects)
- **gnosis-scheduler**: Scheduling Service (timetables, resource allocation)
- **gnosis-journal-command**: Journal Command Service (grades, attendance - write operations)
- **gnosis-analytics-query**: Analytics Query Service (reporting - read operations)
- **gnosis-notifier**: Notifications Service (email, push, SMS)

## Technology Stack

- **Language**: Kotlin 1.9.20
- **Framework**: Spring Boot 3.2.0
- **Build Tool**: Gradle 8.5
- **Message Broker**: Apache Kafka
- **Databases**: PostgreSQL, Elasticsearch, Redis
- **Container**: Docker
- **Orchestration**: Kubernetes
- **Observability**: Prometheus, Grafana, Jaeger

## Project Structure

```
opengnosis-platform/
├── shared/                      # Shared libraries
│   ├── domain/                  # Domain models
│   ├── events/                  # Event definitions
│   └── common/                  # Common utilities
├── services/                    # Microservices
│   ├── gnosis-iam/
│   ├── gnosis-api-gateway/
│   ├── gnosis-structure/
│   ├── gnosis-scheduler/
│   ├── gnosis-journal-command/
│   ├── gnosis-analytics-query/
│   └── gnosis-notifier/
├── docker/                      # Docker configurations
├── k8s/                         # Kubernetes manifests
└── build.gradle.kts             # Root build configuration
```

## Prerequisites

- JDK 17 or higher
- Docker and Docker Compose
- Kubernetes cluster (for production deployment)
- Gradle 8.5+ (or use the wrapper)

## Getting Started

### 1. Build the Project

```bash
# Build all modules
./gradlew build

# Build specific service
./gradlew :services:gnosis-iam:build
```

### 2. Start Infrastructure Services

```bash
# Start PostgreSQL, Redis, Kafka, Elasticsearch
cd docker
docker-compose up -d

# Verify services are running
docker-compose ps
```

### 3. Run Services Locally

```bash
# Run a specific service
./gradlew :services:gnosis-iam:bootRun

# Or build and run with Docker
docker build -f docker/service.Dockerfile --build-arg SERVICE_NAME=gnosis-iam -t opengnosis/gnosis-iam:latest .
docker run -p 8080:8080 opengnosis/gnosis-iam:latest
```

## Kubernetes Deployment

### 1. Create Namespace

```bash
kubectl apply -f k8s/namespace.yaml
```

### 2. Deploy Infrastructure

```bash
# Deploy PostgreSQL
kubectl apply -f k8s/infrastructure/postgres.yaml

# Deploy Redis
kubectl apply -f k8s/infrastructure/redis.yaml

# Deploy Kafka
kubectl apply -f k8s/infrastructure/kafka.yaml

# Deploy Elasticsearch
kubectl apply -f k8s/infrastructure/elasticsearch.yaml
```

### 3. Create ConfigMap and Secrets

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
```

### 4. Deploy Services

```bash
# Use the service template to create deployment manifests
# Replace SERVICE_NAME with actual service name
sed 's/SERVICE_NAME/gnosis-iam/g' k8s/service-template.yaml | kubectl apply -f -
```

## Development

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests for specific service
./gradlew :services:gnosis-iam:test

# Run with coverage
./gradlew test jacocoTestReport
```

### Code Quality

```bash
# Run Kotlin linter
./gradlew ktlintCheck

# Format code
./gradlew ktlintFormat
```

## Configuration

### Environment Variables

Each service can be configured using environment variables:

- `SPRING_PROFILES_ACTIVE`: Active Spring profile (dev, staging, production)
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka broker addresses
- `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`: PostgreSQL connection
- `REDIS_HOST`, `REDIS_PORT`: Redis connection
- `JWT_SECRET`: Secret key for JWT token signing
- `JWT_EXPIRATION`: Token expiration time in milliseconds

### Application Properties

Service-specific configuration is in `services/{service-name}/src/main/resources/application.yml`

## Monitoring and Observability

### Metrics

All services expose Prometheus metrics at `/actuator/prometheus`

### Health Checks

- Liveness: `/actuator/health/liveness`
- Readiness: `/actuator/health/readiness`

### Distributed Tracing

Traces are collected using OpenTelemetry and sent to Jaeger.

## API Documentation

API documentation is available at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8080/v3/api-docs`

## Contributing

1. Create a feature branch from `main`
2. Make your changes
3. Write tests
4. Ensure all tests pass
5. Submit a pull request

## License

Copyright © 2025 OpenGnosis. All rights reserved.
