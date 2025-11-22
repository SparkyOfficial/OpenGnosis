# OpenGnosis Deployment Helper Script for PowerShell

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet('dev', 'staging', 'production')]
    [string]$Environment,
    
    [Parameter(Mandatory=$false)]
    [string]$Version = "latest",
    
    [Parameter(Mandatory=$false)]
    [string]$Namespace = "",
    
    [Parameter(Mandatory=$false)]
    [switch]$DryRun,
    
    [Parameter(Mandatory=$false)]
    [switch]$Help
)

$ErrorActionPreference = "Stop"

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Show-Usage {
    @"
Usage: .\deploy.ps1 -Environment <env> [OPTIONS]

Deploy OpenGnosis services to Kubernetes

PARAMETERS:
    -Environment <env>      Environment to deploy to (dev|staging|production)
    -Version <version>      Version tag to deploy (default: latest)
    -Namespace <ns>         Kubernetes namespace (overrides default)
    -DryRun                 Show what would be deployed without applying
    -Help                   Show this help message

EXAMPLES:
    # Deploy to dev environment
    .\deploy.ps1 -Environment dev

    # Deploy specific version to staging
    .\deploy.ps1 -Environment staging -Version v1.0.0

    # Dry run for production
    .\deploy.ps1 -Environment production -Version v1.0.0 -DryRun

"@
}

function Test-Prerequisites {
    Write-Info "Checking prerequisites..."
    
    if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
        Write-Error "kubectl not found. Please install kubectl."
        exit 1
    }
    
    if (-not (Get-Command kustomize -ErrorAction SilentlyContinue)) {
        Write-Error "kustomize not found. Please install kustomize."
        exit 1
    }
    
    Write-Info "Prerequisites check passed"
}

function Test-ClusterConnection {
    Write-Info "Checking cluster connection..."
    
    try {
        $null = kubectl cluster-info 2>&1
    } catch {
        Write-Error "Cannot connect to Kubernetes cluster"
        exit 1
    }
    
    $currentContext = kubectl config current-context
    Write-Info "Connected to cluster: $currentContext"
}

function Invoke-Deploy {
    param(
        [string]$Env,
        [string]$Ver,
        [string]$Ns,
        [bool]$Dry
    )
    
    Write-Info "Deploying to $Env environment"
    Write-Info "Version: $Ver"
    Write-Info "Namespace: $Ns"
    
    # Navigate to overlay directory
    $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
    $projectRoot = Split-Path -Parent $scriptDir
    $overlayDir = Join-Path $projectRoot "k8s\overlays\$Env"
    
    if (-not (Test-Path $overlayDir)) {
        Write-Error "Overlay directory not found: $overlayDir"
        exit 1
    }
    
    Push-Location $overlayDir
    
    try {
        # Update image tags if version specified
        if ($Ver -ne "latest") {
            Write-Info "Updating image tags to $Ver..."
            kustomize edit set image `
                opengnosis/gnosis-iam=opengnosis/gnosis-iam:$Ver `
                opengnosis/gnosis-api-gateway=opengnosis/gnosis-api-gateway:$Ver `
                opengnosis/gnosis-structure=opengnosis/gnosis-structure:$Ver `
                opengnosis/gnosis-scheduler=opengnosis/gnosis-scheduler:$Ver `
                opengnosis/gnosis-journal-command=opengnosis/gnosis-journal-command:$Ver `
                opengnosis/gnosis-analytics-query=opengnosis/gnosis-analytics-query:$Ver `
                opengnosis/gnosis-notifier=opengnosis/gnosis-notifier:$Ver
        }
        
        # Build kustomization
        Write-Info "Building kustomization..."
        if ($Dry) {
            kustomize build .
            Write-Info "Dry run completed. No changes applied."
        } else {
            kustomize build . | kubectl apply -f -
            
            Write-Info "Waiting for rollout to complete..."
            
            # Wait for all deployments
            $deployments = kubectl get deployments -n $Ns -o name
            foreach ($deployment in $deployments) {
                Write-Info "Waiting for $deployment..."
                kubectl rollout status $deployment -n $Ns --timeout=5m
            }
            
            Write-Info "Deployment completed successfully!"
            
            # Show pod status
            Write-Info "Pod status:"
            kubectl get pods -n $Ns
        }
    } finally {
        Pop-Location
    }
}

function Test-Deployment {
    param([string]$Ns)
    
    Write-Info "Verifying deployment..."
    
    # Check pod health
    $unhealthyPods = (kubectl get pods -n $Ns --field-selector=status.phase!=Running -o name).Count
    
    if ($unhealthyPods -gt 0) {
        Write-Warn "Found $unhealthyPods unhealthy pods"
        kubectl get pods -n $Ns --field-selector=status.phase!=Running
    } else {
        Write-Info "All pods are healthy"
    }
    
    # Check service endpoints
    Write-Info "Service endpoints:"
    kubectl get services -n $Ns
}

# Show help if requested
if ($Help) {
    Show-Usage
    exit 0
}

# Set default namespace if not provided
if ([string]::IsNullOrEmpty($Namespace)) {
    switch ($Environment) {
        'dev' { $Namespace = "opengnosis-dev" }
        'staging' { $Namespace = "opengnosis-staging" }
        'production' { $Namespace = "opengnosis-prod" }
    }
}

# Confirm production deployment
if ($Environment -eq 'production' -and -not $DryRun) {
    Write-Warn "You are about to deploy to PRODUCTION!"
    $confirm = Read-Host "Are you sure you want to continue? (yes/no)"
    if ($confirm -ne "yes") {
        Write-Info "Deployment cancelled"
        exit 0
    }
}

# Execute deployment
Test-Prerequisites
Test-ClusterConnection
Invoke-Deploy -Env $Environment -Ver $Version -Ns $Namespace -Dry $DryRun.IsPresent

if (-not $DryRun) {
    Test-Deployment -Ns $Namespace
}

Write-Info "Done!"
