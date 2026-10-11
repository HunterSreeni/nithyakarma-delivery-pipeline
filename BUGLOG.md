1. Version mismatch:
  - Symptom: kubectl 1.34 talking to a 1.37 cluster triggers the skew warning.
  - Cause: where.exe kubectl shows the kubectl on your PATH is the one bundled with Docker Desktop (C:\Program Files\Docker\Docker\resources\bin). That's
    why it's old.
  - Fix: run winget install Kubernetes.kubectl, reopen PowerShell, then run where.exe kubectl again. If Docker's copy is still listed first, the new one is
    installed but hidden behind it, so change the PATH order. Check with kubectl version; there should be no warning.
  - Skew warning: two copies of kubectl installed, System PATH beats User PATH, and the skew rule is ±1 minor version.
  - Connection refused: the kind cluster is a container that stops when Docker stops. Lesson: if Docker isn't running, the cluster isn't either.