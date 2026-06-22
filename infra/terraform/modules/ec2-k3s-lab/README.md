# EC2 k3s Lab Module

이 모듈은 Mode B Lite 저비용 라이브 데모를 위해 단일 EC2 인스턴스에 k3s를 설치합니다.

생성 리소스:

- EC2 1대
- 암호화된 gp3 root EBS volume
- SSM Session Manager용 IAM Role/Profile
- ECR pull 권한
- 기본 inbound가 닫힌 Security Group

비용 주의:

- EC2 instance-hour
- EBS GB-month
- public IPv4 address hour
- ECR 저장소 용량

기본값은 SSH, Kubernetes API, HTTP/HTTPS inbound를 모두 열지 않습니다. 접속은 SSM을
기본으로 사용합니다.

화면 확인은 아래 구조를 사용합니다.

```text
노트북 browser
  -> SSM port forwarding
    -> EC2 127.0.0.1:8080
      -> kubectl port-forward
        -> opspilot-frontend Service
          -> nginx /api proxy
            -> opspilot-backend Service
```

이 구조에서는 EC2의 public HTTP inbound를 열지 않아도 live UI와 backend API를 확인할 수
있습니다.
