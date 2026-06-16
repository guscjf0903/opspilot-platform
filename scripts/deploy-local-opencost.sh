#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONTEXT="${KUBE_CONTEXT:-kind-opspilot-demo}"
VALUES_FILE="${OPENCOST_VALUES_FILE:-${ROOT_DIR}/local/opencost/values-local.yml}"
RELEASE_NAME="${OPENCOST_RELEASE_NAME:-opencost}"
NAMESPACE="${OPENCOST_NAMESPACE:-opencost}"
CHART_REPO_NAME="${OPENCOST_CHART_REPO_NAME:-opencost-charts}"
CHART_REPO_URL="${OPENCOST_CHART_REPO_URL:-https://opencost.github.io/opencost-helm-chart}"
CHART_NAME="${OPENCOST_CHART_NAME:-opencost}"

helm repo add "${CHART_REPO_NAME}" "${CHART_REPO_URL}" >/dev/null
helm repo update "${CHART_REPO_NAME}" >/dev/null

helm upgrade --install "${RELEASE_NAME}" "${CHART_REPO_NAME}/${CHART_NAME}" \
  --namespace "${NAMESPACE}" \
  --create-namespace \
  --kube-context "${CONTEXT}" \
  -f "${VALUES_FILE}" \
  --wait \
  --timeout 5m

kubectl --context "${CONTEXT}" rollout status "deployment/${RELEASE_NAME}" -n "${NAMESPACE}"
