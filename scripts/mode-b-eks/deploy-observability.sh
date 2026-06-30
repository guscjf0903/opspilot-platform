#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

PROMETHEUS_MANIFEST_DIR="${PROMETHEUS_MANIFEST_DIR:-${ROOT_DIR}/local/prometheus}"
OPENCOST_VALUES_FILE="${OPENCOST_VALUES_FILE:-${ROOT_DIR}/deploy/helm/opencost/values-aws-dev-eks.yaml}"
OPENCOST_RELEASE_NAME="${OPENCOST_RELEASE_NAME:-opencost}"
OPENCOST_NAMESPACE="${OPENCOST_NAMESPACE:-opencost}"
OPENCOST_CHART_REPO_NAME="${OPENCOST_CHART_REPO_NAME:-opencost-charts}"
OPENCOST_CHART_REPO_URL="${OPENCOST_CHART_REPO_URL:-https://opencost.github.io/opencost-helm-chart}"
OPENCOST_CHART_NAME="${OPENCOST_CHART_NAME:-opencost}"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-5m}"
ENABLE_NODE_EXPORTER="${ENABLE_NODE_EXPORTER:-false}"
EXPECTED_KUBE_CONTEXT="${EXPECTED_KUBE_CONTEXT:-opspilot-aws-dev-eks}"
KUBE_CONTEXT="${KUBE_CONTEXT:-$(kubectl config current-context 2>/dev/null || true)}"

if [[ -z "${KUBE_CONTEXT}" ]]; then
  echo "No kubectl context is selected. Run aws eks update-kubeconfig first." >&2
  exit 1
fi

if [[ -n "${EXPECTED_KUBE_CONTEXT}" && "${KUBE_CONTEXT}" != "${EXPECTED_KUBE_CONTEXT}" ]]; then
  echo "Refusing to deploy observability to unexpected kubectl context: ${KUBE_CONTEXT}" >&2
  echo "Expected: ${EXPECTED_KUBE_CONTEXT}" >&2
  echo "Set EXPECTED_KUBE_CONTEXT= to bypass this guard intentionally." >&2
  exit 1
fi

KUBECTL=(kubectl --context "${KUBE_CONTEXT}")

echo "Deploying Mode B EKS observability"
echo "Context: ${KUBE_CONTEXT}"
echo "Prometheus manifests: ${PROMETHEUS_MANIFEST_DIR}"
echo "OpenCost values: ${OPENCOST_VALUES_FILE}"
echo "Node exporter enabled: ${ENABLE_NODE_EXPORTER}"

if [[ "${ENABLE_NODE_EXPORTER}" == "true" ]]; then
  "${KUBECTL[@]}" apply -k "${PROMETHEUS_MANIFEST_DIR}"
else
  for manifest in \
    namespace.yml \
    prometheus-rbac.yml \
    prometheus-config.yml \
    prometheus-deployment.yml \
    prometheus-service.yml \
    kube-state-metrics-rbac.yml \
    kube-state-metrics-deployment.yml \
    kube-state-metrics-service.yml
  do
    "${KUBECTL[@]}" apply -f "${PROMETHEUS_MANIFEST_DIR}/${manifest}"
  done
  "${KUBECTL[@]}" -n monitoring delete daemonset/node-exporter service/node-exporter --ignore-not-found
fi
"${KUBECTL[@]}" -n monitoring rollout status deployment/prometheus --timeout="${WAIT_TIMEOUT}"
"${KUBECTL[@]}" -n monitoring rollout status deployment/kube-state-metrics --timeout="${WAIT_TIMEOUT}"
if "${KUBECTL[@]}" -n monitoring get daemonset/node-exporter >/dev/null 2>&1; then
  "${KUBECTL[@]}" -n monitoring rollout status daemonset/node-exporter --timeout="${WAIT_TIMEOUT}"
fi

helm repo add "${OPENCOST_CHART_REPO_NAME}" "${OPENCOST_CHART_REPO_URL}" >/dev/null
helm repo update "${OPENCOST_CHART_REPO_NAME}" >/dev/null

helm --kube-context "${KUBE_CONTEXT}" upgrade --install "${OPENCOST_RELEASE_NAME}" "${OPENCOST_CHART_REPO_NAME}/${OPENCOST_CHART_NAME}" \
  --namespace "${OPENCOST_NAMESPACE}" \
  --create-namespace \
  -f "${OPENCOST_VALUES_FILE}" \
  --wait \
  --timeout "${WAIT_TIMEOUT}"

"${KUBECTL[@]}" -n "${OPENCOST_NAMESPACE}" rollout status deployment/"${OPENCOST_RELEASE_NAME}" --timeout="${WAIT_TIMEOUT}"

echo
"${KUBECTL[@]}" -n monitoring get deploy,ds,svc,pods -o wide
"${KUBECTL[@]}" -n "${OPENCOST_NAMESPACE}" get deploy,svc,pods -o wide

echo
echo "Mode B EKS observability deployment completed."
echo "If OpsPilot was already deployed, run ./scripts/mode-b-eks/deploy.sh again to refresh backend env values."
