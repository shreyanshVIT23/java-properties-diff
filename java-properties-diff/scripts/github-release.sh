#!/bin/sh
# Uploads release files to a DRAFT GitHub Release for a tag. Run by the Jenkinsfile's Release
# stage on tag builds; you then review and publish the draft on GitHub.
#
# Usage: sh scripts/github-release.sh <tag> <version> <commit> <file>...
# Needs GH_TOKEN (fine-grained token, Contents: read/write) and GH_REPO (owner/repo).
#
# - The tag must already be on GitHub (you push it) and point to <commit>, the commit Jenkins
#   built; the script never creates tags.
# - No release yet: creates a draft. A draft: replaces its assets. Published: refuses.
# https://cli.github.com/manual/gh_release_create
set -u

[ "$#" -ge 4 ] || { echo "usage: $0 <tag> <version> <commit> <file>..." >&2; exit 2; }
tag=$1
version=$2
commit=$3
shift 3
gh=${PROPDIFF_GH:-gh}

if [ -z "${GH_TOKEN:-}" ]; then
  echo "No GitHub token. Create one (Contents: read and write on ${GH_REPO:-the repository})," >&2
  echo "run jenkinsgithubsetup.ps1, then recreate Jenkins: docker compose up -d --build" >&2
  exit 1
fi

# The uploaded files were built from $commit, so the tag on GitHub must point there too
if ! remote=$("$gh" api "repos/$GH_REPO/commits/$tag" --jq .sha); then
  echo "Tag $tag is not on GitHub ($GH_REPO). Push it first: git push origin $tag" >&2
  exit 1
fi
if [ "$remote" != "$commit" ]; then
  echo "Tag $tag on GitHub points to $remote, but Jenkins built $commit." >&2
  echo "Push the same tag (git push origin $tag) or move it, then build again." >&2
  exit 1
fi

# "true" for a draft, "false" for a published release; fails if there is no release
if draft=$("$gh" release view "$tag" --json isDraft --jq .isDraft 2>/dev/null); then
  if [ "$draft" = true ]; then
    echo "Replacing the assets of the draft release $tag"
    "$gh" release upload "$tag" "$@" --clobber || exit 1
  else
    echo "Release $tag is already published; it is never changed. Use a new tag." >&2
    exit 1
  fi
else
  echo "Creating the draft release $tag"
  # --verify-tag: fail rather than let gh create a missing tag
  "$gh" release create "$tag" "$@" --draft --verify-tag --title "propdiff $version" \
    --generate-notes || exit 1
fi
echo "Draft release $tag is ready: https://github.com/$GH_REPO/releases (review, then publish)"
