#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CONTEXT="${KUBE_CONTEXT:-kind-opspilot-demo}"

kubectl --context "${CONTEXT}" apply -k "${ROOT_DIR}/local/prometheus"
kubectl --context "${CONTEXT}" rollout status deployment/prometheus -n monitoring
kubectl --context "${CONTEXT}" rollout status deployment/kube-state-metrics -n monitoring
kubectl --context "${CONTEXT}" rollout status daemonset/node-exporter -n monitoring
