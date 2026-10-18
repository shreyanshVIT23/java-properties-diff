#!/bin/sh
# Tests for scripts/github-release.sh with a fake gh that only records its arguments and answers
# from FAKE_TAG_SHA (empty: the tag is not on GitHub) and FAKE_RELEASE (none, draft or
# published). Nothing talks to GitHub.
#
# Usage (from the project directory): sh scripts/test-github-release.sh
set -u

script="$(cd "$(dirname "$0")" && pwd)/github-release.sh"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
calls="$work/calls"
out="$work/out"

cat > "$work/fake-gh" <<EOF
#!/bin/sh
echo "\$*" >> "$calls"
case "\$1 \$2" in
  "api repos/"*)
    [ -n "\${FAKE_TAG_SHA:-}" ] || { echo "gh: Not Found (HTTP 404)" >&2; exit 1; }
    echo "\$FAKE_TAG_SHA" ;;
  "release view")
    case "\${FAKE_RELEASE:-none}" in
      none) echo "release not found" >&2; exit 1 ;;
      draft) echo true ;;
      published) echo false ;;
    esac ;;
esac
exit 0
EOF
chmod +x "$work/fake-gh"

zip1="$work/propdiff-1.2.3-linux-x64.zip"
zip2="$work/propdiff-1.2.3-windows-x64.zip"
touch "$zip1" "$zip2"
commit=0123456789abcdef0123456789abcdef01234567
other=fedcba9876543210fedcba9876543210fedcba98

export PROPDIFF_GH="$work/fake-gh"
export GH_REPO=owner/repo
failures=0

# release <GH_TOKEN> <FAKE_TAG_SHA> <FAKE_RELEASE>: runs the script for tag v1.2.3, built from
# $commit, with fresh call records; sets $status
release() {
  : > "$calls"
  GH_TOKEN=$1 FAKE_TAG_SHA=$2 FAKE_RELEASE=$3 \
    sh "$script" v1.2.3 1.2.3 "$commit" "$zip1" "$zip2" > "$out" 2>&1
  status=$?
}

ok() { echo "ok   - $1"; }
fail() {
  echo "FAIL - $1"
  sed 's/^/       out: /' "$out"
  sed 's/^/       gh:  /' "$calls"
  failures=$((failures + 1))
}

# expect <exit 0|nonzero> <text in output or ""> <gh call pattern that must appear or ""> <description>
expect() {
  if [ "$1" = 0 ] && [ "$status" -ne 0 ]; then fail "$4: exit $status"; return; fi
  if [ "$1" != 0 ] && [ "$status" -eq 0 ]; then fail "$4: exit 0, expected a failure"; return; fi
  if [ -n "$2" ] && ! grep -qF -- "$2" "$out"; then fail "$4: output lacks '$2'"; return; fi
  if [ -n "$3" ] && ! grep -qF -- "$3" "$calls"; then fail "$4: gh was not called with '$3'"; return; fi
  ok "$4"
}

# changes_nothing <description>: no create or upload was called
changes_nothing() {
  if grep -qE '^release (create|upload)' "$calls"; then fail "$1"; else ok "$1"; fi
}

release "" "$commit" none
expect 1 "jenkinsgithubsetup.ps1" "" "without a token it fails with the setup hint"
if [ -s "$calls" ]; then fail "without a token gh is never called"; else ok "without a token gh is never called"; fi

release token "" none
expect 1 "git push origin v1.2.3" "api repos/owner/repo/commits/v1.2.3" "a tag missing on GitHub fails with the push hint"
changes_nothing "a tag missing on GitHub changes nothing"

release token "$other" none
expect 1 "$other" "" "a tag at another commit on GitHub fails and names both commits"
changes_nothing "a tag at another commit changes nothing"

release token "$commit" none
expect 0 "" "release create v1.2.3 $zip1 $zip2 --draft --verify-tag --title propdiff 1.2.3 --generate-notes" \
  "no release yet: creates a draft with every file"

release token "$commit" draft
expect 0 "" "release upload v1.2.3 $zip1 $zip2 --clobber" "an existing draft gets its assets replaced"

release token "$commit" published
expect 1 "already published" "" "a published release is refused"
changes_nothing "a published release is never changed"

if [ "$failures" -eq 0 ]; then
  echo "github release test passed"
else
  echo "github release test FAILED: $failures check(s)"
fi
exit "$failures"
