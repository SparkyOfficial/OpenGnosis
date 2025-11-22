#!/bin/bash
# Build all OpenGnosis service Docker images locally

set -e

SERVICES=(
    "gnosis-iam"
    "gnosis-api-gateway"
    "gnosis-structure"
    "gnosis-scheduler"
    "gnosis-journal-command"
    "gnosis-analytics-query"
    "gnosis-notifier"
)

VERSION=${1:-latest}
REGISTRY=${2:-localhost:5000}

echo "Building OpenGnosis Docker images..."
echo "Version: $VERSION"
echo "Registry: $REGISTRY"
echo ""

for SERVICE in "${SERVICES[@]}"; do
    echo "Building $SERVICE..."
    docker build \
        --build-arg SERVICE_NAME=$SERVICE \
        -t $REGISTRY/$SERVICE:$VERSION \
        -t $REGISTRY/$SERVICE:latest \
        -f service.Dockerfile \
        ..
    echo "✅ $SERVICE built successfully"
    echo ""
done

echo "All images built successfully!"
echo ""
echo "To push images to registry, run:"
for SERVICE in "${SERVICES[@]}"; do
    echo "  docker push $REGISTRY/$SERVICE:$VERSION"
done
