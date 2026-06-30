#!/usr/bin/env bash
set -euo pipefail

OPENCOST_RELEASE_NAME="${OPENCOST_RELEASE_NAME:-opencost}"
OPENCOST_NAMESPACE="${OPENCOST_NAMESPACE:-opencost}"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-60s}"
EXPECTED_KUBE_CONTEXT="${EXPECTED_KUBE_CONTEXT:-opspilot-aws-dev-eks}"
KUBE_CONTEXT="${KUBE_CONTEXT:-$(kubectl config current-context 2>/dev/null || true)}"

if [[ -z "${KUBE_CONTEXT}" ]]; then
  echo "No kubectl context is selected. Run aws eks update-kubeconfig first." >&2
  exit 1
fi

if [[ -n "${EXPECTED_KUBE_CONTEXT}" && "${KUBE_CONTEXT}" != "${EXPECTED_KUBE_CONTEXT}" ]]; then
  echo "Refusing to verify observability on unexpected kubectl context: ${KUBE_CONTEXT}" >&2
  echo "Expected: ${EXPECTED_KUBE_CONTEXT}" >&2
  echo "Set EXPECTED_KUBE_CONTEXT= to bypass this guard intentionally." >&2
  exit 1
fi

KUBECTL=(kubectl --context "${KUBE_CONTEXT}")

echo "Context: ${KUBE_CONTEXT}"
echo
"${KUBECTL[@]}" -n monitoring get deploy,ds,svc,pods -o wide

echo
"${KUBECTL[@]}" -n "${OPENCOST_NAMESPACE}" get deploy,svc,pods -o wide

echo
"${KUBECTL[@]}" -n monitoring rollout status deployment/prometheus --timeout="${WAIT_TIMEOUT}"
"${KUBECTL[@]}" -n monitoring rollout status deployment/kube-state-metrics --timeout="${WAIT_TIMEOUT}"
if "${KUBECTL[@]}" -n monitoring get daemonset/node-exporter >/dev/null 2>&1; then
  "${KUBECTL[@]}" -n monitoring rollout status daemonset/node-exporter --timeout="${WAIT_TIMEOUT}"
fi
"${KUBECTL[@]}" -n "${OPENCOST_NAMESPACE}" rollout status deployment/"${OPENCOST_RELEASE_NAME}" --timeout="${WAIT_TIMEOUT}"

echo
echo "Prometheus service:"
"${KUBECTL[@]}" -n monitoring get svc prometheus

echo
echo "OpenCost service:"
"${KUBECTL[@]}" -n "${OPENCOST_NAMESPACE}" get svc "${OPENCOST_RELEASE_NAME}"
