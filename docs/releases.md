# Release Little Colors

[Release Little Colors](https://github.com/harshvishu/LittleColors/actions/workflows/release.yml) builds and lints the Android release variant. Its **Publish** option ships the signed APK and matching source archive to [GitHub Releases](https://github.com/harshvishu/LittleColors/releases) and the [Little Colors F-Droid feed](https://harshvishu.github.io/LittleColors/fdroid/repo/).

## Configure GitHub once

In **Settings → Environments → release**, confirm only `main` can deploy. Add these secrets to that environment. Base64-encode each keystore as a single line and use the corresponding password file's contents as the password secret.

| Secret | Value |
| --- | --- |
| `APP_KEYSTORE_BASE64` | Existing `~/.android/littlecolors-distribution/app.p12` |
| `APP_KEYSTORE_PASSWORD` | `~/.android/littlecolors-distribution/app-password` |
| `FDROID_KEYSTORE_BASE64` | Existing `~/.android/littlecolors-distribution/repo.p12` |
| `FDROID_KEYSTORE_PASSWORD` | `~/.android/littlecolors-distribution/repo-password` |
| `PAGES_PUBLISH_TOKEN` | Fine-grained token for `harshvishu/LittleColors` with **Contents: Read and write** |

From a trusted machine with `gh` authenticated as a repository admin, use these commands to enter the keystores and passwords without displaying them. Enter the Pages token at the prompt or in the environment settings. [GitHub Actions secrets](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets) · [Fine-grained access tokens](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)

```bash
signing_dir="$HOME/.android/littlecolors-distribution"
repo=harshvishu/LittleColors
base64 < "$signing_dir/app.p12" | tr -d '\r\n' | gh secret set APP_KEYSTORE_BASE64 --repo "$repo" --env release
tr -d '\r\n' < "$signing_dir/app-password" | gh secret set APP_KEYSTORE_PASSWORD --repo "$repo" --env release
base64 < "$signing_dir/repo.p12" | tr -d '\r\n' | gh secret set FDROID_KEYSTORE_BASE64 --repo "$repo" --env release
tr -d '\r\n' < "$signing_dir/repo-password" | gh secret set FDROID_KEYSTORE_PASSWORD --repo "$repo" --env release
gh secret set PAGES_PUBLISH_TOKEN --repo "$repo" --env release
```

Keep **Settings → Pages** on **Deploy from a branch**, using `privacy-policy` and `/docs`. This serves the privacy policy and the existing F-Droid feed. The repository-scoped Pages credential triggers deployment when the workflow pushes the feed update. [GitHub Pages publishing sources](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site)

## Build and publish

1. Set a new numeric `versionName` such as `1.11` and increase `versionCode` in [`app/build.gradle`](../app/build.gradle). The F-Droid feed already has version `1.10`, code `11`. Merge the changes into `main`.
2. Open **Actions → Release Little Colors → Run workflow**. Leave **Publish** unchecked for an unsigned APK and lint report in the `release-build` Actions artifact.
3. Run it from `main` with **Publish** checked when ready to release. The run creates a `LittleColors-v<versionName>` artifact with the signed APK, source archive, and `SHA256SUMS`. The same files appear on the corresponding `v<versionName>` GitHub Release after the new F-Droid index and APK are live.

If publishing stops after the F-Droid push, rerun the failed job from the original run before publishing another version. It reuses the staged APK and source archive and can finish the draft GitHub Release. If the build artifact has expired, rerun all jobs from that run. Use a new version for a new build.

Google Play bundles use the [separate Play upload procedure](play-store/README.md#build-and-sign-an-upload-bundle).
