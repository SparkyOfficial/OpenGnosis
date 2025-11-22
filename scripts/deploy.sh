#!/bin/bash
# OpenGnosis Deployment Helper Script

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Functions
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

usage() {
    cat <<EOF
Usage: $0 [OPTIONS]

Deploy OpenGnosis services to Kubernetes

OPTIONS:
    -e, --environment ENV    Environment to deploy to (dev|staging|production)
    -v, --version VERSION    Version tag to deploy (default: latest)
    -n, --namespace NS       Kubernetes namespace (overrides default)
    -d, --dry-run           Show what would be deployed without applying
    -h, --help              Show this help message

EXAMPLES:
    # Deploy to dev environment
    $0 -e dev

    # Deploy specific version to staging
    $0 -e staging -v v1.0.0

    # Dry run for production
    $0 -e production -v v1.0.0 --dry-run

EOF
}

check_prerequisites() {
    print_info "Checking prerequisites..."
    
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl not found. Please install kubectl."
        exit 1
    fi
    
    if ! command -v kustomize &> /dev/null; then
        print_error "kustomize not found. Please install kustomize."
        exit 1
    fi
    
    print_info "Prerequisites check passed"
}

check_cluster_connection() {
    print_info "Checking cluster connection..."
    
    if ! kubectl cluster-info &> /dev/null; then
        print_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi
    
    CURRENT_CONTEXT=$(kubectl config current-context)
    print_info "Connected to cluster: $CURRENT_CONTEXT"
}

deploy() {
    local environment=$1
    local version=$2
    local namespace=$3
    local dry_run=$4
    
    print_info "Deploying to $environment environment"
    print_info "Version: $version"
    print_info "Namespace: $namespace"
    
    # Navigate to overlay directory
    OVERLAY_DIR="$PROJECT_ROOT/k8s/overlays/$environment"
    
    if [ ! -d "$OVERLAY_DIR" ]; then
        print_error "Overlay directory not found: $OVERLAY_DIR"
        exit 1
    fi
    
    cd "$OVERLAY_DIR"
    
    # Update image tags if version specified
    if [ "$version" != "latest" ]; then
        print_info "Updating image tags to $version..."
        kustomize edit set image \
            opengnosis/gnosis-iam=opengnosis/gnosis-iam:$version \
            opengnosis/gnosis-api-gateway=opengnosis/gnosis-api-gateway:$version \
            opengnosis/gnosis-structure=opengnosis/gnosis-structure:$version \
            opengnosis/gnosis-scheduler=opengnosis/gnosis-scheduler:$version \
            opengnosis/gnosis-journal-command=opengnosis/gnosis-journal-command:$version \
            opengnosis/gnosis-analytics-query=opengnosis/gnosis-analytics-query:$version \
            opengnosis/gnosis-notifier=opengnosis/gnosis-notifier:$version
    fi
    
    # Build kustomization
    print_info "Building kustomization..."
    if [ "$dry_run" = true ]; then
        kustomize build .
        print_info "Dry run completed. No changes applied."
    else
        kustomize build . | kubectl apply -f -
        
        print_info "Waiting for rollout to complete..."
        
        # Wait for all deployments
        DEPLOYMENTS=$(kubectl get deployments -n $namespace -o name)
        for deployment in $DEPLOYMENTS; do
            print_info "Waiting for $deployment..."
            kubectl rollout status $deployment -n $namespace --timeout=5m
        done
        
        print_info "Deployment completed successfully!"
        
        # Show pod status
        print_info "Pod status:"
        kubectl get pods -n $namespace
    fi
}

verify_deployment() {
    local namespace=$1
    
    print_info "Verifying deployment..."
    
    # Check pod health
    UNHEALTHY_PODS=$(kubectl get pods -n $namespace --field-selector=status.phase!=Running -o name | wc -l)
    
    if [ $UNHEALTHY_PODS -gt 0 ]; then
        print_warn "Found $UNHEALTHY_PODS unhealthy pods"
        kubectl get pods -n $namespace --field-selector=status.phase!=Running
    else
        print_info "All pods are healthy"
    fi
    
    # Check service endpoints
    print_info "Service endpoints:"
    kubectl get services -n $namespace
}

# Parse arguments
ENVIRONMENT=""
VERSION="latest"
NAMESPACE=""
DRY_RUN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -d|--dry-run)
            DRY_RUN=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Validate required arguments
if [ -z "$ENVIRONMENT" ]; then
    print_error "Environment is required"
    usage
    exit 1
fi

# Set default namespace if not provided
if [ -z "$NAMESPACE" ]; then
    case $ENVIRONMENT in
        dev)
            NAMESPACE="opengnosis-dev"
            ;;
        staging)
            NAMESPACE="opengnosis-staging"
            ;;
        production)
            NAMESPACE="opengnosis-prod"
            ;;
        *)
            print_error "Invalid environment: $ENVIRONMENT"
            exit 1
            ;;
    esac
fi

# Confirm production deployment
if [ "$ENVIRONMENT" = "production" ] && [ "$DRY_RUN" = false ]; then
    print_warn "You are about to deploy to PRODUCTION!"
    read -p "Are you sure you want to continue? (yes/no): " CONFIRM
    if [ "$CONFIRM" != "yes" ]; then
        print_info "Deployment cancelled"
        exit 0
    fi
fi

# Execute deployment
check_prerequisites
check_cluster_connection
deploy "$ENVIRONMENT" "$VERSION" "$NAMESPACE" "$DRY_RUN"

if [ "$DRY_RUN" = false ]; then
    verify_deployment "$NAMESPACE"
fi

print_info "Done!"
