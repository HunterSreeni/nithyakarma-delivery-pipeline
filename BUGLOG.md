# Bug Log

Every problem hit while building this pipeline. One entry per problem, newest at the bottom.
This file is the raw material for the case study.

## Index

| # | Date | Phase | Area | Title |
|---|---|---|---|---|
| 1 | 2026-10-11 | 0 | kubectl | Client/server version skew warning |
| 2 | 2026-10-11 | 0 | kind | API server connection refused |
| 3 | 2026-10-11 | 0 | git | Reports placeholder ignored by Git |
| 4 | 2026-10-11 | 0 | test data | RLS probe ran as the wrong user |
| 5 | 2026-10-13 | 1 | BiDi | Request intercept hung the test for 4 minutes |
| 6 | 2026-10-13 | 1 | test data | Write-test cleanup refused (HTTP 403) |
| 7 | 2026-10-13 | 3 | docker | Chrome container started without its ports |
| 8 | 2026-10-13 | 3 | shell | PowerShell broke Maven `-D` arguments |
| 9 | 2026-10-13 | 4 | kind | `kind load` failed for the Chrome image |

---

## Entry template (copy for each new bug)

### N. Title
- **Date:**
- **Phase:**
- **Symptom:** what you saw (exact error text)
- **Cause:** why it happened (root cause, not the trigger)
- **Fix:** what you changed
- **How I verified:** command/output that proved it was fixed
- **Lesson:** one line you would say in an interview

---

### 1. Client/server version skew warning
- **Date:** 2026-10-11
- **Phase:** 0
- **Symptom:** `kubectl version` warned that client 1.34 vs server 1.37 exceeds the supported minor version skew of +/-1.
- **Cause:** two kubectl copies installed. `where.exe kubectl` listed Docker Desktop's bundled copy (`C:\Program Files\Docker\Docker\resources\bin`) first. Windows reads System PATH before User PATH, so the Docker copy (System PATH) always beat the winget copy (User PATH).
- **Fix:** installed kubectl via `winget install Kubernetes.kubectl`. Docker Desktop also updated its bundled copy to 1.36.1, which is within skew of the 1.37 cluster.
- **How I verified:** `kubectl version` -> Client v1.36.1, Server v1.37.0, no skew warning.
- **Lesson:** kubectl supports +/-1 minor version against the API server; check which binary actually runs with `where.exe` before assuming.

### 2. API server connection refused
- **Date:** 2026-10-11
- **Phase:** 0
- **Symptom:** `kubectl version` -> `Unable to connect to the server: dial tcp 127.0.0.1:50229: connectex: No connection could be made because the target machine actively refused it.`
- **Cause:** the kind cluster runs as a Docker container (`nk-control-plane`). It was stopped after Docker Desktop / the PC restarted, so nothing was listening on the API server port.
- **Fix:** start Docker Desktop; the node container comes back up. If it does not, `docker start nk-control-plane`.
- **How I verified:** `docker ps` showed `nk-control-plane` Up with `127.0.0.1:50229->6443/tcp`; `kubectl get nodes` -> `nk-control-plane Ready`.
- **Lesson:** if Docker is not running, the kind cluster is not running either.

### 3. Reports placeholder ignored by Git
- **Date:** 2026-10-11
- **Phase:** 0
- **Symptom:** `reports/placeholder.txt` never appeared in `git status`, even with `!reports/placeholder.txt` in `.gitignore`. `git check-ignore -v reports/placeholder.txt` -> `.gitignore:3:reports/`.
- **Cause:** `reports/` ignores the directory itself. Git does not look inside an ignored directory, so a negation rule for a file inside it can never apply.
- **Fix:** changed `reports/` to `reports/*` in `.gitignore`, keeping `!reports/placeholder.txt`.
- **How I verified:** `git check-ignore -v reports/placeholder.txt` prints nothing; `git ls-files reports` -> `reports/placeholder.txt`.
- **Lesson:** to keep one file in an ignored folder, ignore the folder's contents (`folder/*`), not the folder (`folder/`). A fresh clone (or Jenkins) needs the folder to exist for the kind mount.

### 4. RLS probe ran as the wrong user
- **Date:** 2026-10-11
- **Phase:** 0
- **Symptom:** SQL probe "A reads own children" returned 2 rows, but the app showed 4 children for A.
- **Cause:** the emails for `probe.a` and `probe.b` were swapped, so the probe impersonated B (2 children) instead of A (4). The RLS results were still correct, just from the wrong side.
- **Fix:** swapped the emails and re-ran the probes.
- **How I verified:** `select count(*) from family_members where parent_id = '<id>'` per user -> A = 4, B = 2; re-run probe as A -> `1, 0, 4, 0`.
- **Lesson:** cross-check every probe against a known baseline value; a passing security check run as the wrong identity proves nothing about the identity you meant.

### 5. Request intercept hung the test for 4 minutes
- **Date:** 2026-10-13
- **Phase:** 1
- **Symptom:** T10 (`ResilienceTest`) ran 240s and failed with `java.util.concurrent.TimeoutException` thrown from inside the `onBeforeRequestSent` callback; `Exception in thread "BiDi Connection"`.
- **Cause:** the callback called `network.failRequest()` / `continueRequest()`. BiDi events are delivered on the connection's own thread, and a BiDi command waits for its reply on that same thread, so the command could never complete (deadlock). Intercepting every request made it worse: every page request paused.
- **Fix:** intercept only the target URL (`UrlPattern` host + path), and send `failRequest` from a separate single-thread executor. Remove the intercept in `finally`; `Network.close()` does not remove it.
- **How I verified:** `mvn test -Dtest=ResilienceTest` passes in seconds; the "requests failed" counter is positive.
- **Lesson:** never send a BiDi/WebSocket command from inside its own event callback; hand it to another thread.

### 6. Write-test cleanup refused (HTTP 403)
- **Date:** 2026-10-13
- **Phase:** 1
- **Symptom:** with writes on, T8/T9/T12 failed with `restore profiles failed: HTTP 403` / `clean up practice_logs failed: HTTP 403`. Test account A was left with +5 punya, streak 1 and one extra practice log.
- **Cause:** the plan assumed the service-role key can write everything. In this project `service_role` only has SELECT on `profiles`, `practice_logs` and `user_practices` (UPDATE/DELETE were revoked), so the service role can read and verify but not clean up.
- **Fix:** A was reset by hand via the Supabase MCP (SQL, exact pre-run snapshot). The code now has `AdminData` (service-role reads only) and `Cleanup` (restores as the data's owner, through RLS). Cleanup verifies each restore and fails loudly with "manual reset needed" if the app refuses it.
- **How I verified:** SQL snapshot of A before and after the write run is identical (punya 121, streak 0, 20 logs, 3 streak events, practices unchanged).
- **Lesson:** check what your "admin" credential can actually do before you rely on it for cleanup, and make cleanup verify itself; a silent leftover on production data is worse than a red build.
- **Note:** T8's profile restore only works because of the T15 bug (users can edit their own punya/streak). When the app is fixed, T8 cleanup needs another route (e.g. a cleanup RPC for the test account).

### 7. Chrome container started without its ports
- **Date:** 2026-10-13
- **Phase:** 3
- **Symptom:** `docker run ... -p 4444:4444 ... nk-chrome` -> `Bind for 0.0.0.0:4444 failed: port is already allocated`. After freeing the port, `docker start nk-chrome` ran, but `docker ps` showed `4444/tcp` instead of `0.0.0.0:4444->4444/tcp`.
- **Cause:** a stray `selenium/standalone-chrome:latest` container (random name) already held 4444/7900. `docker run` = create + start: the create succeeded, the start failed at port binding, and restarting that half-made container did not publish the ports.
- **Fix:** removed the stray container, then `docker rm -f nk-chrome` and ran the full `docker run` again.
- **How I verified:** `docker ps` -> `0.0.0.0:4444->4444/tcp`; `curl.exe -s http://localhost:4444/status` -> `"ready": true`.
- **Lesson:** a failed `docker run` can leave a container in the Created state (`docker ps -a`); recreate it rather than restart it. Pin image tags, never `latest`.

### 8. PowerShell broke Maven `-D` arguments
- **Date:** 2026-10-13
- **Phase:** 3
- **Symptom:** `mvn test -Dtest=DashboardTest -Dsurefire.failIfNoSpecifiedTests=false` -> `Unknown lifecycle phase ".failIfNoSpecifiedTests=false"`. With `-Dtest=DashboardTest,ResilienceTest` -> `Missing argument in parameter list`.
- **Cause:** PowerShell parses the whole line before any program sees it: an unquoted `-D` argument is split at the first dot, and a comma is read as an array operator. Earlier runs used Git Bash, which does not do this. It applies to arguments passed through `docker run` too.
- **Fix:** quote those arguments: `"-Dsurefire.failIfNoSpecifiedTests=false"`, `"-Dtest=DashboardTest,ResilienceTest"`. RUNBOOK commands updated.
- **How I verified:** both commands ran; DashboardTest + ResilienceTest passed (4/4) in a `maven` container against `nk-chrome`.
- **Lesson:** the shell is part of the pipeline; write commands for the shell the CI agent actually uses (Jenkins on Linux = sh).

### 9. `kind load` failed for the Chrome image
- **Date:** 2026-10-13
- **Phase:** 4
- **Symptom:** `kind load docker-image selenium/standalone-chrome:4.49.0-20260909 --name nk` -> `ctr: content digest sha256:66e7ec5c...: not found`. `kind load` of `nk-runner:0.1.0` worked.
- **Cause:** Docker Desktop uses the containerd image store. The Selenium image is multi-platform (amd64 + arm64) and only the amd64 layers were downloaded locally, but `kind load` exports the whole image index and imports it with `--all-platforms`, so the arm64 layers it points to are missing. `nk-runner` is built locally for one platform, so it has no such gap.
- **Fix:** let the node pull the image itself: `docker exec nk-control-plane crictl pull selenium/standalone-chrome:4.49.0-20260909`. The Job uses `imagePullPolicy: IfNotPresent`.
- **How I verified:** `docker exec nk-control-plane crictl images` lists both images; the Job ran 22 / 19 passed / 3 skipped.
- **Lesson:** a kind node is a container running containerd; `docker exec` + `crictl` work on it like SSH on a real node. `kind load` suits locally built images; public multi-platform images are simpler to pull on the node.
