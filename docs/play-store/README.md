# Google Play release

## Main store listing

| Field | Value |
| --- | --- |
| App name | Little Colors |
| App or game | Game |
| Category | Educational |
| Short description | Offline coloring, letter tracing, and counting games for toddlers. |
| Privacy policy | https://harshvishu.github.io/LittleColors/privacy/ |
| Support email | harshvishu.apps@gmail.com |

**Full description**

Little Colors is an offline coloring and early learning game for toddlers and preschoolers.

Choose from 100 pictures and use a palette made for each one. In Assisted mode, touch and hold to trace or fill a picture. In Follow Along, move near the guide to practice tracing. In Freehand, draw and erase at your own pace.

Trace uppercase A–Z and numbers 1–10. Finish a letter to reveal a picture, or finish a number to reveal a group of objects to count. The app reads the words aloud.

Parents can turn music and sounds on or off and optionally use Android screen pinning. Little Colors works offline and has no ads or accounts.

Upload the [listing icon](icon.png) and the [feature graphic](feature-graphic.png). Add the screenshots from [phone](phone/) and [tablet](tablet/) to their respective sections. The captures show coloring, color selection, letter tracing, and counting.

## App content

| Play Console section | Answers for this build |
| --- | --- |
| Target audience | Ages 5 & Under |
| Data safety | No user data collected or shared. No accounts. Preferences stay on the device. |
| Ads | No ads |
| App access | All features are available without credentials. Select **Start play** and then **Play without pinning** if Android screen pinning is unavailable or declined. |
| Content rating | Complete the IARC questionnaire for the Educational game. The app contains pictures for coloring, tracing lessons, and counting activities. It has no purchases, user-generated sharing, or online interactions. |

## First internal test

In Play Console, find Little Colors under **All apps**, or create it as a free game. Go to **Testing → Internal testing → Create new release** and upload `app/build/outputs/bundle/release/LittleColors-1.10-play.aab`. Check that Play shows version 1.10 with version code 11 before saving the draft.

**English (United States) release notes**

> First internal test of Little Colors. Try 100 coloring pictures, uppercase letter tracing, counting from 1 to 10, and optional Android screen pinning. Activities work offline.

## Build and sign an upload bundle

Build with JDK 17 and Android SDK 36. To support updates across Google Play and the Little Colors F-Droid repository, supply the existing app signing key from `~/.android/littlecolors-distribution/app.p12` when setting up Play App Signing. The bundle upload uses the separate upload key below. Keep both keys and their passwords outside the repository.

```sh
./gradlew :app:bundleRelease :app:lintRelease
release_dir=app/build/outputs/bundle/release
signing_dir="$HOME/.android/littlecolors-play"
jarsigner -keystore "$signing_dir/upload.p12" \
  -storepass:file "$signing_dir/upload-password" \
  -signedjar "$release_dir/LittleColors-1.10-play.aab" \
  "$release_dir/app-release.aab" littlecolors-upload
jarsigner -verify "$release_dir/LittleColors-1.10-play.aab"
```
