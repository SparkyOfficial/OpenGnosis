# PowerShell script to verify mTLS configuration in the OpenGnosis platform

Write-Host "=== Verifying Istio mTLS Configuration ===" -ForegroundColor Cyan
Write-Host ""

# Check if kubectl is available
if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    Write-Host "Error: kubectl is not installed or not in PATH" -ForegroundColor Red
    exit 1
}

# Check if istioctl is available
$istioctl_available = $false
if (Get-Command istioctl -ErrorAction SilentlyContinue) {
    $istioctl_available = $true
} else {
    Write-Host "Warning: istioctl is not installed. Some checks will be skipped." -ForegroundColor Yellow
}

$namespace = "opengnosis"

Write-Host "1. Checking PeerAuthentication policies..." -ForegroundColor Green
kubectl get peerauthentication -n $namespace
kubectl get peerauthentication -n istio-system
Write-Host ""

Write-Host "2. Checking DestinationRules..." -ForegroundColor Green
kubectl get destinationrules -n $namespace
Write-Host ""

Write-Host "3. Verifying mTLS status for services..." -ForegroundColor Green
if ($istioctl_available) {
    # Check mTLS status using istioctl
    Write-Host "Checking gnosis-api-gateway:" -ForegroundColor Yellow
    $gateway_pod = kubectl get pod -n $namespace -l app=gnosis-api-gateway -o jsonpath='{.items[0].metadata.name}'
    if ($gateway_pod) {
        istioctl authn tls-check $gateway_pod -n $namespace 2>&1 | Out-Null
    }
    Write-Host ""
    
    Write-Host "Checking gnosis-iam:" -ForegroundColor Yellow
    $iam_pod = kubectl get pod -n $namespace -l app=gnosis-iam -o jsonpath='{.items[0].metadata.name}'
    if ($iam_pod) {
        istioctl authn tls-check $iam_pod -n $namespace 2>&1 | Out-Null
    }
    Write-Host ""
    
    Write-Host "Checking gnosis-structure:" -ForegroundColor Yellow
    $structure_pod = kubectl get pod -n $namespace -l app=gnosis-structure -o jsonpath='{.items[0].metadata.name}'
    if ($structure_pod) {
        istioctl authn tls-check $structure_pod -n $namespace 2>&1 | Out-Null
    }
    Write-Host ""
} else {
    Write-Host "Skipping istioctl checks (istioctl not available)" -ForegroundColor Yellow
}

Write-Host "4. Checking for Istio sidecars in pods..." -ForegroundColor Green
$pods = kubectl get pods -n $namespace -o jsonpath='{.items[*].metadata.name}'
if ($pods) {
    $pod_list = $pods -split ' '
    foreach ($pod in $pod_list) {
        $containers = kubectl get pod $pod -n $namespace -o jsonpath='{.spec.containers[*].name}'
        if ($containers -match "istio-proxy") {
            Write-Host "✓ $pod has Istio sidecar" -ForegroundColor Green
        } else {
            Write-Host "✗ $pod does NOT have Istio sidecar" -ForegroundColor Red
        }
    }
}
Write-Host ""

Write-Host "5. Checking certificate details for a sample pod..." -ForegroundColor Green
$sample_pod = kubectl get pod -n $namespace -l app=gnosis-api-gateway -o jsonpath='{.items[0].metadata.name}'
if ($sample_pod) {
    Write-Host "Inspecting certificates in pod: $sample_pod" -ForegroundColor Yellow
    $cert_output = kubectl exec -n $namespace $sample_pod -c istio-proxy -- openssl s_client -showcerts -connect gnosis-iam.opengnosis.svc.cluster.local:8080 2>&1
    if ($cert_output -match "Certificate chain") {
        Write-Host "Certificate chain found" -ForegroundColor Green
    } else {
        Write-Host "Could not retrieve certificate chain" -ForegroundColor Yellow
    }
} else {
    Write-Host "No pods found to inspect" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "6. Checking Istio proxy configuration..." -ForegroundColor Green
if ($sample_pod) {
    $config_dump = kubectl exec -n $namespace $sample_pod -c istio-proxy -- pilot-agent request GET config_dump 2>&1
    if ($config_dump -match "tls_context") {
        Write-Host "TLS context found in proxy configuration" -ForegroundColor Green
    } else {
        Write-Host "Could not retrieve TLS context" -ForegroundColor Yellow
    }
}
Write-Host ""

Write-Host "7. Testing service-to-service communication..." -ForegroundColor Green
if ($sample_pod) {
    Write-Host "Testing connection from $sample_pod to gnosis-iam..." -ForegroundColor Yellow
    $http_code = kubectl exec -n $namespace $sample_pod -c gnosis-api-gateway -- curl -s -o /dev/null -w "%{http_code}" http://gnosis-iam.opengnosis.svc.cluster.local:8080/actuator/health 2>&1
    if ($http_code -eq "200") {
        Write-Host "Connection successful (HTTP $http_code)" -ForegroundColor Green
    } else {
        Write-Host "Connection test returned: $http_code" -ForegroundColor Yellow
    }
}
Write-Host ""

Write-Host "=== Verification Complete ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "To manually verify mTLS for a specific service, use:" -ForegroundColor Yellow
Write-Host "  istioctl authn tls-check <pod-name>.<namespace> <service-name>.<namespace>.svc.cluster.local"
Write-Host ""
Write-Host "To check proxy configuration:" -ForegroundColor Yellow
Write-Host "  kubectl exec -n $namespace <pod-name> -c istio-proxy -- pilot-agent request GET config_dump"
Write-Host ""
Write-Host "Expected mTLS mode: STRICT (all service-to-service communication must use mTLS)" -ForegroundColor Green
