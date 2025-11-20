#!/bin/bash

# Verify Jaeger Distributed Tracing Setup
# This script verifies that distributed tracing is properly configured

set -e

echo "========================================="
echo "Verifying Jaeger Tracing Configuration"
echo "========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0

# Function to print colored output
print_pass() {
    echo -e "${GREEN}✓${NC} $1"
    ((PASSED++))
}

print_fail() {
    echo -e "${RED}✗${NC} $1"
    ((FAILED++))
}

print_info() {
    echo -e "${YELLOW}ℹ${NC} $1"
}

# Check 1: Jaeger Operator
echo ""
echo "1. Checking Jaeger Operator..."
if kubectl get deployment jaeger-operator -n observability &> /dev/null; then
    if kubectl get deployment jaeger-operator -n observability -o jsonpath='{.status.conditions[?(@.type=="Available")].status}' | grep -q "True"; then
        print_pass "Jaeger Operator is running"
    else
        print_fail "Jaeger Operator is not ready"
    fi
else
    print_fail "Jaeger Operator not found"
fi

# Check 2: Jaeger Instance
echo ""
echo "2. Checking Jaeger Instance..."
if kubectl get jaeger jaeger -n observability &> /dev/null; then
    print_pass "Jaeger instance exists"
    
    # Check Jaeger components
    if kubectl get deployment -l app=jaeger,component=collector -n observability &> /dev/null; then
        print_pass "Jaeger Collector deployment exists"
    else
        print_fail "Jaeger Collector deployment not found"
    fi
    
    if kubectl get deployment -l app=jaeger,component=query -n observability &> /dev/null; then
        print_pass "Jaeger Query deployment exists"
    else
        print_fail "Jaeger Query deployment not found"
    fi
else
    print_fail "Jaeger instance not found"
fi

# Check 3: Jaeger Services
echo ""
echo "3. Checking Jaeger Services..."
if kubectl get service jaeger-collector -n observability &> /dev/null; then
    print_pass "Jaeger Collector service exists"
    COLLECTOR_IP=$(kubectl get service jaeger-collector -n observability -o jsonpath='{.spec.clusterIP}')
    print_info "Collector IP: $COLLECTOR_IP"
else
    print_fail "Jaeger Collector service not found"
fi

if kubectl get service jaeger-query -n observability &> /dev/null; then
    print_pass "Jaeger Query service exists"
else
    print_fail "Jaeger Query service not found"
fi

# Check 4: Istio Tracing Configuration
echo ""
echo "4. Checking Istio Tracing Configuration..."
if kubectl get telemetry tracing-default -n istio-system &> /dev/null; then
    print_pass "Istio Telemetry resource exists"
    
    # Check if Jaeger provider is configured
    if kubectl get telemetry tracing-default -n istio-system -o yaml | grep -q "jaeger"; then
        print_pass "Jaeger provider configured in Istio"
    else
        print_fail "Jaeger provider not configured in Istio"
    fi
else
    print_fail "Istio Telemetry resource not found"
fi

# Check 5: Service Configuration
echo ""
echo "5. Checking Service Tracing Configuration..."

# Check if services have Istio sidecars
SERVICES=("gnosis-api-gateway" "gnosis-iam" "gnosis-structure" "gnosis-scheduler")
for service in "${SERVICES[@]}"; do
    if kubectl get deployment "$service" -n opengnosis &> /dev/null 2>&1; then
        CONTAINERS=$(kubectl get deployment "$service" -n opengnosis -o jsonpath='{.spec.template.spec.containers[*].name}')
        if echo "$CONTAINERS" | grep -q "istio-proxy"; then
            print_pass "$service has Istio sidecar"
        else
            print_fail "$service missing Istio sidecar"
        fi
    else
        print_info "$service deployment not found (may not be deployed yet)"
    fi
done

# Check 6: Trace Context Headers
echo ""
echo "6. Checking Trace Context Propagation..."
if kubectl get deployment gnosis-api-gateway -n opengnosis &> /dev/null 2>&1; then
    # Check if TracingContextFilter exists in the codebase
    if [ -f "services/gnosis-api-gateway/src/main/kotlin/com/opengnosis/gateway/filter/TracingContextFilter.kt" ]; then
        print_pass "TracingContextFilter exists in API Gateway"
    else
        print_fail "TracingContextFilter not found in API Gateway"
    fi
else
    print_info "API Gateway not deployed yet"
fi

# Check shared tracing components
if [ -f "shared/common/src/main/kotlin/com/opengnosis/common/tracing/TracingInterceptor.kt" ]; then
    print_pass "Shared TracingInterceptor exists"
else
    print_fail "Shared TracingInterceptor not found"
fi

# Check 7: Connectivity Test
echo ""
echo "7. Testing Jaeger Collector Connectivity..."
if kubectl get service jaeger-collector -n observability &> /dev/null; then
    # Create a test pod to check connectivity
    kubectl run tracing-test --image=curlimages/curl:latest --rm -i --restart=Never --command -- \
        curl -s -o /dev/null -w "%{http_code}" \
        http://jaeger-collector.observability.svc.cluster.local:9411/api/v2/spans \
        > /tmp/jaeger-test.txt 2>&1 || true
    
    if [ -f /tmp/jaeger-test.txt ]; then
        HTTP_CODE=$(cat /tmp/jaeger-test.txt)
        if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "202" ] || [ "$HTTP_CODE" = "405" ]; then
            print_pass "Jaeger Collector is reachable (HTTP $HTTP_CODE)"
        else
            print_fail "Jaeger Collector returned HTTP $HTTP_CODE"
        fi
        rm -f /tmp/jaeger-test.txt
    else
        print_info "Could not test connectivity (test pod may have failed)"
    fi
else
    print_fail "Cannot test connectivity - Jaeger Collector service not found"
fi

# Check 8: Elasticsearch Backend
echo ""
echo "8. Checking Elasticsearch Backend..."
if kubectl get jaeger jaeger -n observability -o yaml | grep -q "elasticsearch"; then
    print_pass "Jaeger configured with Elasticsearch backend"
    
    # Check if Elasticsearch is accessible
    if kubectl get service elasticsearch -n opengnosis &> /dev/null || kubectl get service elasticsearch -n default &> /dev/null; then
        print_pass "Elasticsearch service found"
    else
        print_fail "Elasticsearch service not found"
    fi
else
    print_info "Jaeger using in-memory storage (not recommended for production)"
fi

# Check 9: Sampling Configuration
echo ""
echo "9. Checking Sampling Configuration..."
NAMESPACES=("opengnosis" "opengnosis-dev" "opengnosis-staging")
for ns in "${NAMESPACES[@]}"; do
    if kubectl get namespace "$ns" &> /dev/null 2>&1; then
        if kubectl get telemetry -n "$ns" &> /dev/null 2>&1; then
            SAMPLING=$(kubectl get telemetry -n "$ns" -o jsonpath='{.items[0].spec.tracing[0].randomSamplingPercentage}' 2>/dev/null || echo "not set")
            print_pass "Sampling configured for $ns: $SAMPLING%"
        else
            print_info "No telemetry resource in $ns (will use default)"
        fi
    fi
done

# Check 10: Application Configuration
echo ""
echo "10. Checking Application Configuration..."
APP_CONFIGS=(
    "services/gnosis-iam/src/main/resources/application.yml"
    "services/gnosis-structure/src/main/resources/application.yml"
    "services/gnosis-scheduler/src/main/resources/application.yml"
    "services/gnosis-api-gateway/src/main/resources/application.yml"
)

for config in "${APP_CONFIGS[@]}"; do
    if [ -f "$config" ]; then
        if grep -q "management.tracing.sampling" "$config" || grep -q "management.zipkin" "$config"; then
            SERVICE_NAME=$(basename $(dirname $(dirname $(dirname "$config"))))
            print_pass "$SERVICE_NAME has tracing configuration"
        fi
    fi
done

# Summary
echo ""
echo "========================================="
echo "Verification Summary"
echo "========================================="
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Deploy your services: kubectl apply -f k8s/"
    echo "2. Generate some traffic to your services"
    echo "3. Access Jaeger UI to view traces:"
    echo "   kubectl port-forward -n observability svc/jaeger-query 16686:16686"
    echo "   Open: http://localhost:16686"
    exit 0
else
    echo -e "${RED}✗ Some checks failed. Please review the output above.${NC}"
    echo ""
    echo "Common fixes:"
    echo "1. Deploy Jaeger: ./k8s/istio/deploy-jaeger.sh"
    echo "2. Apply Istio config: kubectl apply -f k8s/istio/tracing-config.yaml"
    echo "3. Enable Istio injection: kubectl label namespace opengnosis istio-injection=enabled"
    echo "4. Restart services: kubectl rollout restart deployment -n opengnosis"
    exit 1
fi
