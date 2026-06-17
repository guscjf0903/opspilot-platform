# GitHub Actions ECR Push Role 모듈

이 모듈은 GitHub Actions가 AWS access key 없이 ECR에 이미지를 push할 수 있도록
GitHub OIDC Provider와 IAM Role을 구성합니다.

## 역할

- GitHub Actions OIDC Provider를 생성하거나 기존 Provider ARN을 사용합니다.
- 특정 GitHub repository, branch, environment subject만 Role을 assume할 수 있게 제한합니다.
- 지정한 ECR repository ARN에 대해서만 image push 권한을 부여합니다.
- `terraform apply`나 EKS 배포 권한은 주지 않습니다.

## 권한 범위

허용되는 작업:

- `ecr:GetAuthorizationToken`
- `ecr:BatchCheckLayerAvailability`
- `ecr:InitiateLayerUpload`
- `ecr:UploadLayerPart`
- `ecr:CompleteLayerUpload`
- `ecr:PutImage`

허용하지 않는 작업:

- Terraform apply/destroy
- IAM, EKS, RDS, MSK, ALB 생성
- 다른 ECR repository 접근
- AWS access key 발급

## OIDC Provider 중복 주의

AWS 계정에는 같은 URL의 OIDC Provider를 하나만 만들 수 있습니다. 이미
`https://token.actions.githubusercontent.com` Provider가 있다면 아래처럼 기존 ARN을
사용해야 합니다.

```hcl
create_oidc_provider       = false
existing_oidc_provider_arn = "arn:aws:iam::<account-id>:oidc-provider/token.actions.githubusercontent.com"
```
