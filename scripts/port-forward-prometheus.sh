#!/usr/bin/env bash
set -euo pipefail

CONTEXT="${KUBE_CONTEXT:-kind-opspilot-demo}"
LOCAL_PORT="${PROMETHEUS_LOCAL_PORT:-9090}"

kubectl --context "${CONTEXT}" port-forward -n monitoring service/prometheus "${LOCAL_PORT}:9090"
