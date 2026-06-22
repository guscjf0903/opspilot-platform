# Helm 배포 계획

이 디렉토리는 OpsPilot과 sample-app을 Kubernetes에 재현 가능하게 배포하기 위한
Helm chart를 둘 위치입니다.

## Chart 원칙

- `values.yaml`은 안전하고 저비용인 기본값만 가집니다.
- `values-local-kind.yaml`은 로컬 kind 검증용입니다.
- `values-aws-dev-ephemeral.yaml`은 Mode B Lite k3s lab과 이후 AWS EKS 데모에서 재사용하는 AWS 개발용 값입니다.
- public ingress는 기본적으로 꺼둡니다.
- 외부 방문자용 demo에서는 action API를 비활성화하거나 Viewer 권한만 허용합니다.
- Secret 값은 values 파일에 직접 넣지 않고 Kubernetes Secret 또는 AWS Secrets Manager 참조로 주입합니다.

## 현재 chart

```text
opspilot/
  Chart.yaml
  values.yaml
  values-local-kind.yaml
  values-aws-dev-ephemeral.yaml
  templates/
```

현재 `opspilot` chart가 생성하는 리소스:

- backend Deployment, Service, ConfigMap
- frontend Deployment, Service
- optional demo PostgreSQL Deployment, Service
- optional sample-app demo Deployment, Service, PV, PVC
- optional Redpanda, Kafka producer, Kafka consumer demo Deployment, Service
- ServiceAccount
- optional read-only ClusterRole, ClusterRoleBinding
- optional Ingress

기본값:

- backend/frontend replica는 각각 1개
- public ingress는 비활성
- backend AI provider는 `stub`
- Secret 값은 생성하지 않고 기존 Kubernetes Secret을 참조
- `values-aws-dev-ephemeral.yaml`은 RDS 대신 demo PostgreSQL을 활성화
- `values-aws-dev-ephemeral.yaml`은 클러스터 표시 이름을 `Mode B Lite k3s`로 설정
- `values-aws-dev-ephemeral.yaml`은 Service 이름을 `opspilot-backend`, `opspilot-frontend`로 고정
- `values-aws-dev-ephemeral.yaml`은 실제 action execute를 비활성화하고 dry-run만 허용
- `values-aws-dev-ephemeral.yaml`은 Secret list를 제외한 Kubernetes read-only RBAC를 활성화
- `values-aws-dev-ephemeral.yaml`은 sample-app과 Redpanda Kafka demo를 활성화
- EKS, ALB, RDS, MSK, NAT Gateway는 생성하지 않음

`values-aws-dev-ephemeral.yaml`의 image repository는 Terraform으로 만든 ECR repository를
가리킵니다. tag는 GitHub Actions가 ECR에 이미지를 push한 commit SHA로 교체해서
사용합니다.

## 검증

```bash
helm lint deploy/helm/opspilot
helm template opspilot deploy/helm/opspilot -f deploy/helm/opspilot/values-local-kind.yaml
helm template opspilot deploy/helm/opspilot -f deploy/helm/opspilot/values-aws-dev-ephemeral.yaml
```

Ingress manifest까지 확인하려면 아래처럼 렌더링합니다.

```bash
helm template opspilot deploy/helm/opspilot -f deploy/helm/opspilot/values-aws-dev-ephemeral.yaml --set ingress.enabled=true
```

## 적용 예시

아래 명령은 클러스터가 준비된 뒤 사용하는 예시입니다. 현재 Terraform 단계에서는 아직 EKS를
생성하지 않았으므로 먼저 local kind 또는 EKS kubeconfig가 준비되어 있어야 합니다.

```bash
helm upgrade --install opspilot deploy/helm/opspilot \
  -n opspilot \
  --create-namespace \
  -f deploy/helm/opspilot/values-aws-dev-ephemeral.yaml \
  --set imagePullSecrets[0].name=ecr-registry \
  --set backend.image.tag=<commit-sha> \
  --set frontend.image.tag=<commit-sha>
```

Mode B Lite k3s lab에서는 위 명령 대신 루트의 `scripts/deploy-mode-b-lite-k3s.sh`를 사용하면
Secret 생성, ECR 인증, OpsPilot rollout, Redpanda/Kafka demo rollout 확인, EC2 내부 frontend
port-forward까지 함께 처리합니다.

노트북에서 화면을 볼 때는 SSM 터널을 엽니다.

```bash
AWS_PROFILE=opspilot-lab ./scripts/start-mode-b-lite-tunnel.sh
```

Secret topology는 Kubernetes Secret API를 직접 list하지 않고 Pod spec의 Secret 참조에서
이름만 추론합니다.
