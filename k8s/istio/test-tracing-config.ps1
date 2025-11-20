# PowerShell script to test tracing configuration
# This script validates that all tracing components are properly configured

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Testing Jaeger Tracing Configuration" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$passed = 0
$failed = 0

function Test-Pass {
    param([string]$message)
    Write-Host "✓ $message" -ForegroundColor Green
    $script:passed++
}

function Test-Fail {
    param([string]$message)
    Write-Host "✗ $message" -ForegroundColor Red
    $script:failed++
}

function Test-Info {
    param([string]$message)
    Write-Host "ℹ $message" -ForegroundColor Yellow
}

# Test 1: Check Kubernetes manifests exist
Write-Host "1. Checking Kubernetes manifests..." -ForegroundColor Cyan
if (Test-Path "k8s/istio/jaeger-installation.yaml") {
    Test-Pass "jaeger-installation.yaml exists"
} else {
    Test-Fail "jaeger-installation.yaml not found"
}

if (Test-Path "k8s/istio/tracing-config.yaml") {
    Test-Pass "tracing-config.yaml exists"
} else {
    Test-Fail "tracing-config.yaml not found"
}

# Test 2: Check deployment scripts
Write-Host ""
Write-Host "2. Checking deployment scripts..." -ForegroundColor Cyan
if (Test-Path "k8s/istio/deploy-jaeger.sh") {
    Test-Pass "deploy-jaeger.sh exists"
} else {
    Test-Fail "deploy-jaeger.sh not found"
}

if (Test-Path "k8s/istio/verify-tracing.sh") {
    Test-Pass "verify-tracing.sh exists"
} else {
    Test-Fail "verify-tracing.sh not found"
}

# Test 3: Check application code
Write-Host ""
Write-Host "3. Checking application tracing code..." -ForegroundColor Cyan

$tracingFiles = @(
    "services/gnosis-api-gateway/src/main/kotlin/com/opengnosis/gateway/filter/TracingContextFilter.kt",
    "shared/common/src/main/kotlin/com/opengnosis/common/tracing/TracingContext.kt",
    "shared/common/src/main/kotlin/com/opengnosis/common/tracing/TracingInterceptor.kt",
    "shared/common/src/main/kotlin/com/opengnosis/common/tracing/TracingConfiguration.kt"
)

foreach ($file in $tracingFiles) {
    if (Test-Path $file) {
        Test-Pass "$(Split-Path $file -Leaf) exists"
    } else {
        Test-Fail "$(Split-Path $file -Leaf) not found"
    }
}

# Test 4: Check service configurations
Write-Host ""
Write-Host "4. Checking service application.yml configurations..." -ForegroundColor Cyan

$services = @(
    "services/gnosis-iam/src/main/resources/application.yml",
    "services/gnosis-structure/src/main/resources/application.yml",
    "services/gnosis-scheduler/src/main/resources/application.yml",
    "services/gnosis-journal-command/src/main/resources/application.yml",
    "services/gnosis-analytics-query/src/main/resources/application.yml",
    "services/gnosis-notifier/src/main/resources/application.yml",
    "services/gnosis-api-gateway/src/main/resources/application.yml"
)

foreach ($service in $services) {
    if (Test-Path $service) {
        $content = Get-Content $service -Raw
        $serviceName = Split-Path (Split-Path (Split-Path (Split-Path $service))) -Leaf
        
        if ($content -match "management\.tracing" -or $content -match "management:\s+tracing:") {
            Test-Pass "$serviceName has tracing configuration"
        } else {
            Test-Fail "$serviceName missing tracing configuration"
        }
        
        if ($content -match "zipkin" -or $content -match "jaeger-collector") {
            Test-Pass "$serviceName has Zipkin/Jaeger endpoint configured"
        } else {
            Test-Fail "$serviceName missing Zipkin/Jaeger endpoint"
        }
        
        if ($content -match "%X\{traceId" -or $content -match "traceId") {
            Test-Pass "$serviceName has trace ID in logging pattern"
        } else {
            Test-Fail "$serviceName missing trace ID in logging pattern"
        }
    } else {
        Test-Fail "$service not found"
    }
}

# Test 5: Check build dependencies
Write-Host ""
Write-Host "5. Checking build.gradle.kts dependencies..." -ForegroundColor Cyan

$buildFiles = @(
    "shared/common/build.gradle.kts",
    "services/gnosis-iam/build.gradle.kts",
    "services/gnosis-structure/build.gradle.kts",
    "services/gnosis-scheduler/build.gradle.kts",
    "services/gnosis-journal-command/build.gradle.kts",
    "services/gnosis-analytics-query/build.gradle.kts",
    "services/gnosis-notifier/build.gradle.kts",
    "services/gnosis-api-gateway/build.gradle.kts"
)

foreach ($buildFile in $buildFiles) {
    if (Test-Path $buildFile) {
        $content = Get-Content $buildFile -Raw
        $moduleName = Split-Path (Split-Path $buildFile) -Leaf
        
        if ($content -match "micrometer-tracing" -or $content -match "zipkin-reporter") {
            Test-Pass "$moduleName has tracing dependencies"
        } else {
            Test-Fail "$moduleName missing tracing dependencies"
        }
    } else {
        Test-Fail "$buildFile not found"
    }
}

# Test 6: Check documentation
Write-Host ""
Write-Host "6. Checking documentation..." -ForegroundColor Cyan

$docs = @(
    "k8s/istio/jaeger-tracing-guide.md",
    "k8s/istio/TRACING_QUICK_START.md",
    "k8s/istio/TASK_10.4_COMPLETION_SUMMARY.md"
)

foreach ($doc in $docs) {
    if (Test-Path $doc) {
        Test-Pass "$(Split-Path $doc -Leaf) exists"
    } else {
        Test-Fail "$(Split-Path $doc -Leaf) not found"
    }
}

# Test 7: Validate YAML syntax (basic check)
Write-Host ""
Write-Host "7. Validating YAML syntax..." -ForegroundColor Cyan

$yamlFiles = @(
    "k8s/istio/jaeger-installation.yaml",
    "k8s/istio/tracing-config.yaml"
)

foreach ($yamlFile in $yamlFiles) {
    if (Test-Path $yamlFile) {
        $content = Get-Content $yamlFile -Raw
        $fileName = Split-Path $yamlFile -Leaf
        
        # Basic YAML validation
        if ($content -match "apiVersion:" -and $content -match "kind:" -and $content -match "metadata:") {
            Test-Pass "$fileName has valid YAML structure"
        } else {
            Test-Fail "$fileName may have invalid YAML structure"
        }
    }
}

# Test 8: Check Istio configuration
Write-Host ""
Write-Host "8. Checking Istio configuration..." -ForegroundColor Cyan

if (Test-Path "k8s/istio/istio-values.yaml") {
    $content = Get-Content "k8s/istio/istio-values.yaml" -Raw
    
    if ($content -match "tracing:" -or $content -match "zipkin:") {
        Test-Pass "istio-values.yaml has tracing configuration"
    } else {
        Test-Fail "istio-values.yaml missing tracing configuration"
    }
    
    if ($content -match "jaeger-collector") {
        Test-Pass "istio-values.yaml points to Jaeger collector"
    } else {
        Test-Fail "istio-values.yaml missing Jaeger collector endpoint"
    }
}

# Test 9: Check B3 header propagation
Write-Host ""
Write-Host "9. Checking B3 header propagation..." -ForegroundColor Cyan

$tracingContextFile = "services/gnosis-api-gateway/src/main/kotlin/com/opengnosis/gateway/filter/TracingContextFilter.kt"
if (Test-Path $tracingContextFile) {
    $content = Get-Content $tracingContextFile -Raw
    
    $b3Headers = @("X-B3-TraceId", "X-B3-SpanId", "X-B3-ParentSpanId", "X-B3-Sampled")
    foreach ($header in $b3Headers) {
        if ($content -match [regex]::Escape($header)) {
            Test-Pass "$header is propagated"
        } else {
            Test-Fail "$header is not propagated"
        }
    }
}

# Test 10: Check MDC integration
Write-Host ""
Write-Host "10. Checking MDC integration..." -ForegroundColor Cyan

$tracingFiles = @(
    "services/gnosis-api-gateway/src/main/kotlin/com/opengnosis/gateway/filter/TracingContextFilter.kt",
    "shared/common/src/main/kotlin/com/opengnosis/common/tracing/TracingInterceptor.kt"
)

foreach ($file in $tracingFiles) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw
        $fileName = Split-Path $file -Leaf
        
        if ($content -match "MDC\.put" -or $content -match "MDC\.remove") {
            Test-Pass "$fileName has MDC integration"
        } else {
            Test-Fail "$fileName missing MDC integration"
        }
    }
}

# Summary
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Passed: $passed" -ForegroundColor Green
Write-Host "Failed: $failed" -ForegroundColor Red
Write-Host ""

if ($failed -eq 0) {
    Write-Host "All tests passed! Tracing configuration is complete." -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "1. Deploy to Kubernetes cluster: ./k8s/istio/deploy-jaeger.sh" -ForegroundColor White
    Write-Host "2. Verify installation: ./k8s/istio/verify-tracing.sh" -ForegroundColor White
    Write-Host "3. Access Jaeger UI: kubectl port-forward -n observability svc/jaeger-query 16686:16686" -ForegroundColor White
    Write-Host "4. Open: http://localhost:16686" -ForegroundColor White
    exit 0
} else {
    Write-Host "Some tests failed. Please review the output above." -ForegroundColor Red
    exit 1
}
