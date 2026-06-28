# AWS 개발용 임시 환경

이 환경은 OpsPilot을 AWS에서 짧게 시연하기 위한 `aws-dev-ephemeral` 구성입니다.
현재 구현된 live Kubernetes 선택지는 Mode B Lite인 단일 EC2 + k3s와 Mode B의 EKS 최소
클러스터입니다. EKS 앱 배포와 관측 구성은 다음 단계입니다.

`aws-dev-ephemeral`은 이제 ECR repository와 GitHub Actions Role을 만들지 않습니다.
그 둘은 `../aws-dev-foundation`에서 관리하고, 이 환경은 사용할 때만 VPC, EC2+k3s,
앞으로의 EKS/ALB/RDS 같은 실습 리소스를 생성합니다.

현재 범위:

- 2개 public subnet을 가진 작은 VPC
- 선택형 단일 EC2 + k3s Mode B Lite lab
- 선택형 EKS cluster + managed node group Mode B lab
- NAT Gateway 없음
- ALB, RDS, MSK는 아직 생성하지 않음

목표는 ECR과 GitHub Actions Role은 유지하면서, 비용이 나는 실습 리소스만 켰다 끄기
쉽게 만드는 것입니다.

## 지금 어떤 단계인지 확인

Mode B 계열 Terraform은 두 환경으로 나누어 생각합니다.

| 환경 | 만드는 것 | destroy 정책 |
| --- | --- | --- |
| `aws-dev-foundation` | ECR, GitHub Actions ECR push Role | 평소 유지 |
| `aws-dev-ephemeral` | VPC, EC2+k3s, EKS, 향후 ALB/RDS | 실습 후 destroy |

이미 GitHub Actions가 ECR에 backend/frontend 이미지를 push했다면 foundation 단계는 끝난
상태입니다. 이 경우 foundation을 다시 만들 필요 없이, 이 환경의 `terraform.tfvars`에
`foundation_ecr_repository_arns`, `foundation_ecr_repository_urls`, `enable_k3s_lab=true`만
맞춰 k3s lab을 켭니다.

`terraform.tfvars`는 Git에 올라가지 않는 로컬 override 파일입니다. 예전에 만든
`terraform.tfvars`에는 k3s 관련 값이 없을 수 있으므로, 기존 파일에 아래 Mode B Lite 블록을
직접 추가합니다.

## 안전 기본값

```text
enable_vpc         = true
enable_eks         = false
enable_alb         = false
enable_rds         = false
enable_msk         = false
enable_nat_gateway = false
default_ttl_hours  = 4
enable_k3s_lab     = false
enable_eks         = false
```

`enable_ecr`, `enable_github_actions_ecr_push_role`, `github_actions_*` 변수는 예전
로컬 `terraform.tfvars` 호환을 위해 남아 있지만 이 환경에서는 더 이상 사용하지 않습니다.
새 설정은 `aws-dev-foundation` output을 기준으로 합니다.

foundation output 확인:

```bash
terraform -chdir=infra/terraform/envs/aws-dev-foundation output ecr_repository_arns
terraform -chdir=infra/terraform/envs/aws-dev-foundation output ecr_repository_urls
```

AWS 계정, 월 예산, 예상 종료 시간을 확인하기 전에는 `terraform apply`를 실행하지 않습니다.

## Mode B EKS 최소 클러스터 켜기

EKS는 클러스터 자체와 worker node가 모두 과금되므로 필요할 때만 켭니다. `terraform.tfvars`에
아래 값을 둡니다.

```hcl
enable_vpc     = true
enable_eks     = true
enable_k3s_lab = false

eks_cluster_name                 = null
eks_kubernetes_version           = null
eks_endpoint_public_access       = true
eks_endpoint_private_access      = false
eks_endpoint_public_access_cidrs = ["0.0.0.0/0"]

eks_node_group_instance_types = ["t3.medium"]
eks_node_group_ami_type       = "AL2_x86_64"
eks_node_group_capacity_type  = "ON_DEMAND"
eks_node_group_desired_size   = 1
eks_node_group_min_size       = 0
eks_node_group_max_size       = 1
eks_node_group_disk_size_gb   = 20
```

현재 IP만 허용하고 싶다면 `eks_endpoint_public_access_cidrs`를 내 IP `/32`로 좁힙니다.
1차 저비용 구성은 NAT Gateway를 만들지 않으므로 EKS worker node는 public subnet에 배치되고,
EKS가 켜질 때 public subnet의 instance public IPv4 자동 할당도 켜집니다.

계획 확인:

```bash
AWS_PROFILE=opspilot-lab terraform -chdir=infra/terraform/envs/aws-dev-ephemeral plan
```

정상이라면 plan에 `module.eks_ephemeral`, `aws_eks_cluster`, `aws_eks_node_group`,
EKS cluster/node IAM Role 같은 리소스 생성이 보여야 합니다.

적용 후 kubeconfig 연결:

```bash
AWS_PROFILE=opspilot-lab terraform -chdir=infra/terraform/envs/aws-dev-ephemeral output eks
AWS_PROFILE=opspilot-lab aws eks update-kubeconfig --region ap-northeast-2 --name opspilot-aws-dev-eks
kubectl get nodes
```

현재 Phase 1은 EKS와 node Ready 확인까지입니다. OpsPilot Helm 배포는 다음 Phase에서
`values-aws-dev-eks.yaml`과 배포 스크립트를 추가해 진행합니다.

## Mode B Lite k3s lab 켜기

단일 EC2 + k3s live demo가 필요할 때만 `terraform.tfvars`에서 아래 값을 명시합니다.

```hcl
enable_k3s_lab = true

foundation_ecr_repository_arns = {
  opspilot-backend  = "arn:aws:ecr:ap-northeast-2:<account-id>:repository/opspilot-backend"
  opspilot-frontend = "arn:aws:ecr:ap-northeast-2:<account-id>:repository/opspilot-frontend"
}

foundation_ecr_repository_urls = {
  opspilot-backend  = "<account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/opspilot-backend"
  opspilot-frontend = "<account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/opspilot-frontend"
}

k3s_lab_instance_type       = "t3.small"
k3s_lab_root_volume_size_gb = 20
k3s_lab_key_name            = null

# 기본값은 inbound를 열지 않습니다. 접속은 SSM Session Manager를 사용합니다.
k3s_lab_allowed_ssh_cidr_blocks     = []
k3s_lab_allowed_k3s_api_cidr_blocks = []
k3s_lab_allowed_http_cidr_blocks    = []

k3s_lab_k3s_channel = "stable"
```

기존 `terraform.tfvars`에 `enable_k3s_lab=false`가 있으면 반드시 `true`로 바꿉니다.
`false`로 남아 있으면 `terraform apply`를 해도 EC2와 k3s가 생성되지 않습니다.

계획 확인:

```bash
AWS_PROFILE=opspilot-lab terraform -chdir=infra/terraform/envs/aws-dev-ephemeral plan
```

정상이라면 plan에 `module.k3s_lab`, `aws_instance`, `AmazonSSMManagedInstanceCore`,
`k3s-lab` security group 같은 리소스 생성이 보여야 합니다. plan에 ECR/VPC만 보이거나
변경 없음으로 나오면 `enable_k3s_lab=true`가 적용되지 않은 상태입니다.
`foundation_ecr_repository_arns`를 비워 둔 채 `enable_k3s_lab=true`로 plan하면 ECR pull
권한이 빠지는 것을 막기 위해 Terraform이 오류를 냅니다.

적용:

```bash
AWS_PROFILE=opspilot-lab terraform -chdir=infra/terraform/envs/aws-dev-ephemeral apply
```

생성 후 output을 확인합니다.

```bash
AWS_PROFILE=opspilot-lab terraform -chdir=infra/terraform/envs/aws-dev-ephemeral output k3s_lab
```

로컬에서 SSM Run Command로 EC2에 원격 배포합니다.

```bash
AWS_PROFILE=opspilot-lab IMAGE_TAG=<commit-sha> ./scripts/mode-b-lite/deploy-remote.sh
```

이 스크립트는 EC2에 직접 접속하지 않고 k3s 준비 확인, repository checkout, Helm 배포,
rollout 확인, frontend port-forward 시작을 수행합니다.

EC2에 직접 접속해서 수동 배포해야 한다면 SSM으로 접속한 뒤 repository를 clone하고 ECR에
올라간 commit SHA로 배포합니다.

```bash
IMAGE_TAG=<commit-sha> ./scripts/mode-b-lite/deploy-k3s.sh
```

배포 스크립트는 현재 AWS 계정 ID를 조회해 아래 image repository를 Helm에 자동 주입합니다.

```text
<account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/opspilot-backend
<account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/opspilot-frontend
```

ECR repository 이름을 바꿨다면 `BACKEND_IMAGE_REPOSITORY`,
`FRONTEND_IMAGE_REPOSITORY` 환경 변수로 명시합니다.

배포 스크립트는 EC2 내부 `127.0.0.1:8080`에 frontend port-forward를 백그라운드로
시작합니다. 노트북에서는 repository 루트에서 SSM 터널을 엽니다.

```bash
AWS_PROFILE=opspilot-lab ./scripts/mode-b-lite/start-tunnel.sh
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

GitHub Actions ECR push Role은 `aws-dev-foundation`에서 관리합니다. foundation apply 후
아래 output을 확인합니다.

```bash
terraform -chdir=infra/terraform/envs/aws-dev-foundation output github_actions_ecr_push_role_arn
```

출력된 Role ARN을 GitHub repository secret `AWS_GITHUB_ACTIONS_ROLE_ARN`에 저장합니다.
그 다음 `Container images` workflow를 수동 실행하고 `push_to_ecr=true`를 선택하면
backend/frontend 이미지가 ECR에 commit SHA tag로 push됩니다.

이미 이 단계가 끝났고 ECR에 이미지가 올라갔다면, 다시 Role을 만들 필요 없이 foundation
output의 ECR ARN/URL만 이 환경에 넘기고
`enable_k3s_lab=true`로 바꾼 뒤 Mode B Lite k3s lab 단계로 진행합니다.

## 자주 막히는 지점

### k3s 관련 설정이 terraform.tfvars에 없음

`terraform.tfvars`는 로컬 파일이라 `terraform.tfvars.example`이 업데이트되어도 자동으로
바뀌지 않습니다. 기존 파일에 `enable_k3s_lab=true`와 `k3s_lab_*` 값을 직접 추가합니다.

### plan에 EC2가 나오지 않음

아래 두 값이 모두 맞는지 확인합니다.

```hcl
enable_vpc     = true
enable_k3s_lab = true
```

### GitHub OIDC Provider가 이미 존재한다는 오류

AWS 계정에는 `token.actions.githubusercontent.com` OIDC Provider를 하나만 만들 수 있습니다.
이미 있다면 `aws-dev-foundation`의 `terraform.tfvars`에서
`create_github_oidc_provider=false`와 기존 Provider ARN을 설정합니다.

### terraform apply는 성공했는데 앱 화면이 안 뜸

Terraform은 EC2+k3s까지만 만듭니다. 앱은 EC2에 SSM으로 접속한 뒤 별도로 배포해야 합니다.

```bash
IMAGE_TAG=<commit-sha> ./scripts/mode-b-lite/deploy-k3s.sh
```

그 다음 노트북에서 SSM tunnel을 열어야 합니다.

```bash
AWS_PROFILE=opspilot-lab ./scripts/mode-b-lite/start-tunnel.sh
```
