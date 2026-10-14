# Nithyakarma Delivery Pipeline

A Java + Selenium + REST Assured test suite that runs against the live [Nithyakarma](https://app.nithyakarma.org) PWA through a local delivery pipeline: Docker, a Kubernetes Job on kind, and Jenkins.

Companion to Quality Lab: Quality Lab compares Playwright and Selenium test coverage; this repo is about **delivering** a curated suite safely through CI, against production data, with secrets handled at every layer.

## Status

| Phase | Scope | Status |
|---|---|---|
| 0 | Setup + safety files (SECURITY, TEST-DATA, RUNBOOK, BUGLOG) | done |
| 1 | Framework + tests T1-T15 (local Chrome) | done |
| 2 | WebDriver BiDi: JS error guard, network waits, request interception, token scrubber | done |
| 3 | Docker: runner image + Selenium standalone Chrome on a Docker network | done |
| 4 | Kubernetes: suite as a kind Job with a native Chrome sidecar | done |
| 5 | Jenkins: parameterised pipeline, green + labelled intentional red build | done |
| 6 | SonarQube analysis + quality gate | parked (future work) |
| 7 | Docs + case study | done |

Future work (deliberately out of scope for now): SonarQube quality gate, Trivy image scan, GHCR registry push, see [Future work](#future-work).

## Architecture

```
GitHub (this repo)
  |
Jenkins (Docker container on the "kind" Docker network, drives Docker Desktop via docker.sock)
  1 Checkout
  2 Build runner image (also compiles the tests)  ->  kind load into the cluster
  3 Secret + ConfigMap from Jenkins credentials / build parameters
  4 Run tests: kubectl apply Job -> stream logs -> wait for Complete OR Failed
  post always: copy reports out of the node -> JUnit + archive -> THEN delete the Job
  |
kind cluster (namespace nk-e2e)
  Job nk-e2e (backoffLimit 0, activeDeadlineSeconds 1800)
    - runner:  mvn test -Dsuite=$(SUITE), RemoteWebDriver -> localhost:4444
    - chrome:  selenium/standalone-chrome as a NATIVE SIDECAR (initContainer, restartPolicy Always),
               startupProbe on /status, TZ=Asia/Kolkata, /dev/shm as memory emptyDir
    - env:     Secret nk-env (secrets) + ConfigMap nk-run (SUITE, E2E_ALLOW_DATA_WRITES, DEMO_EXPECTED_FAILURE)
    - reports: hostPath /reports (kind extraMounts -> repo reports/; local-only workaround)
  |
internet -> Nithyakarma PWA (Netlify) + Supabase (REST, Auth, RPC)
```

Why each stage exists:

| Stage | Why |
|---|---|
| Build runner image | One immutable image per build (`nk-runner:<build>`); a compile error fails here, before any browser starts |
| Secret + run settings | Secrets come from Jenkins credentials (masked), never from the image or Git; run settings are plain config in a ConfigMap |
| Run tests in kind | Same image the cluster would run; waits for Complete OR Failed because `kubectl wait` alone hangs on a failed Job |
| post always | Reports are archived even on failure, and only then is the Job deleted |

## Test suites

| # | Test | Type | Group |
|---|---|---|---|
| T1 | Login page renders all elements | UI | public |
| T2 | Login field validation (7 data-driven rows) | UI | public |
| T3 | Captcha blocks an automated login | UI | public |
| T4 | Password login without captcha token is rejected (400 `captcha_failed`) | API | public |
| T5 | Session loads the dashboard with today's date in IST | UI | session |
| T6 | Every tab navigates to its page | UI | features |
| T7 | Panchangam shows day details and three kalams | UI | features |
| T8 | Marking Morning Sandhya saves exactly one log (restores A afterwards) | UI + API | writes |
| T9 | Leaderboard toggle is saved and restored | UI | writes |
| T10 | App stays usable when the practices request fails (BiDi intercept) | UI | features |
| T11 | User cannot read another user's profile (RLS) | API | features |
| T12 | User cannot write another user's data (RLS) | API | writes |
| T13 | User sees only their own children (RLS) | API | features |
| T14 | Logout returns to the login page | UI | logout |
| T15 | User cannot edit server-owned profile fields (**known bug, expected to FAIL**) | API | known-bug |
| - | Pipeline canary (fails only with `DEMO_EXPECTED_FAILURE=true`) | none | canary |
| - | Token scrubber unit tests (2) | unit | none |

- **Regression** (`testng-regression.xml`, default): independent tests, `known-bug` excluded. Writes off: 23 run, 20 passed, 3 skipped (T8, T9, T12).
- **DAG demo** (`testng-dag.xml`): same code, group dependencies declared only in the XML: `public -> session -> features -> logout`, with `writes` as a side branch of `session`. Break T5 (`-Ddag.breakSession=true`) and everything downstream is SKIPPED, pointing at the root failure.
- **Known bugs** (`testng-known-bugs.xml`): T15 only, documents an open security finding (see [TEST-DATA.md](TEST-DATA.md)).
- Every UI test fails if the page threw an uncaught JavaScript error (BiDi `LogInspector` + a TestNG listener), even when its assertions passed.

## How to run

Full commands for every layer (local, Docker, Kubernetes, Jenkins) are in [RUNBOOK.md](RUNBOOK.md). Quick start:

```
mvn test                                   # local Chrome, regression suite, writes off
mvn test "-Dsuite=testng-dag.xml"          # DAG demo suite
```

Jenkins parameters, all safe by default: `SUITE` (regression), `E2E_ALLOW_DATA_WRITES` (false), `DEMO_EXPECTED_FAILURE` (false; when true only the canary fails and the build is labelled "INTENTIONAL RED - demo").

## Evidence

| Build | Result | Screenshot |
|---|---|---|
| #3, all defaults | SUCCESS, 23 run / 20 passed / 3 skipped | [test result](docs/screenshots/build-3-green-test-result.png) |
| #4, `DEMO_EXPECTED_FAILURE=true` | FAILURE, only CanaryTest failed, labelled | [test result](docs/screenshots/build-4-intentional-red-test-result.png), [summary](docs/screenshots/build-4-intentional-red-summary.png) |
| Stage view | builds #1-#4 | [overview](docs/screenshots/jenkins-job-overview.png) |

## Security

The suite runs against **production data** with two dedicated accounts, writes off by default. The Supabase service-role key is a runtime-only secret: Jenkins credential -> K8s Secret -> container env, never in Git, image layers, logs or reports. Captured browser console output is passed through a token scrubber before it is written. Details: [SECURITY.md](SECURITY.md) and [TEST-DATA.md](TEST-DATA.md).

## What would change in production

- A separate test environment and seeded test data instead of production accounts.
- A real registry (e.g. GHCR) with signed, scanned images instead of `kind load`.
- An external secrets manager (Vault, cloud secret manager, External Secrets Operator) instead of Jenkins credentials -> `kubectl create secret`.
- Namespaced RBAC: a service account that can only manage Jobs in `nk-e2e`, not a cluster-admin kubeconfig.
- Reports to object storage or a report service, not a hostPath on the node.
- Jenkins builds on agents, not the controller; scheduled and per-commit runs.
- Selenium Grid sized and scaled for parallel classes (see BUGLOG #10).

## Future work

- **SonarQube:** static analysis plus a quality gate stage that waits on the Sonar webhook. The framework code lives in `src/test/java`, so the analysis must point `sonar.sources` there.
- **Trivy:** scan the runner image and fail the build on CRITICAL vulnerabilities.
- **GHCR:** push images tagged with build number + git SHA; the Job pulls that tag.
- Bump Selenium to 4.51 when a matching Grid image is published.
- Move T8 cleanup to a dedicated route once the T15 bug is fixed (T8's restore currently relies on it).

## Lessons

Every problem hit while building this, with cause, fix and verification: [BUGLOG.md](BUGLOG.md).

Case study: [hunter-sreeni.netlify.app/case-studies/delivery-pipeline.html](https://hunter-sreeni.netlify.app/case-studies/delivery-pipeline.html)
