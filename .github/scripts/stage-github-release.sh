#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '::error::%s\n' "$1" >&2
  exit 1
}

tag_ref="refs/tags/$RELEASE_TAG"
tag_commit=$(git ls-remote origin "$tag_ref" "$tag_ref^{}" |
  awk 'NR == 1 {commit = $1} /\^\{\}$/ {commit = $1} END {print commit}')
[[ -z $tag_commit || $tag_commit == "$GITHUB_SHA" ]] || fail "$RELEASE_TAG already points to a different commit. Choose a new versionName."

release=$(gh api --paginate "repos/$GITHUB_REPOSITORY/releases" |
  jq -cs --arg tag "$RELEASE_TAG" '[.[][] | select(.tag_name == $tag)] | first // null')

if [[ $release == null ]]; then
  gh release create "$RELEASE_TAG" --target "$GITHUB_SHA" --draft --generate-notes --title "Little Colors $RELEASE_TAG"
elif [[ $(jq -r '.draft' <<< "$release") == true ]]; then
  [[ $tag_commit == "$GITHUB_SHA" || $(jq -r '.target_commitish' <<< "$release") == "$GITHUB_SHA" ]] ||
    fail "$RELEASE_TAG has a draft for a different source commit."
else
  [[ $tag_commit == "$GITHUB_SHA" ]] || fail "$RELEASE_TAG is published without the expected tag."
  for asset in release-files/*; do
    name=${asset##*/}
    digest="sha256:$(shasum -a 256 "$asset" | cut -d ' ' -f 1)"
    jq -e --arg name "$name" --arg digest "$digest" \
      'any(.assets[]; .name == $name and .digest == $digest)' <<< "$release" > /dev/null ||
      fail "$RELEASE_TAG is already published with different assets."
  done
  printf 'published=true\n' >> "$GITHUB_OUTPUT"
  exit 0
fi

gh release upload "$RELEASE_TAG" release-files/* --clobber
printf 'published=false\n' >> "$GITHUB_OUTPUT"
