## ADDED Requirements

### Requirement: Release build artifacts

The release workflow SHALL assemble and lint the release variant and provide a build-only mode that needs no signing secrets.

#### Scenario: Build only

- **WHEN** a maintainer runs the workflow with publishing disabled
- **THEN** the workflow uploads the unsigned release APK and lint report to the Actions run without publishing either channel

### Requirement: Publish to both distribution channels

The workflow SHALL sign the APK with Little Colors' existing distribution key and publish the APK and matching source archive to both GitHub Releases and the developer-hosted F-Droid feed. It SHALL upload those files and SHA-256 checksums to the Actions run and attach them to the GitHub Release.

#### Scenario: Publish an eligible release

- **WHEN** a maintainer enables publishing on the default branch for a build with a new version name and a higher version code
- **THEN** the existing F-Droid URL serves the signed package and updated index, and a GitHub Release tagged with the app version links to that source commit and provides the same package and source archive

#### Scenario: Reject an incompatible release

- **WHEN** the APK signing identity differs, the F-Droid repository signing identity differs, or the version code belongs to a different build already on the feed
- **THEN** the workflow fails before changing the live feed

#### Scenario: Retry a partial publication

- **WHEN** publishing is retried from the same commit after updating the F-Droid branch
- **THEN** the workflow reuses the prepared files and can finish the GitHub Release without changing the published APK
