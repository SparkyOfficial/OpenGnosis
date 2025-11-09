# OpenGnosis Platform - Setup Guide

This document provides detailed setup instructions for the OpenGnosis Enterprise Education Platform.

## Task 1: Project Infrastructure Setup - COMPLETED ✓

The following components have been successfully created:

### 1. Multi-Module Gradle Project Structure ✓

The project uses Gradle with Kotlin DSL for build configuration:

```
opengnosis-platform/
├── build.gradle.kts              # Root build configuration
├── settings.gradle.kts           # Module definitions
├── gradle.properties             # Gradle settings
├── gradlew / gradlew.bat         # Gradle wrapper scripts
└── gradle/wrapper/               # Gradle wrapper files
```

**Modules Created:**
- `shared:domain` - Common domain models
- `shared:events` - Event definitions for EDA
- `shared:common` - Shared utilities and configurations
- `services:gnosis-iam` - Identity & Access Management
- `services:gnosis-api-gateway` - API Gateway
- `services:gnosis-structure` - Academic Structure Service
- `services:gnosis-scheduler` - Scheduling Service
- `services:gnosis-journal-command` - Journal Command Service
- `services:gnosis-analytics-query` - Analytics Query Service
- `services:gnosis-notifier` - Notifications Service

### 2. Shared Kotlin Libraries ✓

#### Domain Models (`shared/domain`)
- User, Role, UserStatus
- School, Class, Subject, Enrollment
- Schedule, ScheduleEntry, Classroom
- Journal commands (PlaceGradeCommand, MarkAttendanceCommand)
- Analytics read models (StudentGradesReadModel, StudentAttendanceReadModel)
- Notification models (NotificationPreferences, NotificationDelivery)

#### Events (`shared/events`)
- Base event infrastructure (DomainEvent, BaseDomainEvent)
- User events (UserRegisteredEvent, UserAuthenticatedEvent, UserRoleChangedEvent)
- Structure events (SchoolCreatedEvent, ClassCreatedEvent, StudentEnrolledEvent)
- Schedule events (ScheduleCreatedEvent, ScheduleModifiedEvent)
- Journal events (GradePlacedEvent, AttendanceMarkedEvent, HomeworkAssignedEvent)

#### Common Utilities (`shared/common`)
- JWT token provider for authentication
- Kafka configuration (producer and consumer)
- Event publisher for domain events
- Global exception handler
- Custom exception classes (ValidationException, ResourceNotFoundException, etc.)

### 3. Spring Boot 3 Configuration ✓

All services are configured with:
- Spring Boot 3.2.0
- Kotlin 1.9.20
- JDK 17
- Spring Data JPA (where applicable)
- Spring Kafka for event streaming
- Spring Security (for IAM service)
- Spring Cloud Gateway (for API Gateway)
- Spring WebFlux (for reactive services)
- Actuator for health checks and metrics
- Micrometer with Prometheus for observability

### 4. Docker Base Images ✓

Created Docker configurations:

**Base Image** (`docker/base/Dockerfile`):
- Based on Eclipse Temurin JRE 17 Alpine
- Non-root user for security
- Health check configuration
- Optimized for container environments

**Service Dockerfile** (`docker/service.Dockerfile`):
- Multi-stage build for efficiency
- Gradle build stage
- Minimal runtime stage
- Parameterized for all services

**Docker Compose** (`docker/docker-compose.yml`):
- PostgreSQL 16
- Redis 7
- Apache Kafka with Zookeeper
- Elasticsearch 8.11
- Pre-configured networking
- Health checks for all services

### 5. Kubernetes Resources ✓

Created Kubernetes manifests:

**Namespaces** (`k8s/namespace.yaml`):
- `opengnosis` - Production environment
- `opengnosis-dev` - Development environment
- `opengnosis-staging` - Staging environment

**Infrastructure** (`k8s/infrastructure/`):
- PostgreSQL StatefulSet with persistent storage
- Redis Deployment
- Kafka StatefulSet (3 brokers) with Zookeeper
- Elasticsearch StatefulSet with persistent storage

**Configuration** (`k8s/configmap.yaml` & `k8s/secrets.yaml`):
- ConfigMap for non-sensitive configuration
- Secrets for credentials and JWT keys
- Environment-specific settings

**Service Template** (`k8s/service-template.yaml`):
- Reusable template for all microservices
- Configured with:
  - Resource requests and limits
  - Liveness and readiness probes
  - Environment variables from ConfigMap and Secrets
  - Service discovery
  - Auto-scaling ready

## Build and Verification

### Prerequisites Check

Ensure you have the following installed:
- JDK 17 or higher
- Docker Desktop (for local development)
- kubectl (for Kubernetes deployment)

### Build the Project

```bash
# On Windows
gradlew.bat build

# On Linux/Mac
./gradlew build
```

### Start Infrastructure Locally

```bash
cd docker
docker-compose up -d
```

### Verify Infrastructure

```bash
# Check all services are running
docker-compose ps

# View logs
docker-compose logs -f
```

### Deploy to Kubernetes

```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Deploy infrastructure
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/infrastructure/

# Verify deployment
kubectl get pods -n opengnosis
```

## Project Structure Summary

```
opengnosis-platform/
├── shared/                          # Shared libraries
│   ├── domain/                      # Domain models (User, School, etc.)
│   ├── events/                      # Event definitions (DomainEvent, etc.)
│   └── common/                      # Utilities (JWT, Kafka, Exceptions)
│
├── services/                        # Microservices
│   ├── gnosis-iam/                  # Identity & Access Management
│   ├── gnosis-api-gateway/          # API Gateway
│   ├── gnosis-structure/            # Academic Structure
│   ├── gnosis-scheduler/            # Scheduling
│   ├── gnosis-journal-command/      # Journal Commands
│   ├── gnosis-analytics-query/      # Analytics Queries
│   └── gnosis-notifier/             # Notifications
│
├── docker/                          # Docker configurations
│   ├── base/                        # Base image
│   ├── service.Dockerfile           # Service build template
│   └── docker-compose.yml           # Local infrastructure
│
├── k8s/                             # Kubernetes manifests
│   ├── infrastructure/              # Infrastructure services
│   ├── namespace.yaml               # Namespaces
│   ├── configmap.yaml               # Configuration
│   ├── secrets.yaml                 # Secrets
│   └── service-template.yaml        # Service template
│
├── build.gradle.kts                 # Root build config
├── settings.gradle.kts              # Module definitions
├── Makefile                         # Build automation
├── README.md                        # Project documentation
└── .gitignore                       # Git ignore rules
```

## Next Steps

With the infrastructure and shared libraries in place, you can now proceed to:

1. **Task 2**: Implement core infrastructure services (Kafka, PostgreSQL, Redis, Elasticsearch)
2. **Task 3**: Implement gnosis-iam (Identity & Access Management Service)
3. **Task 4**: Implement gnosis-api-gateway (API Gateway)
4. And continue with subsequent tasks...

## Requirements Satisfied

This implementation satisfies the following requirements from the specification:

- **Requirement 11.1**: Docker containers for each service ✓
- **Requirement 11.2**: Kubernetes deployment configuration ✓
- **Requirement 11.4**: Infrastructure as code with Terraform-ready structure ✓

## Additional Resources

- **Makefile**: Use `make help` to see all available commands
- **README.md**: Comprehensive project documentation
- **Docker Compose**: Local development environment
- **Kubernetes Templates**: Production-ready manifests

---

**Status**: Task 1 - Project Infrastructure Setup - COMPLETED ✓

All components have been created and are ready for implementation of individual services.
