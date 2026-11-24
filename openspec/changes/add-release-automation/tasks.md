## 1. Build and publish

- [x] 1.1 Add an Actions build-only run and a gated publication job that signs and stages release artifacts.
- [x] 1.2 Extend and verify the existing F-Droid feed, then publish the matching GitHub Release. Preserve staged files on retry.

## 2. Setup and verification

- [x] 2.1 Document the Actions environment, credentials, first eligible version, and retry procedure.
- [x] 2.2 Validate the workflow, OpenSpec change, and release preparation without publishing a live release.
- [x] 2.3 Verify SDK setup and release artifact creation on the hosted Actions runner.
- [x] 2.4 Add a standalone dry-run check of the configured Pages token.
- [ ] 2.5 Run the token check on the default branch against the release environment.
