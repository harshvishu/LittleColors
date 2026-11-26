#!/usr/bin/env bash
set -euo pipefail

# Fail if any sensitive key, keystore, password, or environment file is tracked in Git.
forbidden_regex='(^|/)(\.env(\.[^/]+)?|keystore\.properties|google-services\.json|signing-backup/.*|.*-password|.*passphrase.*|.*\.(p12|jks|keystore|pem|key))$'

offenders=$(git ls-files | grep -iE "$forbidden_regex" | grep -vE '(^|/)\.env\.example$' || true)

if [[ -n "$offenders" ]]; then
  echo "ERROR: Sensitive file(s) must not be committed to the repository:"
  echo "$offenders"
  exit 1
fi

echo "OK: No forbidden sensitive files tracked."
