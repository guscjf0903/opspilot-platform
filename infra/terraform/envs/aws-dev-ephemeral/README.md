# AWS 개발용 임시 환경

이 환경은 OpsPilot을 AWS에서 짧게 시연하기 위한 Mode B 기반 구성입니다.

현재 범위:

- 백엔드와 프론트엔드 이미지를 저장할 private ECR 저장소
- GitHub Actions가 ECR에 이미지를 push하기 위한 OIDC IAM Role
- 2개 public subnet을 가진 작은 VPC
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
