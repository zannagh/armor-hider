# Self-Hosted Runner Setup

This documents the prerequisites for setting up a **Windows** self-hosted GitHub Actions
runner for this repository. The CI workflows automatically detect whether the runner is
available and fall back to GitHub-hosted `ubuntu-latest` runners if it is not.

## Prerequisites

- **Windows 10/11** (x64)
- **Git for Windows** with `C:\Program Files\Git\bin` in the **system** PATH
  (must come before `C:\Windows\System32` so Git Bash is resolved instead of WSL bash)

## Required Software

| Tool | Purpose | Install |
|------|---------|---------|
| **JDK 17** (Temurin) | Gradle toolchain for MC 1.20.x | `winget install EclipseAdoptium.Temurin.17.JDK` |
| **JDK 21** (Temurin) | Gradle toolchain for MC 1.21.x | `winget install EclipseAdoptium.Temurin.21.JDK` |
| **JDK 25** (Temurin) | Gradle toolchain for MC 26.x snapshots | `winget install EclipseAdoptium.Temurin.25.JDK` |
| **jq** | Used by workflow `run:` steps | `winget install jqlang.jq` |
| **GitHub CLI** | Optional, useful for diagnostics | `winget install GitHub.cli` |

> **Note:** `jq` and `gh` are installed per-user by default (winget/scoop). If the runner
> service runs as `NETWORK SERVICE`, copy the binaries to a directory on the **system** PATH
> (e.g., `C:\tools\bin`) and grant `NETWORK SERVICE` read+execute access:
> ```
> icacls "C:\tools\bin" /grant "NETWORK SERVICE:(OI)(CI)RX"
> ```

## Runner Installation

1. Download the latest runner from **Settings > Actions > Runners > New self-hosted runner**.

2. Configure with the name `armor-hider-runner`:
   ```powershell
   .\config.cmd --url https://github.com/zannagh/armor-hider `
     --token <REGISTRATION_TOKEN> `
     --name armor-hider-runner `
     --runasservice `
     --windowslogonaccount "NT AUTHORITY\NETWORK SERVICE"
   ```

3. Verify the service is running:
   ```
   sc.exe query "actions.runner.zannagh-armor-hider.armor-hider-runner"
   ```

## Warming the Gradle Cache

If the runner service runs as `NETWORK SERVICE`, its Gradle home is at
`C:\Windows\ServiceProfiles\NetworkService\.gradle`. To pre-warm it:

```powershell
$NS = "C:\Windows\ServiceProfiles\NetworkService"
Copy-Item -Recurse "$env:USERPROFILE\.gradle\wrapper" "$NS\.gradle\wrapper"
Copy-Item -Recurse "$env:USERPROFILE\.gradle\caches" "$NS\.gradle\caches"
icacls "$NS\.gradle" /grant "NETWORK SERVICE:(OI)(CI)F" /T
```

Subsequent workflow runs will further populate and update this cache automatically.

## Repository Secret

The `check-runner` reusable workflow requires a **RUNNER_TOKEN** repository secret:

- **Type:** Fine-grained PAT scoped to this repository
- **Permission:** Administration (read)
- **Purpose:** Queries `GET /repos/{owner}/{repo}/actions/runners` to check runner status
- **Fallback:** If the secret is missing (e.g., in forks), the workflow falls back to `ubuntu-latest`

## Git Bash vs WSL

Windows ships `C:\Windows\System32\bash.exe` (WSL). GitHub Actions `shell: bash` steps
will use whichever `bash` is found first in PATH. If WSL bash is resolved instead of
Git Bash, all `run:` steps will fail. Ensure `C:\Program Files\Git\bin` is **prepended**
to the system PATH:

```powershell
$path = [Environment]::GetEnvironmentVariable('Path', 'Machine')
if ($path -notlike '*Git\bin*') {
    [Environment]::SetEnvironmentVariable('Path', "C:\Program Files\Git\bin;$path", 'Machine')
}
```

Then restart the runner service to pick up the change.
