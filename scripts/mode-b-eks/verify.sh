#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-opspilot}"
SAMPLE_NAMESPACE="${SAMPLE_NAMESPACE:-sample-app}"
HELM_FULLNAME="${HELM_FULLNAME:-opspilot}"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-60s}"
VERIFY_OBSERVABILITY="${VERIFY_OBSERVABILITY:-true}"
EXPECTED_KUBE_CONTEXT="${EXPECTED_KUBE_CONTEXT:-opspilot-aws-dev-eks}"
KUBE_CONTEXT="${KUBE_CONTEXT:-$(kubectl config current-context 2>/dev/null || true)}"

if [[ -z "${KUBE_CONTEXT}" ]]; then
  echo "No kubectl context is selected. Run aws eks update-kubeconfig first." >&2
  exit 1
fi

if [[ -n "${EXPECTED_KUBE_CONTEXT}" && "${KUBE_CONTEXT}" != "${EXPECTED_KUBE_CONTEXT}" ]]; then
  echo "Refusing to verify unexpected kubectl context: ${KUBE_CONTEXT}" >&2
  echo "Expected: ${EXPECTED_KUBE_CONTEXT}" >&2
  echo "Set EXPECTED_KUBE_CONTEXT= to bypass this guard intentionally." >&2
  exit 1
fi

KUBECTL=(kubectl --context "${KUBE_CONTEXT}")

echo "Context: ${KUBE_CONTEXT}"
echo
"${KUBECTL[@]}" get nodes -o wide

echo
if [[ "${VERIFY_OBSERVABILITY}" == "true" ]]; then
  if "${KUBECTL[@]}" get namespace monitoring >/dev/null 2>&1; then
    "${KUBECTL[@]}" -n monitoring get deploy,ds,svc,pods -o wide
  else
    echo "monitoring namespace is not present. Run ./scripts/mode-b-eks/deploy-observability.sh first."
  fi

  echo
  if "${KUBECTL[@]}" get namespace opencost >/dev/null 2>&1; then
    "${KUBECTL[@]}" -n opencost get deploy,svc,pods -o wide
  else
    echo "opencost namespace is not present. Run ./scripts/mode-b-eks/deploy-observability.sh first."
  fi
fi

echo
"${KUBECTL[@]}" -n "${NAMESPACE}" get deploy,svc,pods -o wide

echo
"${KUBECTL[@]}" -n "${SAMPLE_NAMESPACE}" get deploy,svc,pods -o wide

echo
"${KUBECTL[@]}" -n "${NAMESPACE}" rollout status deploy/opspilot-postgresql --timeout="${WAIT_TIMEOUT}"
"${KUBECTL[@]}" -n "${NAMESPACE}" rollout status deploy/"${HELM_FULLNAME}-backend" --timeout="${WAIT_TIMEOUT}"
"${KUBECTL[@]}" -n "${NAMESPACE}" rollout status deploy/"${HELM_FULLNAME}-frontend" --timeout="${WAIT_TIMEOUT}"
if [[ "${VERIFY_OBSERVABILITY}" == "true" ]]; then
  if "${KUBECTL[@]}" get namespace monitoring >/dev/null 2>&1; then
    "${KUBECTL[@]}" -n monitoring rollout status deploy/prometheus --timeout="${WAIT_TIMEOUT}"
    "${KUBECTL[@]}" -n monitoring rollout status deploy/kube-state-metrics --timeout="${WAIT_TIMEOUT}"
    if "${KUBECTL[@]}" -n monitoring get daemonset/node-exporter >/dev/null 2>&1; then
      "${KUBECTL[@]}" -n monitoring rollout status daemonset/node-exporter --timeout="${WAIT_TIMEOUT}"
    fi
  fi

  if "${KUBECTL[@]}" get namespace opencost >/dev/null 2>&1; then
    "${KUBECTL[@]}" -n opencost rollout status deploy/opencost --timeout="${WAIT_TIMEOUT}"
  fi
fi

for deployment in redpanda order-producer order-consumer; do
  if "${KUBECTL[@]}" -n "${SAMPLE_NAMESPACE}" get deploy/"${deployment}" >/dev/null 2>&1; then
    "${KUBECTL[@]}" -n "${SAMPLE_NAMESPACE}" rollout status deploy/"${deployment}" --timeout="${WAIT_TIMEOUT}"
  else
    echo "Skipping ${SAMPLE_NAMESPACE}/${deployment}; Kafka demo is not deployed."
  fi
done

echo
echo "Expected degraded demo workloads:"
echo "- sample-app/payment-api has a failing readiness probe."
echo "- sample-app/worker intentionally CrashLoops."
echo "- Kafka demo is optional and disabled by default in Mode B EKS."
