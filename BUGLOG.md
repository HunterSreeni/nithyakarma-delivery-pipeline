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
