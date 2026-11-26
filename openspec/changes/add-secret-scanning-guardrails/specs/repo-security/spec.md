## ADDED Requirements

### Requirement: Sensitive file and secret scanning guardrails

The repository SHALL block tracked sensitive files (keystores, private keys, password/passphrase files, and `.env` files) and scan commits and pull requests for leaked secrets using a shared configuration.

#### Scenario: Forbidden keystore or password file is tracked

- **WHEN** a commit or pull request includes a tracked file matching forbidden key, keystore, password, or `.env` patterns
- **THEN** the sensitive-file check fails and reports the offending path

#### Scenario: Secret or token is introduced in a diff

- **WHEN** a commit or pull request introduces a known token, private key header, or Little Colors secret assignment
- **THEN** the secret scanner fails in CI (and locally when pre-commit hooks are installed)

#### Scenario: Public certificate fingerprints in documentation

- **WHEN** the secret scanner runs against `docs/release-runbook.md` containing the documented public SHA-256 certificate fingerprints
- **THEN** the scan passes without false-positive failures
