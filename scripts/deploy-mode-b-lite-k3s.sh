#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

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
ECR_PULL_SECRET_NAME="${ECR_PULL_SECRET_NAME:-ecr-registry}"
WAIT_DEMO_WORKLOADS="${WAIT_DEMO_WORKLOADS:-true}"
START_FRONTEND_PORT_FORWARD="${START_FRONTEND_PORT_FORWARD:-true}"
FRONTEND_FORWARD_HOST="${FRONTEND_FORWARD_HOST:-127.0.0.1}"
FRONTEND_FORWARD_PORT="${FRONTEND_FORWARD_PORT:-8080}"
FRONTEND_SERVICE_PORT="${FRONTEND_SERVICE_PORT:-8080}"

kubectl create namespace "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

if ! kubectl -n "${NAMESPACE}" get secret "${BACKEND_SECRET_NAME}" >/dev/null 2>&1; then
  DB_PASSWORD="${DB_PASSWORD:-$(openssl rand -base64 24 | tr -d '\n')}"
  kubectl -n "${NAMESPACE}" create secret generic "${BACKEND_SECRET_NAME}" \
    --from-literal=db-password="${DB_PASSWORD}" \
    --from-literal=openai-api-key="${OPENAI_API_KEY:-}"
fi

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
ECR_REGISTRY="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
ECR_PASSWORD="$(aws ecr get-login-password --region "${AWS_REGION}")"

kubectl -n "${NAMESPACE}" create secret docker-registry "${ECR_PULL_SECRET_NAME}" \
  --docker-server="https://${ECR_REGISTRY}" \
  --docker-username=AWS \
  --docker-password="${ECR_PASSWORD}" \
  --dry-run=client \
  -o yaml | kubectl apply -f -

helm upgrade --install "${RELEASE_NAME}" "${ROOT_DIR}/deploy/helm/opspilot" \
  -n "${NAMESPACE}" \
  --create-namespace \
  -f "${ROOT_DIR}/deploy/helm/opspilot/values-aws-dev-ephemeral.yaml" \
  --set "backend.image.tag=${IMAGE_TAG}" \
  --set "frontend.image.tag=${IMAGE_TAG}" \
  --set "imagePullSecrets[0].name=${ECR_PULL_SECRET_NAME}" \
  --set "demoPostgresql.enabled=true"

kubectl -n "${NAMESPACE}" rollout status deploy/"${HELM_FULLNAME}-backend" --timeout=180s
kubectl -n "${NAMESPACE}" rollout status deploy/"${HELM_FULLNAME}-frontend" --timeout=180s

if [[ "${WAIT_DEMO_WORKLOADS}" == "true" ]]; then
  for deployment in redpanda order-producer order-consumer; do
    if kubectl -n "${SAMPLE_NAMESPACE}" get deploy/"${deployment}" >/dev/null 2>&1; then
      kubectl -n "${SAMPLE_NAMESPACE}" rollout status deploy/"${deployment}" --timeout=240s
    fi
  done
fi

echo "OpsPilot Mode B Lite deployment completed."

if [[ "${START_FRONTEND_PORT_FORWARD}" == "true" ]]; then
  PORT_FORWARD_LOG="/tmp/opspilot-${NAMESPACE}-frontend-port-forward.log"
  pkill -f "kubectl.*port-forward.*svc/${HELM_FULLNAME}-frontend" >/dev/null 2>&1 || true
  nohup kubectl -n "${NAMESPACE}" port-forward \
    --address "${FRONTEND_FORWARD_HOST}" \
    "svc/${HELM_FULLNAME}-frontend" \
    "${FRONTEND_FORWARD_PORT}:${FRONTEND_SERVICE_PORT}" \
    >"${PORT_FORWARD_LOG}" 2>&1 &

  sleep 2
  echo "Frontend port-forward on this k3s host:"
  echo "${FRONTEND_FORWARD_HOST}:${FRONTEND_FORWARD_PORT} -> svc/${HELM_FULLNAME}-frontend:${FRONTEND_SERVICE_PORT}"
  echo "Log: ${PORT_FORWARD_LOG}"
fi

echo "From your laptop, open an SSM tunnel with:"
echo "AWS_PROFILE=opspilot-lab ./scripts/start-mode-b-lite-tunnel.sh"
