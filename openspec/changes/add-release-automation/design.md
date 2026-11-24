## Context

GitHub Pages serves `privacy-policy:/docs`, which contains the privacy page and the existing F-Droid feed. The feed has separate app and repository signing certificates. Version 1.10 (code 11) is already live on that feed.

## Goals

- Give maintainers a build-only run and an explicit publish option from the default branch.
- Distribute the same signed APK and corresponding source archive in GitHub Releases and the existing F-Droid feed.

## Decisions

- Build and lint the release variant with the Gradle wrapper, JDK 17, and SDK 36. Sign the release APK on the runner and verify its package, version, and signer. Store the existing keys in an Actions environment restricted to the default branch.
- Keep GitHub Pages on its current source. Use a repository-scoped publish credential for the Pages branch push because GitHub skips branch-based Pages builds for pushes authenticated with `GITHUB_TOKEN`. Use the job token for GitHub Releases.
- Extend the existing feed with fdroidserver and keep its APK history, signing fingerprint, assets, and privacy page. Reject a reused or lower version code. A retry for the same source commit can use the previously prepared APK and source archive from the Pages branch.
- Verify the public F-Droid entry and APK before publishing the GitHub Release. Use the APK version name as the release tag and the build commit as the tag target.

## Risks / Trade-offs

- Publishing spans two services. A failure after the Pages push can leave the F-Droid update live while the GitHub Release is pending. Rerun the workflow from the original commit to finish publishing the same files.
- The first release through the workflow needs a version code greater than 11 and the existing signing keys and Pages push credential configured in GitHub.
