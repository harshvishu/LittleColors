#!/usr/bin/env bash
set -euo pipefail
export LC_ALL=C

unsigned_apk=$1
pages_dir=$2
release_dir=$3
fdroid_dir="$pages_dir/docs/fdroid"
repo="$fdroid_dir/repo"
package_id=family.littlecolors
app_fingerprint=${APP_CERT_SHA256:-ef00318a21655b29dbf05677493274fbcf803d8d089ca4ef0fd77587f353a5ef}
repo_fingerprint=${FDROID_CERT_SHA256:-b39625a96f1ebf888ce98ea1dc4772c1f53e0bf006f2060f4138f2156dbf7fc9}

fail() {
  printf '::error::%s\n' "$1" >&2
  exit 1
}

file_sha256() {
  shasum -a 256 "$1" | cut -d ' ' -f 1
}

verify_index() {
  local index_dir=$1 signer index_hash
  jarsigner -verify "$index_dir/entry.jar" > /dev/null
  signer=$(keytool -printcert -jarfile "$index_dir/entry.jar" |
    awk '/SHA256:/{print tolower($2)}' | tr -d ':')
  [[ $signer == "$repo_fingerprint" ]] || fail 'F-Droid index signer does not match the existing feed.'
  index_hash=$(unzip -p "$index_dir/entry.jar" entry.json | jq -er '.index.sha256')
  [[ $(file_sha256 "$index_dir/index-v2.json") == "$index_hash" ]] || fail 'F-Droid index hash is invalid.'
}

verify_apk() {
  local apk=$1 signer package_line
  signer=$(apksigner verify --print-certs "$apk" |
    awk '/certificate SHA-256 digest:/{print tolower($NF)}')
  [[ $signer == "$app_fingerprint" ]] || fail 'APK signer does not match the existing distribution key.'
  package_line=$(aapt2 dump badging "$apk" | sed -n '/^package: /p')
  [[ $package_line == "$unsigned_package" ]] || fail 'Signed APK package metadata does not match the release build.'
}

unsigned_package=$(aapt2 dump badging "$unsigned_apk" | sed -n '/^package: /p')
package_pattern="^package: name='family[.]littlecolors' versionCode='([1-9][0-9]*)' versionName='([0-9]+[.][0-9]+([.][0-9]+)?)'"
[[ $unsigned_package =~ $package_pattern ]] || fail 'Use application ID family.littlecolors and a numeric release version such as 1.11.'
version_code=${BASH_REMATCH[1]}
version_name=${BASH_REMATCH[2]}
tag=v$version_name
apk_name="${package_id}_${version_code}.apk"
source_name="${package_id}_${version_code}_src.tar.gz"
apk_path="docs/fdroid/repo/$apk_name"

verify_index "$repo"
latest_code=$(jq -er --arg id "$package_id" '[.packages[$id].versions[].manifest.versionCode] | max' "$repo/index-v2.json")
if (( version_code < latest_code )); then
  fail "Version code $version_code is older than F-Droid version code $latest_code."
fi

pages_changed=false
if (( version_code == latest_code )); then
  previous_source=$(git -C "$pages_dir" log -1 --format='%(trailers:key=Source-Commit,valueonly)' -- "$apk_path")
  [[ $previous_source == "$GITHUB_SHA" ]] || fail "F-Droid version code $version_code is already published. Increase versionCode for the next release."
  verify_apk "$repo/$apk_name"
else
  if jq -e --arg id "$package_id" --arg version "$version_name" \
    'any(.packages[$id].versions[]; .manifest.versionName == $version)' "$repo/index-v2.json" > /dev/null; then
    fail "F-Droid version name $version_name is already published. Choose a new versionName."
  fi
  [[ ! -e "$repo/$apk_name" && ! -e "$repo/$source_name" ]] || fail 'The F-Droid branch already contains files for this version code.'
  : "${APP_KEYSTORE_BASE64:?Set APP_KEYSTORE_BASE64 in the release environment}"
  : "${APP_KEYSTORE_PASSWORD:?Set APP_KEYSTORE_PASSWORD in the release environment}"
  : "${FDROID_KEYSTORE_BASE64:?Set FDROID_KEYSTORE_BASE64 in the release environment}"
  : "${FDROID_KEYSTORE_PASSWORD:?Set FDROID_KEYSTORE_PASSWORD in the release environment}"
  work_dir=$(mktemp -d "$RUNNER_TEMP/little-colors-signing.XXXXXX")
  trap 'rm -rf -- "$work_dir"' EXIT
  umask 077
  printf '%s' "$APP_KEYSTORE_BASE64" | base64 --decode > "$work_dir/app.p12"
  printf '%s' "$FDROID_KEYSTORE_BASE64" | base64 --decode > "$work_dir/repo.p12"
  unset APP_KEYSTORE_BASE64 FDROID_KEYSTORE_BASE64

  zipalign -P 16 -f 4 "$unsigned_apk" "$work_dir/aligned.apk"
  apksigner sign --ks "$work_dir/app.p12" --ks-type PKCS12 --ks-key-alias littlecolors-app \
    --ks-pass env:APP_KEYSTORE_PASSWORD --out "$work_dir/$apk_name" "$work_dir/aligned.apk"
  unset APP_KEYSTORE_PASSWORD
  zipalign -P 16 -c 4 "$work_dir/$apk_name"
  verify_apk "$work_dir/$apk_name"

  git archive --format=tar --prefix="LittleColors-$version_name/" "$GITHUB_SHA" |
    gzip -n > "$work_dir/$source_name"
  cp -R "$repo" "$work_dir/repo"
  cp "$repo/icons/icon.png" "$work_dir/icon.png"
  cp "$work_dir/$apk_name" "$work_dir/$source_name" "$work_dir/repo/"
  cp -R .github/fdroid/. "$work_dir/"
  (cd "$work_dir" && fdroid update --use-date-from-apk)
  unset FDROID_KEYSTORE_PASSWORD
  verify_index "$work_dir/repo"

  # Check the signed index includes both files with the hashes we will publish.
  jq -e --arg id "$package_id" --arg apk "$apk_name" --arg source "$source_name" \
    --arg apk_hash "$(file_sha256 "$work_dir/$apk_name")" \
    --arg source_hash "$(file_sha256 "$work_dir/$source_name")" \
    --argjson code "$version_code" \
    'any(.packages[$id].versions[]; .manifest.versionCode == $code
      and .file.name == ("/" + $apk) and .file.sha256 == $apk_hash
      and .src.name == ("/" + $source) and .src.sha256 == $source_hash)' \
    "$work_dir/repo/index-v2.json" > /dev/null || fail 'The generated F-Droid index is missing the new APK or source archive.'

  rsync -a --delete "$work_dir/repo/" "$repo/"
  cp LICENSE.md "$fdroid_dir/LICENSE.md"
  cp NOTICE "$fdroid_dir/NOTICE.txt"
  pages_changed=true
fi

mkdir -p "$release_dir"
cp "$repo/$apk_name" "$repo/$source_name" "$release_dir/"
(cd "$release_dir" && shasum -a 256 "$apk_name" "$source_name" > SHA256SUMS)
{
  printf 'tag=%s\n' "$tag"
  printf 'apk=%s\n' "$apk_name"
  printf 'pages_changed=%s\n' "$pages_changed"
} >> "$GITHUB_OUTPUT"
