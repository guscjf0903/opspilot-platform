# 배포

OpsPilot 배포는 Helm을 기준으로 정리합니다. 로컬 데모 앱은 기존처럼
`sample-app/manifests`의 Kustomize 리소스를 사용할 수 있고, AWS 데모 단계에서는
Helm chart로 OpsPilot과 sample-app을 재현 가능하게 배포합니다.

공개 저장소에서는 기본값을 저비용, 읽기 전용, public ingress 비활성으로 유지합니다.

## 원칙

- public ingress는 기본 비활성입니다.
- 외부 방문자용 live demo는 읽기 전용으로 제한합니다.
- Secret 값은 Helm values에 직접 넣지 않습니다.
- resource request와 limit은 포트폴리오 데모 규모에 맞게 작게 시작합니다.
- Prometheus, OpenCost, sample-app은 데모 목적에 맞게 작은 retention과 replica를 사용합니다.

## 현재 구조

```text
deploy/
  helm/
    opspilot/
      Chart.yaml
      values.yaml
      values-local-kind.yaml
      values-aws-dev-ephemeral.yaml
      templates/
```

`opspilot` chart는 backend와 frontend를 작은 replica/resource 기본값으로 배포합니다.
public ingress는 기본 비활성이고, DB password와 AI provider key 같은 Secret 값은 chart
values에 직접 넣지 않습니다.

아직 이 chart는 EKS 클러스터를 만들지 않습니다. Terraform의 `aws-dev-ephemeral`에서
생성한 ECR 이미지 저장소를 참조할 수 있는 배포 템플릿과, Mode B Lite 단일 EC2+k3s 데모용
임시 PostgreSQL 옵션을 제공합니다.

Mode B Lite에서는 RDS 비용을 만들지 않기 위해 `demoPostgresql.enabled=true`를 사용합니다.
이 PostgreSQL은 클러스터 안의 임시 pod이며 production 용도가 아닙니다.
`kubernetesReadOnlyRbac.enabled=true`는 backend가 클러스터 상태를 읽기 위한 최소 조회
권한만 만들고, Kubernetes Secret list 권한은 부여하지 않습니다. Secret 관계도는 Pod의
참조 정보에서 이름만 추론합니다.

Mode B Lite에서는 `demoSampleApp.enabled=true`, `demoKafka.enabled=true`도 함께 사용합니다.
이 값은 `sample-app` namespace에 readiness 실패 workload, CrashLoop workload, 과한 CPU request
workload, Redpanda broker, Kafka producer/consumer를 배포합니다. producer는 `orders.created`
topic에 메시지를 계속 만들고 consumer는 천천히 읽어 consumer lag를 생성합니다.

## 검증 명령

```bash
helm lint deploy/helm/opspilot
helm template opspilot deploy/helm/opspilot -f deploy/helm/opspilot/values-local-kind.yaml
helm template opspilot deploy/helm/opspilot -f deploy/helm/opspilot/values-aws-dev-ephemeral.yaml
kubeconform
```

## Mode B Lite 배포 예시

ECR에 backend/frontend 이미지가 같은 commit SHA tag로 올라간 뒤, k3s lab 안에서 아래
스크립트를 실행합니다.

```bash
IMAGE_TAG=<commit-sha> ./scripts/deploy-mode-b-lite-k3s.sh
```

이 스크립트는 다음을 수행합니다.

- demo PostgreSQL password Secret 생성
- ECR image pull Secret 생성
- Helm으로 backend, frontend, demo PostgreSQL, sample-app, Redpanda 배포
- Secret list 권한 없는 Kubernetes read-only RBAC 생성
- backend의 실제 action execute 차단
- backend/frontend rollout 확인
- Redpanda, order-producer, order-consumer rollout 확인
- EC2 내부 `127.0.0.1:8080`에 frontend port-forward 시작

그 다음 노트북의 repository 루트에서 SSM 터널을 엽니다.

```bash
AWS_PROFILE=opspilot-lab ./scripts/start-mode-b-lite-tunnel.sh
```

터널이 열려 있는 동안 `http://127.0.0.1:8080`에서 Mode B Lite live 화면을 볼 수 있습니다.
frontend nginx는 `/api`와 `/actuator`를 backend Service로 프록시하므로 backend를 public으로
노출하지 않습니다.
