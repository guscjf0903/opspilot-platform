#!/usr/bin/env bash
set -euo pipefail

CONTEXT="${KUBE_CONTEXT:-kind-opspilot-demo}"
NAMESPACE="${OPENCOST_NAMESPACE:-opencost}"
SERVICE_NAME="${OPENCOST_SERVICE_NAME:-opencost}"
API_LOCAL_PORT="${OPENCOST_API_LOCAL_PORT:-9003}"
UI_LOCAL_PORT="${OPENCOST_UI_LOCAL_PORT:-9091}"

kubectl --context "${CONTEXT}" port-forward -n "${NAMESPACE}" \
  "service/${SERVICE_NAME}" \
  "${API_LOCAL_PORT}:9003" \
  "${UI_LOCAL_PORT}:9090"
