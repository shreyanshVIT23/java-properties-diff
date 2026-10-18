#!/bin/sh
# Tests for scripts/git-hooks/reference-transaction, in a throwaway repository with a fake curl
# that only records its arguments. Nothing talks to the real Jenkins.
#
# Usage (from the project directory): sh scripts/git-hooks/test-reference-transaction.sh
set -u

hook="$(cd "$(dirname "$0")" && pwd)/reference-transaction"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
calls="$work/calls"
: > "$calls"

cat > "$work/fake-curl" <<EOF
#!/bin/sh
echo "\$*" >> "$calls"
EOF
chmod +x "$work/fake-curl"
printf 'user = "admin:test-token"\n' > "$work/token"

git init -q "$work/repo"
cd "$work/repo" || exit 1
git config user.email test@example.com
git config user.name test
cp "$hook" .git/hooks/reference-transaction
chmod +x .git/hooks/reference-transaction

export PROPDIFF_JENKINS_CURL="$work/fake-curl"
export PROPDIFF_JENKINS_TOKEN_FILE="$work/token"
export PROPDIFF_JENKINS_URL=http://jenkins.test

failures=0

count() {
  wc -l < "$calls" | tr -d ' '
}

# expect_more <calls before> <description>: the hook called curl again (it runs in the
# background, so wait up to 3 seconds)
expect_more() {
  for _ in 1 2 3 4 5 6; do
    [ "$(count)" -gt "$1" ] && { echo "ok   - $2"; return; }
    sleep 0.5
  done
  echo "FAIL - $2: no call to Jenkins"
  failures=$((failures + 1))
}

# expect_same <calls before> <description>: no new call, even after waiting
expect_same() {
  sleep 1.5
  if [ "$(count)" -eq "$1" ]; then
    echo "ok   - $2"
  else
    echo "FAIL - $2: unexpected call(s):"
    tail -n "+$(($1 + 1))" "$calls" | sed 's/^/       /'
    failures=$((failures + 1))
  fi
}

before=$(count)
git commit -q --allow-empty -m first
expect_more "$before" "a commit asks Jenkins to scan"

before=$(count)
git tag -a v1.0.0 -m "release"
expect_more "$before" "a tag asks Jenkins to scan"

before=$(count)
git branch feature
expect_more "$before" "a new branch asks Jenkins to scan"

before=$(count)
git update-ref refs/remotes/origin/main HEAD
expect_same "$before" "remote-tracking refs (git fetch) are ignored"

if grep -qF -- "http://jenkins.test/job/java-properties-diff/build?delay=0" "$calls" \
    && grep -qF -- "--config $work/token" "$calls"; then
  echo "ok   - calls the scan URL with the token as a curl config file"
else
  echo "FAIL - wrong curl arguments:"
  sed 's/^/       /' "$calls"
  failures=$((failures + 1))
fi

before=$(count)
if PROPDIFF_JENKINS_TOKEN_FILE="$work/missing" git commit -q --allow-empty -m second; then
  expect_same "$before" "without a token file nothing is called and git still succeeds"
else
  echo "FAIL - git commit failed without a token file"
  failures=$((failures + 1))
fi

before=$(count)
if PROPDIFF_JENKINS_CURL=/nonexistent/curl git commit -q --allow-empty -m third; then
  echo "ok   - a failing curl never breaks git"
else
  echo "FAIL - git commit failed when curl could not run"
  failures=$((failures + 1))
fi

if [ "$failures" -eq 0 ]; then
  echo "hook test passed"
else
  echo "hook test FAILED: $failures check(s)"
fi
exit "$failures"
