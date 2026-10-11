# Nithyakarma Delivery Pipeline

Quality Lab compares Playwright and Selenium test coverage. Nithyakarma Delivery Pipeline focuses on running a curated Java/Selenium and REST Assured suite through Jenkins, Docker, SonarQube, and Kubernetes against the live Nithyakarma PWA.

## Status

| Phase | Scope | Status |
|---|---|---|
| 0 | Setup + safety files | in progress |
| 1 | Framework + 14 tests (local) | |
| 2 | WebDriver BiDi | |
| 3 | Docker | |
| 4 | Kubernetes Job | |
| 5 | Jenkins | |
| 6 | SonarQube + Trivy + GHCR | |
| 7 | Docs + case study | |

## Architecture

<!-- Diagram + one line per pipeline stage: why it exists -->

## Test suites

<!-- Regression (independent, default) vs DAG demo suite; the 14 tests table -->

## How to run

See [RUNBOOK.md](RUNBOOK.md).

## Security

See [SECURITY.md](SECURITY.md) and [TEST-DATA.md](TEST-DATA.md).

## What would change in production

<!-- Namespaces, RBAC, external secrets manager, object storage for reports, Grid scaling, scheduled runs, separate test environment -->

## Lessons

See [BUGLOG.md](BUGLOG.md).
