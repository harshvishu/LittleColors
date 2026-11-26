# Repository Security & Secret Guardrails

Little Colors protects signing keys, passwords, and deployment tokens across four layers.

## 1. Local pre-commit hooks (before commit)

Install `pre-commit` once per workstation to block secrets and sensitive files before they are committed:

```sh
brew install pre-commit gitleaks
pre-commit install
```

This runs:
- `gitleaks` using [.gitleaks.toml](../.gitleaks.toml)
- `detect-private-key`
- [.github/scripts/check-sensitive-files.sh](../.github/scripts/check-sensitive-files.sh) to block `.p12`, `.jks`, `.keystore`, `.pem`, `.key`, `*-password`, `*passphrase*`, `keystore.properties`, and `.env` files even if staged with `git add -f`.

## 2. GitHub Secret Scanning & Push Protection

In GitHub **Settings → Code security**:
- Enable **Secret scanning**
- Enable **Push protection** (blocks known tokens and private key headers at `git push`)

## 3. Pull Request & Push CI (`.github/workflows/security.yml`)

Every pull request and push (excluding automated F-Droid feed pushes to `privacy-policy`) runs:
- `.github/scripts/check-sensitive-files.sh`
- `gitleaks/gitleaks-action@v2` using [.gitleaks.toml](../.gitleaks.toml)

## 4. Branch Protection / Rulesets on `main`

In GitHub **Settings → Rules → Rulesets** (for `main`):
- Require a pull request before merging
- Require status checks to pass: `Secret & sensitive-file scan`
- Block force pushes and deletions
