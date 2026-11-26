## 1. Local & Shared Guardrails

- [x] 1.1 Harden `.gitignore` with `*.pem`, `*.key`, `*-password`, `*passphrase*`, `.env`, `.env.*` (except `!.env.example`), and `google-services.json`
- [x] 1.2 Create `.gitleaks.toml` extending default rules, adding Little Colors secret rules, and allowlisting documented public certificate fingerprints
- [x] 1.3 Create `.github/scripts/check-sensitive-files.sh` (executable) to reject forbidden tracked files
- [x] 1.4 Create `.pre-commit-config.yaml` with `gitleaks`, `detect-private-key`, and the sensitive-file check

## 2. CI Workflow & Documentation

- [x] 2.1 Create `.github/workflows/security.yml` running `gitleaks` and `check-sensitive-files.sh` on pushes and pull requests
- [x] 2.2 Create `docs/security.md` documenting local `pre-commit` setup, GitHub Push Protection, and branch rulesets

## 3. Verification

- [x] 3.1 Validate OpenSpec change strictly, run `.github/scripts/check-sensitive-files.sh`, and verify GitHub security settings via `gh api`
