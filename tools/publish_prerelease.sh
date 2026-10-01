#!/usr/bin/env bash
# Creates or updates a GitHub pre-release whose tag points at $GITHUB_SHA, uploading the given
# files (replacing older ones with the same name). Used by the CI workflows.
#
# Usage: tools/publish_prerelease.sh TAG TITLE NOTES FILE...
#
# Updating in place avoids a GitHub race: deleting a tag and immediately recreating a release
# with the same name can leave the new release as an untagged draft whose assets 404.
set -euo pipefail

TAG=$1
TITLE=$2
NOTES=$3
shift 3

state=$(gh release view "$TAG" --json isDraft --jq '.isDraft' 2>/dev/null || echo missing)
if [ "$state" = "true" ]; then
  gh release delete "$TAG" --yes
  state=missing
fi

if [ "$state" = "missing" ]; then
  gh release create "$TAG" "$@" --prerelease --target "$GITHUB_SHA" --title "$TITLE" --notes "$NOTES"
else
  if gh api "repos/$GITHUB_REPOSITORY/git/refs/tags/$TAG" >/dev/null 2>&1; then
    gh api -X PATCH "repos/$GITHUB_REPOSITORY/git/refs/tags/$TAG" -f sha="$GITHUB_SHA" -F force=true >/dev/null
  else
    gh api -X POST "repos/$GITHUB_REPOSITORY/git/refs" -f ref="refs/tags/$TAG" -f sha="$GITHUB_SHA" >/dev/null
  fi
  gh release upload "$TAG" "$@" --clobber
  gh release edit "$TAG" --prerelease --title "$TITLE" --notes "$NOTES"
fi

gh release view "$TAG" --json url,isDraft,tagName --jq '"\(.url) draft=\(.isDraft)"'
