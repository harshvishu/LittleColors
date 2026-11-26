## Context

`LittleColors` uses local PKCS#12 keystores (`app.p12`, `repo.p12`, `upload.p12`), password files (`app-password`, `repo-password`, `upload-password`, `backup-recovery-passphrase`), and GitHub environment secrets (`APP_KEYSTORE_BASE64`, `FDROID_KEYSTORE_BASE64`, `PAGES_PUBLISH_TOKEN`). Public SHA-256 certificate fingerprints are intentionally documented in `docs/release-runbook.md`.

## Goals

- Prevent accidental commits or merges of signing keys, passwords, tokens, or `.env` files.
- Provide fast local feedback via `pre-commit` and authoritative enforcement in GitHub Actions.
- Avoid false positives on intentionally documented public SHA-256 certificate fingerprints.

## Decisions

- **Use Gitleaks as the primary secret scanner**: Both `.pre-commit-config.yaml` and `.github/workflows/security.yml` share `.gitleaks.toml` so local and CI checks behave identically.
- **Add a deterministic filename/path gate (`.github/scripts/check-sensitive-files.sh`)**: Secret content scanners do not catch binary `.p12`/`.jks` keystores or empty/short password files. Checking `git ls-files` against forbidden patterns catches `git add -f` and renamed key files.
- **Allowlist public certificate fingerprints**: Allowlist the documented public SHA-256 certificate fingerprints in `docs/release-runbook.md` inside `.gitleaks.toml`.
- **Keep `privacy-policy` release automation intact**: Trigger `security.yml` on `pull_request` and `push` to `main` (and feature branches), excluding `privacy-policy` which only receives automated F-Droid feed commits from `release.yml`.

## Risks / Trade-offs

- **Local hooks are opt-in**: Developers must run `pre-commit install` locally, and `--no-verify` can bypass them. CI enforcement in `security.yml` + GitHub branch rulesets on `main` close that gap.
- **GitHub UI settings**: Native Secret Scanning, Push Protection, and branch rulesets live in GitHub repository settings rather than Git; we will enable or verify them via `gh api` where permitted and document them in `docs/security.md`.
