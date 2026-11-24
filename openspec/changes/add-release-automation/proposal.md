## Why

Publishing Little Colors currently requires building, signing, and updating the F-Droid feed by hand. The same verified build should be available from GitHub Releases and F-Droid.

## What Changes

- Add a manually triggered GitHub Actions workflow for release builds and publishing.
- Produce a signed APK, matching source archive, and SHA-256 checksums for release runs.
- Update the existing developer-hosted F-Droid feed and publish a GitHub Release from the same artifacts.

## Capabilities

### New Capabilities

- `release-distribution` covers artifact builds and publication to the two distribution channels.

### Modified Capabilities

None.

## Impact

The workflow uses the existing APK and F-Droid index signing keys and publishes to the existing GitHub Pages source branch. The app and its Gradle configuration keep their existing build and signing behavior.
