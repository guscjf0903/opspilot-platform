# AWS 개발용 임시 환경

이 환경은 OpsPilot을 AWS에서 짧게 시연하기 위한 Mode B 기반 구성입니다.

현재 범위:

- 백엔드와 프론트엔드 이미지를 저장할 private ECR 저장소
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
default_ttl_hours  = 4
```

AWS 계정, 월 예산, 예상 종료 시간을 확인하기 전에는 `terraform apply`를 실행하지 않습니다.

## 검증

```bash
terraform -chdir=infra/terraform/envs/aws-dev-ephemeral init -backend=false
terraform -chdir=infra/terraform/envs/aws-dev-ephemeral validate
```
