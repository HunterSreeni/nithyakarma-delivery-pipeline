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
  - Regression, writes off: 23 run (T2 counts 7 rows, + CanaryTest from 2026-10-14), 20 passed, 3 skipped (T8, T9, T12).
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
- **Expected result (2026-10-13):** 22 run, 19 passed, 3 skipped (23 / 20 / 3 once CanaryTest is in the image). Results in `target\docker-surefire\`.
- **Tear down:** `docker rm -f nk-chrome`; `docker network rm nk-net`

## Kubernetes

- **Load images onto the node (after each rebuild):** `kind load docker-image nk-runner:0.1.0 --name nk`; Chrome (multi-platform, `kind load` fails, BUGLOG #9): `docker exec nk-control-plane crictl pull selenium/standalone-chrome:4.49.0-20260909`
- **Check node images:** `docker exec nk-control-plane crictl images`
- **Namespace (once):** `kubectl create namespace nk-e2e`
- **Create/refresh Secret:** `kubectl -n nk-e2e delete secret nk-env --ignore-not-found`, then `kubectl -n nk-e2e create secret generic nk-env --from-env-file=.env`. Check keys (never values): `kubectl -n nk-e2e describe secret nk-env`
- **Run Job:** `kubectl -n nk-e2e delete job nk-e2e --ignore-not-found`, then `kubectl apply -f k8s\e2e-job.yml` (Jobs are immutable; delete before re-running)
- **Wait for result:** `kubectl -n nk-e2e get pods -w`, then `kubectl -n nk-e2e get job nk-e2e` -> `Complete` (tests failed -> `Failed`; `backoffLimit: 0`, never auto-retried)
- **Get logs:** `kubectl -n nk-e2e logs -f job/nk-e2e -c runner`
- **Reports at:** repo `reports\surefire\` (TestNG/JUnit XML + HTML) and `reports\console\` (via node mount `/reports`, kind-config.yml)
- **Expected result (2026-10-13):** 22 run, 19 passed, 3 skipped (23 / 20 / 3 with CanaryTest), ~80s (Chrome is a native sidecar on `localhost:4444`)
- **Clean up:** `kubectl -n nk-e2e delete job nk-e2e`

## Jenkins

- **Image (once, or after editing `jenkins\Dockerfile`):** `docker build -t nk-jenkins:0.1.0 jenkins`
- **First start:** `docker volume create nk-jenkins-home`, then `docker run -d --name nk-jenkins --network kind --group-add 0 -p 8080:8080 -v nk-jenkins-home:/var/jenkins_home -v /var/run/docker.sock:/var/run/docker.sock nk-jenkins:0.1.0`; `docker update --restart unless-stopped nk-jenkins`
- **After a reboot:** start Docker Desktop; Jenkins restarts by itself (else `docker start nk-jenkins`). Open `http://localhost:8080`
- **Credentials (Manage Jenkins > Credentials > System > Global):** Secret text `nk-base-url`, `nk-supabase-url`, `nk-publishable-key`, `nk-service-role-key`, `nk-user-a-email`, `nk-user-b-email`; Secret file `nk-kubeconfig` from `kind get kubeconfig --internal --name nk | Out-File -Encoding ascii $env:TEMP\nk-kubeconfig` (delete the local file after upload)
- **Job:** Pipeline `nk-e2e`, Pipeline script from SCM, Git `https://github.com/HunterSreeni/nithyakarma-delivery-pipeline.git`, branch `*/main`, script `Jenkinsfile`
- **Parameters:** `SUITE` (regression / dag), `E2E_ALLOW_DATA_WRITES` (false), `DEMO_EXPECTED_FAILURE` (false). All default to the safe value, even on build #1 (BUGLOG #11)
- **Expected result (2026-10-14):** defaults -> SUCCESS, 22 run / 19 passed / 3 skipped (23 / 20 / 3 with CanaryTest). `DEMO_EXPECTED_FAILURE=true` -> FAILURE, only CanaryTest failed, build description "INTENTIONAL RED - demo"
- **Artifacts:** build page > Test Result (JUnit) and Build Artifacts `job-reports/surefire/` (+ `job-reports/console/` when a UI test failed). The Job is deleted only after these are archived.

## Troubleshooting

| Symptom | Likely cause | Fix | BUGLOG # |
|---|---|---|---|
| `connection refused` on 127.0.0.1:<port> | kind container stopped | Start Docker Desktop / `docker start nk-control-plane` | 2 |
| kubectl version skew warning | Older kubectl first on PATH | `where.exe kubectl`; use a kubectl within +/-1 minor of the server | 1 |
| `Bind for 0.0.0.0:4444 failed: port is already allocated` | Another container holds the port | `docker ps`; remove it, then recreate (not restart) `nk-chrome` | 7 |
| `Unknown lifecycle phase ".xxx"` / `Missing argument in parameter list` | PowerShell split an unquoted `-D` arg at a dot or comma | Quote it: `"-Dtest=A,B"` | 8 |
