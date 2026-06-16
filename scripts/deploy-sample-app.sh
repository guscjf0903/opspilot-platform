#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONTEXT="kind-opspilot-demo"

kubectl --context "${CONTEXT}" apply -k "${ROOT_DIR}/sample-app/manifests/base"
kubectl --context "${CONTEXT}" rollout status deployment/payment-api -n sample-app
kubectl --context "${CONTEXT}" rollout status deployment/catalog-api -n sample-app
