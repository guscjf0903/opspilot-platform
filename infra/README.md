# Infrastructure

OpsPilot 인프라는 개인 포트폴리오용 저비용 구성을 우선합니다. 실제 AWS 리소스는
항상 켜두지 않고, 데모 목적이 있을 때 짧게 생성한 뒤 제거합니다.

공개 저장소에서는 비용 안전장치와 재현 가능한 구조를 보여주는 데 집중합니다. 상세한
개인 기획 문서는 저장소에 포함하지 않습니다.

## 기본 원칙

- EKS, ALB, RDS, MSK, NAT Gateway는 기본 비활성으로 설계한다.
- Terraform `apply`와 `destroy`는 명시적으로 확인한 뒤 실행합니다.
- AWS Budgets와 billing alert를 먼저 설정합니다.
- Terraform state, plan, credential, secret은 commit하지 않습니다.
- 모든 리소스에는 `Project=OpsPilot`, `ManagedBy=Terraform`, `CostMode=portfolio-low-cost` tag를 붙입니다.

## 예정 구조

```text
infra/
  terraform/
    envs/
      bootstrap-cost-guard/
      aws-dev-ephemeral/
      aws-msk-proof/
    modules/
      budget/
      ecr/
      vpc-lite/
      eks-ephemeral/
      rds-free-tier/
      iam-pod-identity/
      alb-optional/
      msk-optional/
```

현재 구현된 환경:

| 경로 | 역할 |
| --- | --- |
| `terraform/envs/bootstrap-cost-guard` | AWS Budgets monthly cost guard |
| `terraform/envs/aws-dev-ephemeral` | ECR과 NAT 없는 VPC-lite 기반의 짧은 AWS 데모 환경 |
| `terraform/modules/budget` | budget 생성 module |
| `terraform/modules/ecr` | backend/frontend image용 private ECR repositories |
| `terraform/modules/vpc-lite` | NAT Gateway 없는 2 AZ public subnet VPC |

`aws-dev-ephemeral`은 실제 EKS 데모를 위한 짧은 실행 환경입니다. 현재는 비용이 커질 수
있는 EKS, ALB, RDS, MSK, NAT Gateway를 만들지 않고 ECR과 VPC foundation만 정의합니다.
`aws-msk-proof`는 Amazon MSK 연동 증명이 꼭 필요할 때만 별도로 사용합니다.

## 검증 명령

```bash
terraform -chdir=infra/terraform/envs/bootstrap-cost-guard init -backend=false
terraform -chdir=infra/terraform/envs/aws-dev-ephemeral init -backend=false
terraform fmt -recursive
terraform -chdir=infra/terraform/envs/bootstrap-cost-guard validate
terraform -chdir=infra/terraform/envs/aws-dev-ephemeral validate
tflint
terraform plan
```

실제 Terraform 리소스 확장은 비용 상한, 공개 방식, domain 사용 여부를 확정한 뒤 진행합니다.
