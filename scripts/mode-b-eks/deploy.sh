#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

IMAGE_TAG="${1:-${IMAGE_TAG:-}}"
if [[ -z "${IMAGE_TAG}" ]]; then
  echo "Usage: IMAGE_TAG=<commit-sha> $0" >&2
  echo "   or: $0 <commit-sha>" >&2
  exit 1
fi

AWS_REGION="${AWS_REGION:-ap-northeast-2}"
NAMESPACE="${NAMESPACE:-opspilot}"
SAMPLE_NAMESPACE="${SAMPLE_NAMESPACE:-sample-app}"
RELEASE_NAME="${RELEASE_NAME:-opspilot}"
HELM_FULLNAME="${HELM_FULLNAME:-opspilot}"
BACKEND_SECRET_NAME="${BACKEND_SECRET_NAME:-opspilot-backend-secret}"
VALUES_FILE="${VALUES_FILE:-${ROOT_DIR}/deploy/helm/opspilot/values-aws-dev-eks.yaml}"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-240s}"
WAIT_DEMO_WORKLOADS="${WAIT_DEMO_WORKLOADS:-true}"
DEPLOY_OBSERVABILITY="${DEPLOY_OBSERVABILITY:-false}"
EXPECTED_KUBE_CONTEXT="${EXPECTED_KUBE_CONTEXT:-opspilot-aws-dev-eks}"
KUBE_CONTEXT="${KUBE_CONTEXT:-$(kubectl config current-context 2>/dev/null || true)}"

if [[ -z "${KUBE_CONTEXT}" ]]; then
  echo "No kubectl context is selected. Run aws eks update-kubeconfig first." >&2
  exit 1
fi

if [[ -n "${EXPECTED_KUBE_CONTEXT}" && "${KUBE_CONTEXT}" != "${EXPECTED_KUBE_CONTEXT}" ]]; then
  echo "Refusing to deploy to unexpected kubectl context: ${KUBE_CONTEXT}" >&2
  echo "Expected: ${EXPECTED_KUBE_CONTEXT}" >&2
  echo "Set EXPECTED_KUBE_CONTEXT= to bypass this guard intentionally." >&2
  exit 1
fi

KUBECTL=(kubectl --context "${KUBE_CONTEXT}")
HELM=(helm --kube-context "${KUBE_CONTEXT}")
AWS_ARGS=(--region "${AWS_REGION}")
if [[ -n "${AWS_PROFILE:-}" ]]; then
  AWS_ARGS+=(--profile "${AWS_PROFILE}")
fi

ACCOUNT_ID="$(aws "${AWS_ARGS[@]}" sts get-caller-identity --query Account --output text)"
ECR_REGISTRY="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
BACKEND_IMAGE_REPOSITORY="${BACKEND_IMAGE_REPOSITORY:-${ECR_REGISTRY}/opspilot-backend}"
FRONTEND_IMAGE_REPOSITORY="${FRONTEND_IMAGE_REPOSITORY:-${ECR_REGISTRY}/opspilot-frontend}"

echo "Deploying OpsPilot Mode B EKS"
echo "Context: ${KUBE_CONTEXT}"
echo "Namespace: ${NAMESPACE}"
echo "Image tag: ${IMAGE_TAG}"
echo "Backend image: ${BACKEND_IMAGE_REPOSITORY}:${IMAGE_TAG}"
echo "Frontend image: ${FRONTEND_IMAGE_REPOSITORY}:${IMAGE_TAG}"

if [[ "${DEPLOY_OBSERVABILITY}" == "true" ]]; then
  KUBE_CONTEXT="${KUBE_CONTEXT}" EXPECTED_KUBE_CONTEXT="${EXPECTED_KUBE_CONTEXT}" \
    "${ROOT_DIR}/scripts/mode-b-eks/deploy-observability.sh"
fi

"${KUBECTL[@]}" create namespace "${NAMESPACE}" --dry-run=client -o yaml | "${KUBECTL[@]}" apply -f -

if ! "${KUBECTL[@]}" -n "${NAMESPACE}" get secret "${BACKEND_SECRET_NAME}" >/dev/null 2>&1; then
  DB_PASSWORD="${DB_PASSWORD:-$(openssl rand -base64 24 | tr -d '\n')}"
  "${KUBECTL[@]}" -n "${NAMESPACE}" create secret generic "${BACKEND_SECRET_NAME}" \
    --from-literal=db-password="${DB_PASSWORD}" \
    --from-literal=openai-api-key="${OPENAI_API_KEY:-}"
fi

"${HELM[@]}" upgrade --install "${RELEASE_NAME}" "${ROOT_DIR}/deploy/helm/opspilot" \
  -n "${NAMESPACE}" \
  --create-namespace \
  -f "${VALUES_FILE}" \
  --set "backend.image.repository=${BACKEND_IMAGE_REPOSITORY}" \
  --set-string "backend.image.tag=${IMAGE_TAG}" \
  --set "frontend.image.repository=${FRONTEND_IMAGE_REPOSITORY}" \
  --set-string "frontend.image.tag=${IMAGE_TAG}"

"${KUBECTL[@]}" -n "${NAMESPACE}" rollout status deploy/opspilot-postgresql --timeout="${WAIT_TIMEOUT}"
"${KUBECTL[@]}" -n "${NAMESPACE}" rollout status deploy/"${HELM_FULLNAME}-backend" --timeout="${WAIT_TIMEOUT}"
"${KUBECTL[@]}" -n "${NAMESPACE}" rollout status deploy/"${HELM_FULLNAME}-frontend" --timeout="${WAIT_TIMEOUT}"

if [[ "${WAIT_DEMO_WORKLOADS}" == "true" ]]; then
  for deployment in redpanda order-producer order-consumer; do
    if "${KUBECTL[@]}" -n "${SAMPLE_NAMESPACE}" get deploy/"${deployment}" >/dev/null 2>&1; then
      "${KUBECTL[@]}" -n "${SAMPLE_NAMESPACE}" rollout status deploy/"${deployment}" --timeout="${WAIT_TIMEOUT}"
    fi
  done
fi

echo
"${KUBECTL[@]}" -n "${NAMESPACE}" get pods
"${KUBECTL[@]}" -n "${SAMPLE_NAMESPACE}" get pods

echo
echo "OpsPilot Mode B EKS deployment completed."
echo "Open the UI with:"
echo "  ./scripts/mode-b-eks/port-forward.sh"
echo "Run Helm test with:"
echo "  ./scripts/mode-b-eks/helm-test.sh"
