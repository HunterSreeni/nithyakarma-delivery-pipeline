# Runbook

How to run the suite at each layer. Fill each section when its phase lands.

## Prerequisites

| Tool | Version (2026-10-11) | Check command |
|---|---|---|
| Docker Desktop (engine) | 29.8.2 | `docker --version`, `docker info` |
| kind | 0.33.0 | `kind version` |
| kubectl | client 1.36.1, server 1.37.0 | `kubectl version` (no skew warning) |
| Java | 21.0.9 LTS | `java -version` |
| Maven | 3.10.0 | `mvn -v` |

## Cluster lifecycle (kind)

| Action | Command |
|---|---|
| Create | `kind create cluster --name nk --config kind-config.yml` |
| Start after reboot | Start Docker Desktop; if the node is still down: `docker start nk-control-plane` |
| Check health | `kubectl get nodes` (expect `nk-control-plane Ready`) |
| Check reports mount | `docker exec nk-control-plane ls /reports` |
| Delete | `kind delete cluster --name nk` |

## Local

- **Setup:** copy `.env.example` to `.env`, fill it in, then load it into the PowerShell window (once per window):
  `Get-Content .env | ? { $_ -match '^[A-Z_]+=' } | % { $k,$v = $_ -split '=',2; Set-Item "env:$k" $v }`
- **Run regression suite:** `mvn test` (default suite `testng-regression.xml`, ~6 min)
- **Run one class:** `mvn test -Dtest=RlsTest "-Dsurefire.failIfNoSpecifiedTests=false"`
- **Run DAG demo suite:** `mvn test "-Dsuite=testng-dag.xml"`; add `"-Ddag.breakSession=true"` to break T5 on purpose
- **PowerShell note:** quote any `-D` argument that contains a dot, or PowerShell splits it at the dot and Maven sees a bogus phase.
- **Run known bugs (T15, expected to FAIL):** `$env:E2E_ALLOW_DATA_WRITES="true"; mvn test "-Dsuite=testng-known-bugs.xml"`
- **Enable writes:** `$env:E2E_ALLOW_DATA_WRITES="true"` (T8, T9, T12 run instead of skipping; they restore A afterwards)
- **Watch the browser:** `$env:HEADLESS="false"`
- **Expected result (2026-10-13):**
  - Regression, writes off: 22 run (T2 counts 7 rows), 19 passed, 3 skipped (T8, T9, T12).
  - Writes on: T8, T9, T12 pass, and A matches the TEST-DATA baseline afterwards.
  - DAG with break: T5 FAILED, T6-T14 SKIPPED.
- **Reports at:** `target/surefire-reports/` (TestNG + JUnit XML); scrubbed browser console of failed UI tests in `reports/console/`

## Docker

- **One-time network:** `docker network create nk-net`
- **Start Chrome container:** `docker run -d --name nk-chrome --network nk-net --shm-size=2g -e TZ=Asia/Kolkata -p 4444:4444 -p 7900:7900 selenium/standalone-chrome:4.49.0-20260909`
  - Ready check: `curl.exe -s http://localhost:4444/status` -> `"ready": true`
  - Watch: `http://localhost:7900/?autoconnect=1&resize=scale&password=secret`
- **Build runner image:** `docker build -t nk-runner:0.1.0 .` (no secrets in the image; `.dockerignore` blocks `.env*`)
- **Check image for secrets:** `docker run --rm nk-runner:0.1.0 ls -a /work` (no `.env`) and `docker history --no-trunc nk-runner:0.1.0` (only Dockerfile steps)
- **Run suite:** `docker run --rm --network nk-net --env-file .env -e SELENIUM_REMOTE_URL=http://nk-chrome:4444 -v "${PWD}\reports:/work/reports" -v "${PWD}\target\docker-surefire:/work/target/surefire-reports" nk-runner:0.1.0`
  - Other suite: append `mvn -B test "-Dsuite=testng-dag.xml"` after the image name
- **Expected result (2026-10-13):** 22 run, 19 passed, 3 skipped. Results in `target\docker-surefire\`.
- **Tear down:** `docker rm -f nk-chrome`; `docker network rm nk-net`

## Kubernetes

- **Create/refresh Secret:**
- **Run Job:**
- **Wait for result:**
- **Get logs:**
- **Reports at:**
- **Clean up:**

## Jenkins

- **Start Jenkins:**
- **Parameters:** `SUITE`, `E2E_ALLOW_DATA_WRITES`, `DEMO_EXPECTED_FAILURE`
- **Expected result:**
- **Artifacts:**

## Troubleshooting

| Symptom | Likely cause | Fix | BUGLOG # |
|---|---|---|---|
| `connection refused` on 127.0.0.1:<port> | kind container stopped | Start Docker Desktop / `docker start nk-control-plane` | 2 |
| kubectl version skew warning | Older kubectl first on PATH | `where.exe kubectl`; use a kubectl within +/-1 minor of the server | 1 |
| `Bind for 0.0.0.0:4444 failed: port is already allocated` | Another container holds the port | `docker ps`; remove it, then recreate (not restart) `nk-chrome` | 7 |
| `Unknown lifecycle phase ".xxx"` / `Missing argument in parameter list` | PowerShell split an unquoted `-D` arg at a dot or comma | Quote it: `"-Dtest=A,B"` | 8 |
