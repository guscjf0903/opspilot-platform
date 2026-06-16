# ECR 모듈

OpsPilot 컨테이너 이미지를 저장할 private Amazon ECR 저장소를 생성합니다.

## 비용 가드레일

- 저장소는 private으로 생성합니다.
- 이미지는 AES-256 방식으로 암호화합니다.
- 예상치 못한 사용량을 줄이기 위해 image scan on push는 기본 비활성입니다.
- lifecycle policy로 untagged image를 빠르게 정리하고 최근 tagged image만 소량 유지합니다.

이 모듈은 저장소만 생성합니다. 이미지 빌드와 push는 GitHub Actions에서 별도로 처리합니다.
