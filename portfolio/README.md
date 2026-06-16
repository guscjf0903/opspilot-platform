# OpsPilot Public Portfolio

이 디렉토리는 GitHub Pages로 공개할 Mode A 정적 포트폴리오 landing page입니다.

목적은 AWS 인프라 자체를 보여주는 것이 아니라, 외부 방문자가 OpsPilot의 문제의식,
핵심 기능, 데모 흐름, 시스템 구조를 빠르게 이해하고 `/demo/`의 Vue read-only demo로
이동하도록 만드는 것입니다.

GitHub Pages artifact는 다음 구조로 조립합니다.

```text
/
  index.html              # 이 landing page
  assets/
  demo/                   # frontend Vue 앱 dist
```

## 로컬 확인

브라우저에서 아래 파일을 열어 확인합니다.

```text
portfolio/index.html
```

## GitHub Pages

`.github/workflows/pages.yml` workflow가 `portfolio/`와 `frontend/dist`를 함께
GitHub Pages artifact로 업로드합니다.

## 콘텐츠 원칙

- 첫 화면은 OpsPilot 제품 가치와 데모 흐름을 우선한다.
- Terraform, Helm, EKS 이야기는 배포 전략 섹션에서 보조적으로 설명한다.
- AWS account id, credential, kubeconfig, Terraform state, secret은 넣지 않는다.
- 실제 live endpoint는 시간 제한과 read-only 정책이 확정되기 전까지 공개하지 않는다.
