#!/bin/bash
# Script to verify mTLS configuration in the OpenGnosis platform

set -e

echo "=== Verifying Istio mTLS Configuration ==="
echo ""

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo "Error: kubectl is not installed or not in PATH"
    exit 1
fi

# Check if istioctl is available
if ! command -v istioctl &> /dev/null; then
    echo "Warning: istioctl is not installed. Some checks will be skipped."
    ISTIOCTL_AVAILABLE=false
else
    ISTIOCTL_AVAILABLE=true
fi

NAMESPACE="opengnosis"

echo "1. Checking PeerAuthentication policies..."
kubectl get peerauthentication -n $NAMESPACE
kubectl get peerauthentication -n istio-system
echo ""

echo "2. Checking DestinationRules..."
kubectl get destinationrules -n $NAMESPACE
echo ""

echo "3. Verifying mTLS status for services..."
if [ "$ISTIOCTL_AVAILABLE" = true ]; then
    # Check mTLS status using istioctl
    echo "Checking gnosis-api-gateway:"
    istioctl authn tls-check $(kubectl get pod -n $NAMESPACE -l app=gnosis-api-gateway -o jsonpath='{.items[0].metadata.name}') -n $NAMESPACE || true
    echo ""
    
    echo "Checking gnosis-iam:"
    istioctl authn tls-check $(kubectl get pod -n $NAMESPACE -l app=gnosis-iam -o jsonpath='{.items[0].metadata.name}') -n $NAMESPACE || true
    echo ""
    
    echo "Checking gnosis-structure:"
    istioctl authn tls-check $(kubectl get pod -n $NAMESPACE -l app=gnosis-structure -o jsonpath='{.items[0].metadata.name}') -n $NAMESPACE || true
    echo ""
else
    echo "Skipping istioctl checks (istioctl not available)"
fi

echo "4. Checking for Istio sidecars in pods..."
PODS=$(kubectl get pods -n $NAMESPACE -o jsonpath='{.items[*].metadata.name}')
for POD in $PODS; do
    CONTAINERS=$(kubectl get pod $POD -n $NAMESPACE -o jsonpath='{.spec.containers[*].name}')
    if echo "$CONTAINERS" | grep -q "istio-proxy"; then
        echo "✓ $POD has Istio sidecar"
    else
        echo "✗ $POD does NOT have Istio sidecar"
    fi
done
echo ""

echo "5. Checking certificate details for a sample pod..."
SAMPLE_POD=$(kubectl get pod -n $NAMESPACE -l app=gnosis-api-gateway -o jsonpath='{.items[0].metadata.name}')
if [ -n "$SAMPLE_POD" ]; then
    echo "Inspecting certificates in pod: $SAMPLE_POD"
    kubectl exec -n $NAMESPACE $SAMPLE_POD -c istio-proxy -- openssl s_client -showcerts -connect gnosis-iam.opengnosis.svc.cluster.local:8080 < /dev/null 2>&1 | grep -A 5 "Certificate chain" || echo "Could not retrieve certificate chain"
else
    echo "No pods found to inspect"
fi
echo ""

echo "6. Checking Istio proxy configuration..."
if [ -n "$SAMPLE_POD" ]; then
    kubectl exec -n $NAMESPACE $SAMPLE_POD -c istio-proxy -- pilot-agent request GET config_dump | grep -A 10 "tls_context" | head -20 || echo "Could not retrieve TLS context"
fi
echo ""

echo "7. Testing service-to-service communication..."
if [ -n "$SAMPLE_POD" ]; then
    echo "Testing connection from $SAMPLE_POD to gnosis-iam..."
    kubectl exec -n $NAMESPACE $SAMPLE_POD -c gnosis-api-gateway -- curl -s -o /dev/null -w "%{http_code}" http://gnosis-iam.opengnosis.svc.cluster.local:8080/actuator/health || echo "Connection test failed"
fi
echo ""

echo "=== Verification Complete ==="
echo ""
echo "To manually verify mTLS for a specific service, use:"
echo "  istioctl authn tls-check <pod-name>.<namespace> <service-name>.<namespace>.svc.cluster.local"
echo ""
echo "To check proxy configuration:"
echo "  kubectl exec -n $NAMESPACE <pod-name> -c istio-proxy -- pilot-agent request GET config_dump"
echo ""
echo "Expected mTLS mode: STRICT (all service-to-service communication must use mTLS)"
