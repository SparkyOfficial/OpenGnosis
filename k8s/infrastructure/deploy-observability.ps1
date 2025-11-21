# Deploy OpenGnosis Observability Stack
# This script deploys Prometheus, Grafana, Alertmanager, Loki, and Promtail

$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Deploying OpenGnosis Observability Stack" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# Create monitoring namespace
Write-Host ""
Write-Host "Creating monitoring namespace..." -ForegroundColor Yellow
@"
apiVersion: v1
kind: Namespace
metadata:
  name: monitoring
  labels:
    name: monitoring
"@ | kubectl apply -f -

# Deploy Prometheus Operator
Write-Host ""
Write-Host "Deploying Prometheus Operator..." -ForegroundColor Yellow
kubectl apply -f prometheus-operator.yaml

# Wait for Prometheus Operator to be ready
Write-Host "Waiting for Prometheus Operator to be ready..." -ForegroundColor Yellow
kubectl wait --for=condition=available --timeout=300s deployment/prometheus-operator -n monitoring

# Deploy Prometheus
Write-Host ""
Write-Host "Deploying Prometheus..." -ForegroundColor Yellow
kubectl apply -f prometheus.yaml

# Deploy ServiceMonitors
Write-Host ""
Write-Host "Deploying ServiceMonitors..." -ForegroundColor Yellow
kubectl apply -f service-monitors.yaml

# Deploy Prometheus Rules
Write-Host ""
Write-Host "Deploying Prometheus Rules..." -ForegroundColor Yellow
kubectl apply -f prometheus-rules.yaml

# Deploy Alertmanager
Write-Host ""
Write-Host "Deploying Alertmanager..." -ForegroundColor Yellow
kubectl apply -f alertmanager.yaml

# Deploy Grafana
Write-Host ""
Write-Host "Deploying Grafana..." -ForegroundColor Yellow
kubectl apply -f grafana.yaml
kubectl apply -f grafana-dashboards.yaml

# Wait for Grafana to be ready
Write-Host "Waiting for Grafana to be ready..." -ForegroundColor Yellow
kubectl wait --for=condition=available --timeout=300s deployment/grafana -n monitoring

# Deploy Loki
Write-Host ""
Write-Host "Deploying Loki..." -ForegroundColor Yellow
kubectl apply -f loki.yaml

# Wait for Loki to be ready
Write-Host "Waiting for Loki to be ready..." -ForegroundColor Yellow
kubectl wait --for=condition=available --timeout=300s deployment/loki -n monitoring

# Deploy Promtail
Write-Host ""
Write-Host "Deploying Promtail..." -ForegroundColor Yellow
kubectl apply -f promtail.yaml

# Wait for Promtail to be ready
Write-Host "Waiting for Promtail DaemonSet to be ready..." -ForegroundColor Yellow
kubectl rollout status daemonset/promtail -n monitoring --timeout=300s

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "Observability Stack Deployment Complete!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Access the services:" -ForegroundColor Cyan
Write-Host ""
Write-Host "Prometheus:" -ForegroundColor Yellow
Write-Host "  kubectl port-forward -n monitoring svc/prometheus 9090:9090"
Write-Host "  Then open: http://localhost:9090"
Write-Host ""
Write-Host "Grafana:" -ForegroundColor Yellow
Write-Host "  kubectl port-forward -n monitoring svc/grafana 3000:3000"
Write-Host "  Then open: http://localhost:3000"
Write-Host "  Username: admin"
Write-Host "  Password: opengnosis-grafana-2024"
Write-Host ""
Write-Host "Alertmanager:" -ForegroundColor Yellow
Write-Host "  kubectl port-forward -n monitoring svc/alertmanager 9093:9093"
Write-Host "  Then open: http://localhost:9093"
Write-Host ""
Write-Host "Loki:" -ForegroundColor Yellow
Write-Host "  kubectl port-forward -n monitoring svc/loki 3100:3100"
Write-Host "  Then open: http://localhost:3100"
Write-Host ""
Write-Host "To view logs in Grafana:" -ForegroundColor Cyan
Write-Host "  1. Open Grafana"
Write-Host "  2. Go to Explore"
Write-Host "  3. Select 'Loki' as the data source"
Write-Host "  4. Use LogQL queries like: {namespace=`"opengnosis`", app=`"gnosis-api-gateway`"}"
Write-Host ""
Write-Host "To configure Slack/PagerDuty notifications:" -ForegroundColor Cyan
Write-Host "  1. Edit k8s/infrastructure/alertmanager.yaml"
Write-Host "  2. Update the webhook URLs and service keys"
Write-Host "  3. Reapply: kubectl apply -f k8s/infrastructure/alertmanager.yaml"
Write-Host ""
