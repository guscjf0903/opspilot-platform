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
      values-aws-dev-eks.yaml
      templates/
```

`opspilot` chart는 backend와 frontend를 작은 replica/resource 기본값으로 배포합니다.
public ingress는 기본 비활성이고, DB password와 AI provider key 같은 Secret 값은 chart
values에 직접 넣지 않습니다.

이 chart는 EKS 클러스터 자체를 만들지 않습니다. Terraform의 `aws-dev-ephemeral`에서 만든
Kubernetes cluster 위에 배포되는 workload만 정의합니다. `values-aws-dev-ephemeral.yaml`은
Mode B Lite 단일 EC2+k3s 데모용이고, `values-aws-dev-eks.yaml`은 Mode B EKS lab용입니다.

Mode B Lite에서는 RDS 비용을 만들지 않기 위해 `demoPostgresql.enabled=true`를 사용합니다.
이 PostgreSQL은 클러스터 안의 임시 pod이며 production 용도가 아닙니다.
`kubernetesReadOnlyRbac.enabled=true`는 backend가 클러스터 상태를 읽기 위한 최소 조회
권한만 만들고, Kubernetes Secret list 권한은 부여하지 않습니다. Secret 관계도는 Pod의
참조 정보에서 이름만 추론합니다.

Mode B Lite에서는 `demoSampleApp.enabled=true`, `demoKafka.enabled=true`도 함께 사용합니다.
이 값은 `sample-app` namespace에 readiness 실패 workload, CrashLoop workload, 과한 CPU request
workload, Redpanda broker, Kafka producer/consumer를 배포합니다. producer는 `orders.created`
topic에 메시지를 계속 만들고 consumer는 천천히 읽어 consumer lag를 생성합니다.

Mode B EKS 기본값은 저비용 단일 node 안정성을 우선하므로 `demoKafka.enabled=false`로 Kafka
demo를 잠시 끕니다. Kafka/Redpanda는 이후 단계에서 다시 활성화합니다.

Mode B EKS도 같은 chart를 사용하지만 cluster 표시 이름과 provider 값을 EKS로 분리합니다.
EKS managed node role에 ECR read 권한이 있으므로 기본 경로에서는 별도 ECR image pull Secret을
만들지 않습니다.

Prometheus는 `local/prometheus`의 작은 manifest 중 Prometheus와 kube-state-metrics를 EKS에
적용합니다. Mode B EKS 기본값에서는 `t3.medium` 단일 node의 pod 한도를 고려해 node-exporter를
생략합니다. OpenCost는 `deploy/helm/opencost/values-aws-dev-eks.yaml` 값을 사용해 upstream Helm
chart로 설치하고, 내부 Prometheus service(`prometheus.monitoring.svc.cluster.local:9090`)를
바라봅니다.

Mode B EKS values는 단일 node의 pod/IP 한도를 넘지 않도록 Kafka/Redpanda demo를 기본값에서
끄고, ALB와 다중 node 구성을 붙이는 단계에서 Kafka demo를 다시 검증합니다.

## 검증 명령

```bash
helm lint deploy/helm/opspilot
kubectl kustomize local/prometheus
helm template opspilot deploy/helm/opspilot -f deploy/helm/opspilot/values-local-kind.yaml
helm template opspilot deploy/helm/opspilot -f deploy/helm/opspilot/values-aws-dev-ephemeral.yaml
helm template opspilot deploy/helm/opspilot -f deploy/helm/opspilot/values-aws-dev-eks.yaml
kubeconform
```

## Mode B Lite 배포 예시

ECR에 backend/frontend 이미지가 같은 commit SHA tag로 올라간 뒤, k3s lab 안에서 아래
스크립트를 실행합니다.

```bash
IMAGE_TAG=<commit-sha> ./scripts/mode-b-lite/deploy-k3s.sh
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
AWS_PROFILE=opspilot-lab ./scripts/mode-b-lite/start-tunnel.sh
```

터널이 열려 있는 동안 `http://127.0.0.1:8080`에서 Mode B Lite live 화면을 볼 수 있습니다.
frontend nginx는 `/api`와 `/actuator`를 backend Service로 프록시하므로 backend를 public으로
노출하지 않습니다.

## Mode B EKS 배포 예시

EKS cluster와 node group을 만든 뒤 kubeconfig를 연결합니다.

```bash
aws eks update-kubeconfig \
  --profile opspilot-lab \
  --region ap-northeast-2 \
  --name opspilot-aws-dev-eks \
  --alias opspilot-aws-dev-eks
```

ECR에 backend/frontend 이미지가 같은 commit SHA tag로 올라간 뒤 로컬에서 배포합니다.

```bash
AWS_PROFILE=opspilot-lab ./scripts/mode-b-eks/deploy-observability.sh
AWS_PROFILE=opspilot-lab IMAGE_TAG=<commit-sha> ./scripts/mode-b-eks/deploy.sh
./scripts/mode-b-eks/verify-observability.sh
./scripts/mode-b-eks/verify.sh
./scripts/mode-b-eks/helm-test.sh
./scripts/mode-b-eks/port-forward.sh
```

터널이 열려 있는 동안 `http://127.0.0.1:8080`에서 Mode B EKS live 화면을 볼 수 있습니다.
이 단계는 ALB를 만들지 않습니다.
