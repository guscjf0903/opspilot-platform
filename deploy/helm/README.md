# Helm Deployment Plan

이 디렉토리는 OpsPilot과 sample-app을 Kubernetes에 재현 가능하게 배포하기 위한
Helm chart를 둘 위치다.

## Chart 원칙

- `values.yaml`은 안전하고 저비용인 기본값만 가진다.
- `values-local-kind.yaml`은 로컬 kind 검증용이다.
- `values-aws-dev-ephemeral.yaml`은 짧게 켜는 AWS EKS 데모용이다.
- public ingress는 기본적으로 꺼둔다.
- 외부 방문자용 demo에서는 action API를 비활성화하거나 Viewer 권한만 허용한다.
- Secret 값은 values 파일에 직접 넣지 않고 Kubernetes Secret 또는 AWS Secrets Manager 참조로 주입한다.

## 예정 chart

```text
opspilot/
  backend Deployment
  frontend Deployment
  Service
  optional Ingress
  ServiceAccount
  ConfigMap

sample-app/
  frontend
  payment-api
  catalog-api
  worker
  demo Kafka or Redpanda resources
```

## 검증

```bash
helm lint deploy/helm/opspilot
helm template opspilot deploy/helm/opspilot -f deploy/helm/opspilot/values-aws-dev-ephemeral.yaml
```
