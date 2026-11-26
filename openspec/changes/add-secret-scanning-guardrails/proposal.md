## Why

Little Colors is a public repository whose release pipeline depends on sensitive Android signing keystores, password files, and GitHub deployment tokens. Without automated secret scanning and filename guardrails, an accidental `git add -f`, pasted token, or base64 keystore in a commit or pull request could expose distribution credentials.

## What Changes

- **`.gitignore` hardening**: Ignore additional sensitive file patterns (`*.pem`, `*.key`, `*-password`, `*passphrase*`, `.env`, `.env.*` excluding `.env.example`, `google-services.json`).
- **Gitleaks configuration (`.gitleaks.toml`)**: Extend default secret-scanning rules with custom rules for Little Colors signing secrets (base64 keystores, password assignments) while allowlisting documented public certificate fingerprints.
- **Tracked sensitive-file check (`.github/scripts/check-sensitive-files.sh`)**: Block any commit or PR that tracks forbidden keystore, key, password, or environment files.
- **GitHub Actions security workflow (`.github/workflows/security.yml`)**: Run `gitleaks` and the sensitive-file check on every push and pull request.
- **Local pre-commit config (`.pre-commit-config.yaml`) & documentation (`docs/security.md`)**: Enable local pre-commit secret scanning and document GitHub Push Protection and branch ruleset configuration.

## Capabilities

### New Capabilities
- `repo-security`: Automated local and CI secret scanning, sensitive-file blocking, and repository security documentation.

### Modified Capabilities
- None

## Impact

- `.gitignore`
- `.gitleaks.toml`
- `.pre-commit-config.yaml`
- `.github/scripts/check-sensitive-files.sh`
- `.github/workflows/security.yml`
- `docs/security.md`
