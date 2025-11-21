#!/bin/bash

# Deploy OpenGnosis Observability Stack
# This script deploys Prometheus, Grafana, Alertmanager, Loki, and Promtail

set -e

echo "=========================================="
echo "Deploying OpenGnosis Observability Stack"
echo "=========================================="

# Create monitoring namespace
echo ""
echo "Creating monitoring namespace..."
kubectl apply -f - <<EOF
apiVersion: v1
kind: Namespace
metadata:
  name: monitoring
  labels:
    name: monitoring
EOF

# Deploy Prometheus Operator
echo ""
echo "Deploying Prometheus Operator..."
kubectl apply -f prometheus-operator.yaml

# Wait for Prometheus Operator to be ready
echo "Waiting for Prometheus Operator to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/prometheus-operator -n monitoring

# Deploy Prometheus
echo ""
echo "Deploying Prometheus..."
kubectl apply -f prometheus.yaml

# Deploy ServiceMonitors
echo ""
echo "Deploying ServiceMonitors..."
kubectl apply -f service-monitors.yaml

# Deploy Prometheus Rules
echo ""
echo "Deploying Prometheus Rules..."
kubectl apply -f prometheus-rules.yaml

# Deploy Alertmanager
echo ""
echo "Deploying Alertmanager..."
kubectl apply -f alertmanager.yaml

# Deploy Grafana
echo ""
echo "Deploying Grafana..."
kubectl apply -f grafana.yaml
kubectl apply -f grafana-dashboards.yaml

# Wait for Grafana to be ready
echo "Waiting for Grafana to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/grafana -n monitoring

# Deploy Loki
echo ""
echo "Deploying Loki..."
kubectl apply -f loki.yaml

# Wait for Loki to be ready
echo "Waiting for Loki to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/loki -n monitoring

# Deploy Promtail
echo ""
echo "Deploying Promtail..."
kubectl apply -f promtail.yaml

# Wait for Promtail to be ready
echo "Waiting for Promtail DaemonSet to be ready..."
kubectl rollout status daemonset/promtail -n monitoring --timeout=300s

echo ""
echo "=========================================="
echo "Observability Stack Deployment Complete!"
echo "=========================================="
echo ""
echo "Access the services:"
echo ""
echo "Prometheus:"
echo "  kubectl port-forward -n monitoring svc/prometheus 9090:9090"
echo "  Then open: http://localhost:9090"
echo ""
echo "Grafana:"
echo "  kubectl port-forward -n monitoring svc/grafana 3000:3000"
echo "  Then open: http://localhost:3000"
echo "  Username: admin"
echo "  Password: opengnosis-grafana-2024"
echo ""
echo "Alertmanager:"
echo "  kubectl port-forward -n monitoring svc/alertmanager 9093:9093"
echo "  Then open: http://localhost:9093"
echo ""
echo "Loki:"
echo "  kubectl port-forward -n monitoring svc/loki 3100:3100"
echo "  Then open: http://localhost:3100"
echo ""
echo "To view logs in Grafana:"
echo "  1. Open Grafana"
echo "  2. Go to Explore"
echo "  3. Select 'Loki' as the data source"
echo "  4. Use LogQL queries like: {namespace=\"opengnosis\", app=\"gnosis-api-gateway\"}"
echo ""
echo "To configure Slack/PagerDuty notifications:"
echo "  1. Edit k8s/infrastructure/alertmanager.yaml"
echo "  2. Update the webhook URLs and service keys"
echo "  3. Reapply: kubectl apply -f k8s/infrastructure/alertmanager.yaml"
echo ""
