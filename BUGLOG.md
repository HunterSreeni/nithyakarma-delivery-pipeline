# Bug Log

Every problem hit while building this pipeline. One entry per problem, newest at the bottom.
This file is the raw material for the case study.

## Index

| # | Date | Phase | Area | Title |
|---|---|---|---|---|
| 1 | 2026-10-11 | 0 | kubectl | Client/server version skew warning |
| 2 | 2026-10-11 | 0 | kind | API server connection refused |

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
- **How I verified:**
- **Lesson:** kubectl supports +/-1 minor version against the API server; check which binary actually runs with `where.exe` before assuming.

### 2. API server connection refused
- **Date:** 2026-10-11
- **Phase:** 0
- **Symptom:** `kubectl version` -> `Unable to connect to the server: dial tcp 127.0.0.1:50229: connectex: No connection could be made because the target machine actively refused it.`
- **Cause:** the kind cluster runs as a Docker container (`nk-control-plane`). It was stopped after Docker Desktop / the PC restarted, so nothing was listening on the API server port.
- **Fix:**
- **How I verified:**
- **Lesson:** if Docker is not running, the kind cluster is not running either.
