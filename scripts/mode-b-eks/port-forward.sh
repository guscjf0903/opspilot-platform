#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-opspilot}"
HELM_FULLNAME="${HELM_FULLNAME:-opspilot}"
LOCAL_ADDRESS="${LOCAL_ADDRESS:-127.0.0.1}"
LOCAL_PORT="${LOCAL_PORT:-8080}"
FRONTEND_SERVICE_PORT="${FRONTEND_SERVICE_PORT:-8080}"
EXPECTED_KUBE_CONTEXT="${EXPECTED_KUBE_CONTEXT:-opspilot-aws-dev-eks}"
KUBE_CONTEXT="${KUBE_CONTEXT:-$(kubectl config current-context 2>/dev/null || true)}"

if [[ -z "${KUBE_CONTEXT}" ]]; then
  echo "No kubectl context is selected. Run aws eks update-kubeconfig first." >&2
  exit 1
fi

if [[ -n "${EXPECTED_KUBE_CONTEXT}" && "${KUBE_CONTEXT}" != "${EXPECTED_KUBE_CONTEXT}" ]]; then
  echo "Refusing to port-forward unexpected kubectl context: ${KUBE_CONTEXT}" >&2
  echo "Expected: ${EXPECTED_KUBE_CONTEXT}" >&2
  echo "Set EXPECTED_KUBE_CONTEXT= to bypass this guard intentionally." >&2
  exit 1
fi

echo "Forwarding http://${LOCAL_ADDRESS}:${LOCAL_PORT} -> svc/${HELM_FULLNAME}-frontend:${FRONTEND_SERVICE_PORT}"
exec kubectl --context "${KUBE_CONTEXT}" -n "${NAMESPACE}" port-forward \
  --address "${LOCAL_ADDRESS}" \
  "svc/${HELM_FULLNAME}-frontend" \
  "${LOCAL_PORT}:${FRONTEND_SERVICE_PORT}"
