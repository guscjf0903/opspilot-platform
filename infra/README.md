# 인프라

OpsPilot 인프라는 개인 포트폴리오용 저비용 구성을 우선합니다. 실제 AWS 리소스는
항상 켜두지 않고, 데모 목적이 있을 때 짧게 생성한 뒤 제거합니다.

공개 저장소에서는 비용 안전장치와 재현 가능한 구조를 보여주는 데 집중합니다. 상세한
개인 기획 문서는 저장소에 포함하지 않습니다.

## 기본 원칙

- EKS, ALB, RDS, MSK, NAT Gateway는 기본 비활성으로 설계합니다.
- Mode B Lite live demo는 EKS보다 먼저 단일 EC2 + k3s로 검증합니다.
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
      aws-dev-foundation/
      aws-dev-ephemeral/
      aws-msk-proof/
    modules/
      budget/
      ecr/
      vpc-lite/
      ec2-k3s-lab/
      eks-ephemeral/
      rds-free-tier/
      iam-pod-identity/
      alb-optional/
      msk-optional/
```

현재 구현된 환경:

| 경로 | 역할 |
| --- | --- |
| `terraform/envs/bootstrap-cost-guard` | AWS Budgets 월 비용 가드 |
| `terraform/envs/aws-dev-foundation` | ECR repository와 GitHub Actions ECR push Role |
| `terraform/envs/aws-dev-ephemeral` | NAT 없는 VPC-lite, 선택형 k3s lab, 향후 EKS용 짧은 AWS 데모 환경 |
| `terraform/modules/budget` | 예산 알림 생성 모듈 |
| `terraform/modules/ecr` | 백엔드와 프론트엔드 image용 private ECR repository |
| `terraform/modules/github-actions-ecr-push-role` | GitHub OIDC 기반 ECR push 전용 IAM Role |
| `terraform/modules/vpc-lite` | NAT Gateway 없는 2 AZ public subnet VPC |
| `terraform/modules/ec2-k3s-lab` | Mode B Lite용 단일 EC2 + k3s live demo 환경 |
| `terraform/modules/eks-ephemeral` | Mode B용 EKS cluster와 managed node group |

`aws-dev-foundation`은 평소 유지해도 되는 ECR과 GitHub Actions ECR push Role만 관리합니다.
GitHub Actions용 IAM Role은 AWS access key 없이 ECR push만 허용하며, Terraform apply나
EKS 생성 권한은 부여하지 않습니다.

`aws-dev-ephemeral`은 실제 EKS 데모를 위한 짧은 실행 환경입니다. 현재는 VPC foundation,
선택형 k3s lab, 선택형 EKS cluster와 managed node group을 정의합니다. ALB, RDS, MSK,
NAT Gateway는 아직 만들지 않습니다. k3s lab이 ECR image를 pull하려면
`aws-dev-foundation` output의 `foundation_ecr_repository_arns`를 입력으로 넘깁니다.
`aws-msk-proof`는 Amazon MSK 연동 증명이 꼭 필요할 때만 별도로 사용합니다.

Mode B EKS 구현 전에는 전략 B로 Terraform 경계를 나눕니다.

| 목표 경로 | 역할 | 비용 정책 |
| --- | --- | --- |
| `terraform/envs/aws-dev-foundation` | ECR repository, GitHub Actions ECR push Role | IAM은 비용 없음, ECR 이미지는 저장 비용 가능 |
| `terraform/envs/aws-dev-ephemeral` | VPC, EKS, managed node group, optional ALB/RDS | 사용할 때만 apply, 끝나면 destroy |

이 분리는 EKS 실습 환경을 삭제할 때 GitHub Actions Role과 ECR repository까지 함께 삭제되는
것을 막기 위한 경계입니다.

## Mode B Lite 비용 발생 지점

기본값 `enable_k3s_lab=false`에서는 EC2가 생성되지 않습니다. `enable_k3s_lab=true`로
켜는 순간 아래 비용이 발생할 수 있습니다.

| 리소스 | 기본 정책 | 비용 주의 |
| --- | --- | --- |
| EC2 k3s instance | 필요할 때만 생성 | instance-hour 과금 |
| EBS root volume | 20GiB gp3 | GB-month 과금 |
| Public IPv4 | EC2 실행 중 사용 | IPv4 address hour 과금 |
| ECR | 이미지 보관 | lifecycle로 최근 이미지만 유지 |
| Redpanda demo | EC2 안의 k3s pod | 별도 AWS 서비스 비용 없음, EC2 리소스만 사용 |
| NAT Gateway | 비활성 | 켜지 않음 |
| ALB | 비활성 | public live demo 때만 별도 검토 |
| EKS/RDS/MSK | 비활성 | 단일 EC2+k3s 검증 이후에만 검토 |

Mode B Lite를 켤 때는 `default_ttl_hours` 안에 검증하고 `terraform destroy`로 제거합니다.

Mode B Lite 접속 흐름:

```text
Terraform apply
  -> EC2 + k3s 생성
  -> SSM으로 EC2 접속
  -> scripts/mode-b-lite/deploy-k3s.sh 실행
  -> EC2 내부 127.0.0.1:8080에 frontend port-forward
  -> 노트북에서 scripts/mode-b-lite/start-tunnel.sh 실행
  -> http://127.0.0.1:8080 접속
```

외부 HTTP inbound를 열지 않아도 SSM 터널로 live 화면을 볼 수 있습니다. 이 방식은 포트폴리오
시연 중 backend Service와 Kubernetes API를 public으로 공개하지 않기 위한 기본 경로입니다.

Mode 구분:

| 모드 | Terraform 대상 | Kubernetes 실행 위치 |
| --- | --- | --- |
| Mode A | 없음 | GitHub Pages fixture demo |
| Mode B Lite | `enable_k3s_lab=true` | EC2 안의 k3s |
| Mode B | `aws-dev-foundation` + `aws-dev-ephemeral` 예정 | Amazon EKS |
| Mode C | `enable_msk=true` 예정 | MSK 연동 proof |

## 검증 명령

```bash
terraform -chdir=infra/terraform/envs/bootstrap-cost-guard init -backend=false
terraform -chdir=infra/terraform/envs/aws-dev-foundation init -backend=false
terraform -chdir=infra/terraform/envs/aws-dev-ephemeral init -backend=false
terraform fmt -recursive
terraform -chdir=infra/terraform/envs/bootstrap-cost-guard validate
terraform -chdir=infra/terraform/envs/aws-dev-foundation validate
terraform -chdir=infra/terraform/envs/aws-dev-ephemeral validate
tflint
terraform plan
```

실제 Terraform 리소스 확장은 비용 상한, 공개 방식, domain 사용 여부를 확정한 뒤 진행합니다.
