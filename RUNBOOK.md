# Runbook

How to run the suite at each layer. Fill each section when its phase lands.

## Prerequisites

| Tool | Version | Check command |
|---|---|---|
| Docker Desktop | | |
| kind | | |
| kubectl | | |
| Java | | |
| Maven | | |

## Cluster lifecycle (kind)

| Action | Command |
|---|---|
| Create | |
| Start after reboot | |
| Check health | |
| Delete | |

## Local

- **Setup:**
- **Run regression suite:**
- **Run DAG demo suite:**
- **Enable writes:**
- **Expected result:**
- **Reports at:**

## Docker

- **Build runner image:**
- **Start Chrome container:**
- **Run suite:**
- **Expected result:**

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
| `connection refused` on 127.0.0.1:<port> | kind container stopped | | 2 |
