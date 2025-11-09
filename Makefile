.PHONY: build test clean docker-up docker-down k8s-deploy k8s-clean

# Build all services
build:
	./gradlew build

# Build specific service
build-service:
	./gradlew :services:$(SERVICE):build

# Run all tests
test:
	./gradlew test

# Clean build artifacts
clean:
	./gradlew clean

# Start infrastructure services with Docker Compose
docker-up:
	cd docker && docker-compose up -d

# Stop infrastructure services
docker-down:
	cd docker && docker-compose down

# View Docker logs
docker-logs:
	cd docker && docker-compose logs -f

# Build Docker image for a service
docker-build:
	docker build -f docker/service.Dockerfile --build-arg SERVICE_NAME=$(SERVICE) -t opengnosis/$(SERVICE):latest .

# Build all Docker images
docker-build-all:
	@for service in gnosis-iam gnosis-api-gateway gnosis-structure gnosis-scheduler gnosis-journal-command gnosis-analytics-query gnosis-notifier; do \
		echo "Building $$service..."; \
		docker build -f docker/service.Dockerfile --build-arg SERVICE_NAME=$$service -t opengnosis/$$service:latest .; \
	done

# Create Kubernetes namespace
k8s-namespace:
	kubectl apply -f k8s/namespace.yaml

# Deploy infrastructure to Kubernetes
k8s-infra:
	kubectl apply -f k8s/configmap.yaml
	kubectl apply -f k8s/secrets.yaml
	kubectl apply -f k8s/infrastructure/postgres.yaml
	kubectl apply -f k8s/infrastructure/redis.yaml
	kubectl apply -f k8s/infrastructure/kafka.yaml
	kubectl apply -f k8s/infrastructure/elasticsearch.yaml

# Deploy a specific service to Kubernetes
k8s-deploy-service:
	sed 's/SERVICE_NAME/$(SERVICE)/g' k8s/service-template.yaml | kubectl apply -f -

# Deploy all services to Kubernetes
k8s-deploy-all: k8s-namespace k8s-infra
	@for service in gnosis-iam gnosis-api-gateway gnosis-structure gnosis-scheduler gnosis-journal-command gnosis-analytics-query gnosis-notifier; do \
		echo "Deploying $$service..."; \
		sed "s/SERVICE_NAME/$$service/g" k8s/service-template.yaml | kubectl apply -f -; \
	done

# Clean Kubernetes resources
k8s-clean:
	kubectl delete namespace opengnosis --ignore-not-found=true

# View Kubernetes pods
k8s-pods:
	kubectl get pods -n opengnosis

# View Kubernetes services
k8s-services:
	kubectl get services -n opengnosis

# View logs for a specific pod
k8s-logs:
	kubectl logs -n opengnosis -l app=$(SERVICE) -f

# Run a specific service locally
run-service:
	./gradlew :services:$(SERVICE):bootRun

# Format code
format:
	./gradlew ktlintFormat

# Check code style
lint:
	./gradlew ktlintCheck

# Generate test coverage report
coverage:
	./gradlew test jacocoTestReport

# Help
help:
	@echo "OpenGnosis Platform - Available Commands:"
	@echo ""
	@echo "Build Commands:"
	@echo "  make build                    - Build all services"
	@echo "  make build-service SERVICE=x  - Build specific service"
	@echo "  make test                     - Run all tests"
	@echo "  make clean                    - Clean build artifacts"
	@echo ""
	@echo "Docker Commands:"
	@echo "  make docker-up                - Start infrastructure services"
	@echo "  make docker-down              - Stop infrastructure services"
	@echo "  make docker-logs              - View Docker logs"
	@echo "  make docker-build SERVICE=x   - Build Docker image for service"
	@echo "  make docker-build-all         - Build all Docker images"
	@echo ""
	@echo "Kubernetes Commands:"
	@echo "  make k8s-namespace            - Create Kubernetes namespace"
	@echo "  make k8s-infra                - Deploy infrastructure"
	@echo "  make k8s-deploy-service SERVICE=x - Deploy specific service"
	@echo "  make k8s-deploy-all           - Deploy all services"
	@echo "  make k8s-clean                - Clean Kubernetes resources"
	@echo "  make k8s-pods                 - View pods"
	@echo "  make k8s-services             - View services"
	@echo "  make k8s-logs SERVICE=x       - View logs for service"
	@echo ""
	@echo "Development Commands:"
	@echo "  make run-service SERVICE=x    - Run service locally"
	@echo "  make format                   - Format code"
	@echo "  make lint                     - Check code style"
	@echo "  make coverage                 - Generate test coverage"
