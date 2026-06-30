#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-opspilot}"
RELEASE_NAME="${RELEASE_NAME:-opspilot}"
EXPECTED_KUBE_CONTEXT="${EXPECTED_KUBE_CONTEXT:-opspilot-aws-dev-eks}"
KUBE_CONTEXT="${KUBE_CONTEXT:-$(kubectl config current-context 2>/dev/null || true)}"

if [[ -z "${KUBE_CONTEXT}" ]]; then
  echo "No kubectl context is selected. Run aws eks update-kubeconfig first." >&2
  exit 1
fi

if [[ -n "${EXPECTED_KUBE_CONTEXT}" && "${KUBE_CONTEXT}" != "${EXPECTED_KUBE_CONTEXT}" ]]; then
  echo "Refusing to run Helm test on unexpected kubectl context: ${KUBE_CONTEXT}" >&2
  echo "Expected: ${EXPECTED_KUBE_CONTEXT}" >&2
  echo "Set EXPECTED_KUBE_CONTEXT= to bypass this guard intentionally." >&2
  exit 1
fi

helm --kube-context "${KUBE_CONTEXT}" test "${RELEASE_NAME}" \
  -n "${NAMESPACE}" \
  --logs
