#!/bin/bash

# Deploy Jaeger for Distributed Tracing
# This script deploys Jaeger operator and instance for the OpenGnosis platform

set -e

echo "========================================="
echo "Deploying Jaeger for Distributed Tracing"
echo "========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    print_error "kubectl is not installed. Please install kubectl first."
    exit 1
fi

# Check if helm is installed
if ! command -v helm &> /dev/null; then
    print_error "helm is not installed. Please install helm first."
    exit 1
fi

# Step 1: Create observability namespace
print_info "Creating observability namespace..."
kubectl create namespace observability --dry-run=client -o yaml | kubectl apply -f -

# Label namespace for Istio injection (optional)
kubectl label namespace observability istio-injection=enabled --overwrite

# Step 2: Add Jaeger Helm repository
print_info "Adding Jaeger Helm repository..."
helm repo add jaegertracing https://jaegertracing.github.io/helm-charts
helm repo update

# Step 3: Install Jaeger Operator
print_info "Installing Jaeger Operator..."
helm upgrade --install jaeger-operator jaegertracing/jaeger-operator \
  --namespace observability \
  --set rbac.clusterRole=true \
  --set jaeger.create=false \
  --wait \
  --timeout 5m

# Wait for operator to be ready
print_info "Waiting for Jaeger Operator to be ready..."
kubectl wait --for=condition=available --timeout=300s \
  deployment/jaeger-operator -n observability

# Step 4: Check if Elasticsearch is running
print_info "Checking Elasticsearch availability..."
if kubectl get service elasticsearch -n opengnosis &> /dev/null; then
    print_info "Elasticsearch service found in opengnosis namespace"
    ES_URL="http://elasticsearch.opengnosis.svc.cluster.local:9200"
elif kubectl get service elasticsearch -n default &> /dev/null; then
    print_info "Elasticsearch service found in default namespace"
    ES_URL="http://elasticsearch.default.svc.cluster.local:9200"
else
    print_warning "Elasticsearch service not found. Using in-memory storage."
    print_warning "For production, deploy Elasticsearch first: kubectl apply -f k8s/infrastructure/elasticsearch.yaml"
    ES_URL=""
fi

# Step 5: Deploy Jaeger instance
print_info "Deploying Jaeger instance..."
kubectl apply -f k8s/istio/jaeger-installation.yaml

# Wait for Jaeger components to be ready
print_info "Waiting for Jaeger components to be ready..."
kubectl wait --for=condition=available --timeout=300s \
  deployment -l app=jaeger -n observability || true

# Step 6: Configure Istio for tracing
print_info "Configuring Istio for distributed tracing..."
kubectl apply -f k8s/istio/tracing-config.yaml

# Step 7: Verify installation
print_info "Verifying Jaeger installation..."
echo ""
echo "Jaeger Pods:"
kubectl get pods -n observability -l app=jaeger

echo ""
echo "Jaeger Services:"
kubectl get svc -n observability -l app=jaeger

# Step 8: Get Jaeger UI access information
echo ""
print_info "========================================="
print_info "Jaeger Installation Complete!"
print_info "========================================="
echo ""

# Check if ingress is configured
if kubectl get ingress jaeger-ui -n observability &> /dev/null; then
    JAEGER_HOST=$(kubectl get ingress jaeger-ui -n observability -o jsonpath='{.spec.rules[0].host}')
    print_info "Jaeger UI is available at: https://${JAEGER_HOST}"
else
    print_info "To access Jaeger UI locally, run:"
    echo "  kubectl port-forward -n observability svc/jaeger-query 16686:16686"
    echo "  Then open: http://localhost:16686"
fi

echo ""
print_info "Jaeger Collector endpoint for services:"
echo "  http://jaeger-collector.observability.svc.cluster.local:9411"

echo ""
print_info "To verify tracing is working:"
echo "  1. Generate some traffic to your services"
echo "  2. Access Jaeger UI"
echo "  3. Select a service and click 'Find Traces'"

echo ""
print_info "Configuration files:"
echo "  - Jaeger installation: k8s/istio/jaeger-installation.yaml"
echo "  - Istio tracing config: k8s/istio/tracing-config.yaml"
echo "  - Documentation: k8s/istio/jaeger-tracing-guide.md"

echo ""
print_info "To adjust sampling rates, edit:"
echo "  - Development: k8s/istio/tracing-config.yaml (tracing-development)"
echo "  - Staging: k8s/istio/tracing-config.yaml (tracing-staging)"
echo "  - Production: k8s/istio/tracing-config.yaml (tracing-production)"

echo ""
print_info "Deployment complete! ✓"
