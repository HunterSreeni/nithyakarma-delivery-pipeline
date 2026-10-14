# Security

## Rule

The Supabase service-role key bypasses Row Level Security and has full access to the production database. It never enters Git, browser code, Docker image layers, screenshots, logs, or reports. Tests use it only for setup and cleanup, never for assertions.

## Secrets inventory

| Secret | What it grants | Who/what uses it | Sensitivity |
|---|---|---|---|
| `SUPABASE_SERVICE_ROLE_KEY` | Bypasses RLS; admin auth API (mint magic links). In this project it is SELECT-only on the app tables (BUGLOG #6) | Test setup and verification only: create sessions, snapshot rows, verify B's rows unchanged. Cleanup runs as the data owner, not as service role | **High** |
| `SUPABASE_PUBLISHABLE_KEY` | Identifies the project to the API; everything else is limited by RLS. Public by design (ships in the app's browser bundle) | REST Assured requests (`apikey` header), session setup | Low (still kept out of Git for hygiene) |
| User A / B session tokens (JWTs) | Act as that user until expiry | Minted at runtime per test class, held in memory only | **High** while valid |
| User A / B emails | Identify the test accounts (PII) | Session setup | Medium |
| SonarQube token | Submit analysis to SonarQube | Jenkins Sonar stage (Phase 6) | Low (local Sonar only) |
| kubeconfig | Admin access to the local kind cluster | Jenkins deploy/run stages | Medium (local cluster only) |

## Where each secret lives, per layer

| Secret | Local | Docker | Kubernetes | Jenkins credential ID |
|---|---|---|---|---|
| `SUPABASE_SERVICE_ROLE_KEY` | `.env` (git-ignored) | `--env-file .env` at `docker run`, never `ENV` in Dockerfile | Secret `nk-env` (`envFrom`) | `nk-service-role-key` (Secret text) |
| `SUPABASE_PUBLISHABLE_KEY` | `.env` | runtime env | Secret `nk-env` | `nk-publishable-key` (Secret text) |
| `SUPABASE_URL`, `BASE_URL` | `.env` | runtime env | Secret `nk-env` | `nk-supabase-url`, `nk-base-url` (Secret text) |
| User A / B emails | `.env` | runtime env | Secret `nk-env` | `nk-user-a-email`, `nk-user-b-email` (Secret text) |
| User A / B JWTs | memory only | memory only | memory only | never stored |
| SonarQube token | not needed | not needed | not needed | `nk-sonar-token` (Secret text) |
| kubeconfig | `~/.kube/config` (from kind) | not needed | not applicable | `nk-kubeconfig` (Secret file, internal kind address) |

The Kubernetes Secret is created by Jenkins from its credentials on every run (`kubectl create secret ... --dry-run=client -o yaml | kubectl apply -f -`). No Secret manifest with real values is ever committed.

## What must never contain a secret

- Git history (including `.env`, kubeconfig, Secret manifests)
- Docker image layers (`docker history` must show none)
- Test code and config files
- Browser localStorage dumps and screenshots
- Raw BiDi console and network captures (they hold JWTs, `apikey` headers, magic-link URLs)
- JUnit / HTML / Allure reports
- Jenkins console output and archived artifacts
- README screenshots and the case study

## Controls

| Control | How it is enforced | How it is checked |
|---|---|---|
| `.env` never committed | `.gitignore`; only `.env.example` with placeholders is tracked | `git ls-files` shows no `.env`; review before each push |
| No secrets in image layers | Secrets passed at runtime only; `.dockerignore` excludes `.env` | `docker history --no-trunc` on the runner image |
| Jenkins masks credentials in logs | `withCredentials` bindings only | Search build logs for key prefixes |
| Token scrubber on console/network captures | Scrubber runs before any capture is written to reports | Unit test feeds a fake JWT, `apikey` header and magic link, asserts all redacted |
| Raw captures never archived | Raw files stay in the pod / temp folder; only scrubbed files archived | Inspect archived artifacts after each phase |
| Service role for setup/verification only | Assertions and cleanup use user A/B JWTs | Code review; RLS tests proven to fail when pointed at A's own data |
| Writes off by default | `E2E_ALLOW_DATA_WRITES=false` locally and in Jenkins | Default build run shows write tests SKIPPED |

## If a secret leaks

1. Rotate the key in the Supabase dashboard immediately (service-role key first).
2. Update the Jenkins credential and local `.env`; the next run recreates the K8s Secret.
3. If it was committed: remove it from Git history and force-push; treat the old key as compromised regardless.
4. Check Supabase logs for unexpected requests since the leak.
5. Record it in BUGLOG.md: what leaked, where, how it was caught, what changed to stop it happening again.
