# Security

## Rule

<!-- One or two sentences: the service-role/secret key bypasses RLS (full DB access). It never enters Git, browser code, screenshots, logs, or reports. -->

## Secrets inventory

| Secret | What it grants | Who/what uses it | Sensitivity (high/medium/low) |
|---|---|---|---|
| `SUPABASE_SERVICE_ROLE_KEY` | | | |
| `SUPABASE_PUBLISHABLE_KEY` | | | |
| User A / B session tokens (JWTs) | | | |
| GHCR push token | | | |
| SonarQube token | | | |
| kubeconfig | | | |

## Where each secret lives, per layer

| Secret | Local (`.env`) | Docker (runtime env) | Kubernetes (Secret) | Jenkins (credential ID) |
|---|---|---|---|---|
| `SUPABASE_SERVICE_ROLE_KEY` | | | | |
| `SUPABASE_PUBLISHABLE_KEY` | | | | |
| GHCR push token | | | | |
| SonarQube token | | | | |
| kubeconfig | | | | |

## What must never contain a secret

<!-- List: Git history, Docker image layers, test code, browser localStorage dumps, screenshots, console/network captures, JUnit/HTML reports, Jenkins console output -->

## Controls

| Control | How it is enforced | How it is checked |
|---|---|---|
| `.env` never committed | | |
| No secrets in image layers | | |
| Jenkins masks credentials in logs | | |
| Token scrubber on console/network captures | | |
| Raw captures never archived | | |
| Service role used for setup/cleanup only, never for assertions | | |

## If a secret leaks

<!-- Steps: rotate the key in Supabase, revoke/replace Jenkins credential and K8s Secret, purge from Git history if committed, check Supabase logs for misuse, note it in BUGLOG.md -->
