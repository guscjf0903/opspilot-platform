# AWS 개발용 임시 환경

이 환경은 OpsPilot을 AWS에서 짧게 시연하기 위한 `aws-dev-ephemeral` 구성입니다.
현재 구현된 live Kubernetes 선택지는 Mode B Lite인 단일 EC2 + k3s입니다. EKS 기반
Mode B 본안은 `enable_eks=false`로 남겨둔 다음 단계입니다.

현재 범위:

- 백엔드와 프론트엔드 이미지를 저장할 private ECR 저장소
- GitHub Actions가 ECR에 이미지를 push하기 위한 OIDC IAM Role
- 2개 public subnet을 가진 작은 VPC
- 선택형 단일 EC2 + k3s Mode B Lite lab
- NAT Gateway 없음
- EKS, ALB, RDS, MSK는 아직 생성하지 않음

목표는 비용이 큰 리소스를 추가하기 전에, 저비용 기반 블록을 먼저 준비하는 것입니다.

## 안전 기본값

```text
enable_ecr         = true
enable_vpc         = true
enable_eks         = false
enable_alb         = false
enable_rds         = false
enable_msk         = false
enable_nat_gateway = false
enable_k3s_lab     = false
enable_github_actions_ecr_push_role = true
default_ttl_hours  = 4
```

`enable_github_actions_ecr_push_role`은 기본 `true`입니다. 이 Role은 비용이 발생하지
않고, `guscjf0903/opspilot-platform` 저장소의 `main` branch workflow가 지정된 ECR
repository에 이미지를 push하는 권한만 갖습니다.

AWS 계정에 GitHub Actions OIDC Provider가 이미 있다면 같은 URL Provider를 중복 생성할 수
없으므로 아래처럼 기존 ARN을 사용합니다.

```hcl
create_github_oidc_provider       = false
existing_github_oidc_provider_arn = "arn:aws:iam::<account-id>:oidc-provider/token.actions.githubusercontent.com"
```

AWS 계정, 월 예산, 예상 종료 시간을 확인하기 전에는 `terraform apply`를 실행하지 않습니다.

## Mode B Lite k3s lab 켜기

단일 EC2 + k3s live demo가 필요할 때만 `terraform.tfvars`에서 아래 값을 명시합니다.

```hcl
enable_k3s_lab = true

k3s_lab_instance_type       = "t3.small"
k3s_lab_root_volume_size_gb = 20

# 기본값은 inbound를 열지 않습니다. 접속은 SSM Session Manager를 사용합니다.
k3s_lab_allowed_ssh_cidr_blocks     = []
k3s_lab_allowed_k3s_api_cidr_blocks = []
k3s_lab_allowed_http_cidr_blocks    = []
```

생성 후 output을 확인합니다.

```bash
terraform -chdir=infra/terraform/envs/aws-dev-ephemeral output k3s_lab
```

SSM으로 접속한 뒤 repository를 clone하고 ECR에 올라간 commit SHA로 배포합니다.

```bash
IMAGE_TAG=<commit-sha> ./scripts/deploy-mode-b-lite-k3s.sh
```

배포 스크립트는 EC2 내부 `127.0.0.1:8080`에 frontend port-forward를 백그라운드로
시작합니다. 노트북에서는 repository 루트에서 SSM 터널을 엽니다.

```bash
AWS_PROFILE=opspilot-lab ./scripts/start-mode-b-lite-tunnel.sh
```

터널이 열려 있는 동안 아래 주소로 live 화면을 확인합니다.

```text
http://127.0.0.1:8080
```

Helm 배포는 Secret list 권한을 제외한 read-only RBAC를 생성합니다. 운영 action API가
write 권한을 갖지 않도록 Mode B Lite 기본값에서는 restart, scale, rollback 같은 조치를 실제
클러스터에 실행하지 않습니다.

Mode B Lite Helm 배포에는 아래 demo workload도 포함됩니다.

- `sample-app/payment-api`: readiness 실패 시나리오
- `sample-app/worker`: CrashLoopBackOff 시나리오
- `sample-app/catalog-api`: 과한 CPU request 시나리오
- `sample-app/redpanda`: Kafka Admin API 조회 대상
- `sample-app/order-producer`: `orders.created` topic 생산자
- `sample-app/order-consumer`: 느린 consumer로 lag 생성

AI 분석은 기본 `stub` provider로 실행되며, workload topology에 연결된 Kafka consumer group이
있으면 consumer lag를 evidence에 포함합니다.

검증이 끝나면 바로 제거합니다.

```bash
terraform -chdir=infra/terraform/envs/aws-dev-ephemeral destroy
```

## 검증

```bash
terraform -chdir=infra/terraform/envs/aws-dev-ephemeral init -backend=false
terraform -chdir=infra/terraform/envs/aws-dev-ephemeral validate
```

## GitHub Actions 연결

Terraform apply 후 아래 output을 확인합니다.

```bash
terraform -chdir=infra/terraform/envs/aws-dev-ephemeral output github_actions_ecr_push_role_arn
```

출력된 Role ARN을 GitHub repository secret `AWS_GITHUB_ACTIONS_ROLE_ARN`에 저장합니다.
그 다음 `Container images` workflow를 수동 실행하고 `push_to_ecr=true`를 선택하면
backend/frontend 이미지가 ECR에 commit SHA tag로 push됩니다.
