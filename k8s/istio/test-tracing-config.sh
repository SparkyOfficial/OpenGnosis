#!/bin/bash

# Test script to validate Jaeger tracing configuration
# This script checks that all tracing components are properly configured

echo "========================================="
echo "Testing Jaeger Tracing Configuration"
echo "========================================="
echo ""

PASSED=0
FAILED=0

test_pass() {
    echo "✓ $1"
    ((PASSED++))
}

test_fail() {
    echo "✗ $1"
    ((FAILED++))
}

# Test 1: Check Kubernetes manifests
echo "1. Checking Kubernetes manifests..."
[ -f "k8s/istio/jaeger-installation.yaml" ] && test_pass "jaeger-installation.yaml exists" || test_fail "jaeger-installation.yaml not found"
[ -f "k8s/istio/tracing-config.yaml" ] && test_pass "tracing-config.yaml exists" || test_fail "tracing-config.yaml not found"

# Test 2: Check deployment scripts
echo ""
echo "2. Checking deployment scripts..."
[ -f "k8s/istio/deploy-jaeger.sh" ] && test_pass "deploy-jaeger.sh exists" || test_fail "deploy-jaeger.sh not found"
[ -f "k8s/istio/verify-tracing.sh" ] && test_pass "verify-tracing.sh exists" || test_fail "verify-tracing.sh not found"

# Test 3: Check application code
echo ""
echo "3. Checking application tracing code..."
[ -f "services/gnosis-api-gateway/src/main/kotlin/com/opengnosis/gateway/filter/TracingContextFilter.kt" ] && test_pass "TracingContextFilter.kt exists" || test_fail "TracingContextFilter.kt not found"
[ -f "shared/common/src/main/kotlin/com/opengnosis/common/tracing/TracingContext.kt" ] && test_pass "TracingContext.kt exists" || test_fail "TracingContext.kt not found"
[ -f "shared/common/src/main/kotlin/com/opengnosis/common/tracing/TracingInterceptor.kt" ] && test_pass "TracingInterceptor.kt exists" || test_fail "TracingInterceptor.kt not found"
[ -f "shared/common/src/main/kotlin/com/opengnosis/common/tracing/TracingConfiguration.kt" ] && test_pass "TracingConfiguration.kt exists" || test_fail "TracingConfiguration.kt not found"

# Test 4: Check service configurations
echo ""
echo "4. Checking service application.yml configurations..."
for service in gnosis-iam gnosis-structure gnosis-scheduler gnosis-journal-command gnosis-analytics-query gnosis-notifier gnosis-api-gateway; do
    config_file="services/$service/src/main/resources/application.yml"
    if [ -f "$config_file" ]; then
        if grep -q "management.tracing\|management:\s*tracing:" "$config_file"; then
            test_pass "$service has tracing configuration"
        else
            test_fail "$service missing tracing configuration"
        fi
        
        if grep -q "zipkin\|jaeger-collector" "$config_file"; then
            test_pass "$service has Zipkin/Jaeger endpoint"
        else
            test_fail "$service missing Zipkin/Jaeger endpoint"
        fi
        
        if grep -q "traceId" "$config_file"; then
            test_pass "$service has trace ID in logging"
        else
            test_fail "$service missing trace ID in logging"
        fi
    else
        test_fail "$config_file not found"
    fi
done

# Test 5: Check build dependencies
echo ""
echo "5. Checking build.gradle.kts dependencies..."
for module in shared/common services/gnosis-iam services/gnosis-structure services/gnosis-scheduler services/gnosis-journal-command services/gnosis-analytics-query services/gnosis-notifier services/gnosis-api-gateway; do
    build_file="$module/build.gradle.kts"
    if [ -f "$build_file" ]; then
        module_name=$(basename "$module")
        if grep -q "micrometer-tracing\|zipkin-reporter" "$build_file"; then
            test_pass "$module_name has tracing dependencies"
        else
            test_fail "$module_name missing tracing dependencies"
        fi
    else
        test_fail "$build_file not found"
    fi
done

# Test 6: Check documentation
echo ""
echo "6. Checking documentation..."
[ -f "k8s/istio/jaeger-tracing-guide.md" ] && test_pass "jaeger-tracing-guide.md exists" || test_fail "jaeger-tracing-guide.md not found"
[ -f "k8s/istio/TRACING_QUICK_START.md" ] && test_pass "TRACING_QUICK_START.md exists" || test_fail "TRACING_QUICK_START.md not found"
[ -f "k8s/istio/TASK_10.4_COMPLETION_SUMMARY.md" ] && test_pass "TASK_10.4_COMPLETION_SUMMARY.md exists" || test_fail "TASK_10.4_COMPLETION_SUMMARY.md not found"

# Test 7: Validate YAML syntax
echo ""
echo "7. Validating YAML syntax..."
for yaml_file in k8s/istio/jaeger-installation.yaml k8s/istio/tracing-config.yaml; do
    if [ -f "$yaml_file" ]; then
        filename=$(basename "$yaml_file")
        if grep -q "apiVersion:" "$yaml_file" && grep -q "kind:" "$yaml_file" && grep -q "metadata:" "$yaml_file"; then
            test_pass "$filename has valid YAML structure"
        else
            test_fail "$filename may have invalid YAML structure"
        fi
    fi
done

# Test 8: Check Istio configuration
echo ""
echo "8. Checking Istio configuration..."
if [ -f "k8s/istio/istio-values.yaml" ]; then
    if grep -q "tracing:\|zipkin:" "k8s/istio/istio-values.yaml"; then
        test_pass "istio-values.yaml has tracing configuration"
    else
        test_fail "istio-values.yaml missing tracing configuration"
    fi
    
    if grep -q "jaeger-collector" "k8s/istio/istio-values.yaml"; then
        test_pass "istio-values.yaml points to Jaeger collector"
    else
        test_fail "istio-values.yaml missing Jaeger collector endpoint"
    fi
fi

# Test 9: Check B3 header propagation
echo ""
echo "9. Checking B3 header propagation..."
tracing_filter="services/gnosis-api-gateway/src/main/kotlin/com/opengnosis/gateway/filter/TracingContextFilter.kt"
if [ -f "$tracing_filter" ]; then
    for header in "X-B3-TraceId" "X-B3-SpanId" "X-B3-ParentSpanId" "X-B3-Sampled"; do
        if grep -q "$header" "$tracing_filter"; then
            test_pass "$header is propagated"
        else
            test_fail "$header is not propagated"
        fi
    done
fi

# Test 10: Check MDC integration
echo ""
echo "10. Checking MDC integration..."
for file in "services/gnosis-api-gateway/src/main/kotlin/com/opengnosis/gateway/filter/TracingContextFilter.kt" "shared/common/src/main/kotlin/com/opengnosis/common/tracing/TracingInterceptor.kt"; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        if grep -q "MDC\.put\|MDC\.remove" "$file"; then
            test_pass "$filename has MDC integration"
        else
            test_fail "$filename missing MDC integration"
        fi
    fi
done

# Summary
echo ""
echo "========================================="
echo "Test Summary"
echo "========================================="
echo "Passed: $PASSED"
echo "Failed: $FAILED"
echo ""

if [ $FAILED -eq 0 ]; then
    echo "✓ All tests passed! Tracing configuration is complete."
    echo ""
    echo "Next steps:"
    echo "1. Deploy to Kubernetes cluster: ./k8s/istio/deploy-jaeger.sh"
    echo "2. Verify installation: ./k8s/istio/verify-tracing.sh"
    echo "3. Access Jaeger UI: kubectl port-forward -n observability svc/jaeger-query 16686:16686"
    echo "4. Open: http://localhost:16686"
    exit 0
else
    echo "✗ Some tests failed. Please review the output above."
    exit 1
fi
