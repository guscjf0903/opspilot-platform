# Flyway migrations

이 디렉토리는 OpsPilot PostgreSQL schema migration을 관리합니다.

현재 migration:

- `V1__create_trend_snapshots.sql`
  - `kubernetes_workload_usage_snapshots`
  - `cost_daily_snapshots`
- `V2__create_ai_analysis_reports.sql`
  - `incident_analysis_reports`
- `V3__create_action_audit_logs.sql`
  - `action_audit_logs`
  - `action_approval_requests`

원본 CPU/Memory metric 전체를 PostgreSQL에 복제하지 않고, Prometheus와 OpenCost
조회 결과를 OpsPilot이 요약한 snapshot만 저장합니다.

AI 분석 결과는 provider, model, provider response id, latency, token usage와 함께
저장합니다. Secret, token, password 원문은 저장하지 않습니다.

운영 조치 이력은 dry-run, 승인 대기, 실행 성공, 실행 실패 상태와 함께 before/after
state, diff, parameter를 JSONB로 저장합니다. 승인 workflow는 별도 approval request
table에 저장하며, 실제 사용자 인증 연동 전까지 actor와 role은 요청 header 기반으로
기록합니다.
