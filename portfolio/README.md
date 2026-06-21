# OpsPilot 공개 포트폴리오

이 디렉토리는 GitHub Pages로 공개할 Mode A 정적 포트폴리오 첫 화면입니다.

목적은 AWS 인프라 자체를 보여주는 것이 아니라, 외부 방문자가 OpsPilot의 문제의식,
핵심 기능, 데모 흐름, 시스템 구조를 빠르게 이해하고 `/demo/`의 Vue 읽기 전용 데모로
이동하도록 만드는 것입니다.

GitHub Pages artifact는 다음 구조로 조립합니다.

```text
/
  index.html              # 이 첫 화면
  assets/
  demo/                   # frontend Vue 앱 dist
```

## 로컬 확인

첫 화면만 빠르게 볼 때는 브라우저에서 아래 파일을 열어 확인합니다.

```text
portfolio/index.html
```

GitHub Pages에 올라갈 실제 산출물과 `/demo/` Vue 앱까지 확인할 때는 루트에서 아래
스크립트를 실행합니다.

```bash
./scripts/build-mode-a-pages.sh
python3 -m http.server 4173 -d .mode-a-pages
```

브라우저에서 `http://localhost:4173`을 열면 `/` 포트폴리오와 `/demo/` 읽기 전용 앱을
같이 확인할 수 있습니다.

## GitHub Pages

`.github/workflows/pages.yml` 워크플로우가 `./scripts/build-mode-a-pages.sh`를 실행해
`portfolio/`와 `frontend/dist`를 `.mode-a-pages` artifact로 조립한 뒤 GitHub Pages에
업로드합니다.

GitHub repository 설정에서 Pages Source를 `GitHub Actions`로 선택해야 공개 URL이
생성됩니다.

`Configure Pages` 단계에서 `Get Pages site failed` 또는 `Not Found`가 나오면 Pages가
아직 활성화되지 않은 상태입니다. `Settings -> Pages -> Build and deployment -> Source`를
`GitHub Actions`로 바꾼 뒤 실패한 workflow를 다시 실행합니다.

## Mode A 공개 범위

| 구분 | 내용 |
| --- | --- |
| 공개 | 첫 화면, 아키텍처 요약, `/demo/` Vue 읽기 전용 앱 |
| 데이터 | `frontend/src/demo` fixture snapshot |
| 비공개 | AWS account id, kubeconfig, Terraform state, secret, live endpoint |
| 비활성 | 실제 restart, scale, rollback, pod delete 같은 운영 action |
| 연결 안 함 | 상시 EKS, ALB, RDS, MSK, NAT Gateway |

## 콘텐츠 원칙

- 첫 화면은 OpsPilot 제품 가치와 데모 흐름을 우선합니다.
- Terraform, Helm, EKS 이야기는 배포 전략 섹션에서 보조적으로 설명합니다.
- AWS account id, credential, kubeconfig, Terraform state, secret은 넣지 않습니다.
- 실제 live endpoint는 시간 제한과 읽기 전용 정책이 확정되기 전까지 공개하지 않습니다.
