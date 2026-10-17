#!/bin/sh
# Smoke test for the packaged propdiff release zip, as a user would get it.
#
# Usage (from the project directory): scripts/smoke-test.sh target/propdiff-<version>-linux-x64.zip
#
# Unzips into a temporary directory, then runs the bundled binary with PATH emptied so it
# cannot fall back to an installed Java. Exits non-zero if any check fails.
set -u

zip=${1:?usage: scripts/smoke-test.sh <propdiff zip>}
fixtures=src/test/resources/fixtures
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

unzip -q "$zip" -d "$work" || { echo "FAIL - cannot unzip $zip"; exit 1; }
bin="$work/propdiff/bin/propdiff"
out="$work/out.txt"
err="$work/err.txt"
failures=0

# check <expected exit code> <description> <propdiff arguments...>
# Exit codes 0 and 1 are successful runs, so they must print nothing on stderr: that catches
# native launcher noise such as "pure virtual method called" (JDK-8274856).
check() {
  expected=$1
  description=$2
  shift 2
  env PATH=/nonexistent "$bin" "$@" > "$out" 2> "$err"
  actual=$?
  if [ "$actual" -ne "$expected" ]; then
    echo "FAIL - $description: exit $actual, expected $expected"
    sed 's/^/       /' "$out" "$err"
    failures=$((failures + 1))
  elif [ "$expected" -le 1 ] && [ -s "$err" ]; then
    echo "FAIL - $description: unexpected output on stderr"
    sed 's/^/       /' "$err"
    failures=$((failures + 1))
  else
    echo "ok   - $description"
  fi
  # Later contains() checks look at both streams, as a user would see them
  cat "$err" >> "$out"
}

# contains <text> <description>: the output of the previous check contains the text
contains() {
  if grep -qF -- "$1" "$out"; then
    echo "ok   - $2"
  else
    echo "FAIL - $2: output lacks '$1'"
    failures=$((failures + 1))
  fi
}

[ -x "$bin" ] || { echo "FAIL - $bin is missing or not executable"; exit 1; }

check 0 "--version runs on the bundled runtime (no Java on PATH)" --version
contains "propdiff" "--version names the program"

check 1 "differences exit with 1" --no-pager --color never \
  "$fixtures/left.properties" "$fixtures/right.properties"
contains "changed (2)" "unified output lists the changed keys"
contains "   3 ⋮     │ - app.version=1.0" "line numbers and UTF-8 box characters survive packaging"

check 1 "side-by-side works" --no-pager --color never -s --width 80 \
  "$fixtures/left.properties" "$fixtures/right.properties"

check 0 "identical files exit with 0" --no-pager \
  "$fixtures/left.properties" "$fixtures/left.properties"
contains "no differences" "identical files say no differences"

check 2 "a missing file exits with 2" --no-pager "$fixtures/left.properties" missing.properties
check 2 "an unknown option exits with 2" --bogus

# The pager starts processes (sh, less), and the runtime needs an executable jspawnhelper for
# that. Output here is redirected, so the pager never runs; check the file mode instead.
[ -x "$work/propdiff/lib/runtime/lib/jspawnhelper" ] \
  && echo "ok   - jspawnhelper kept its executable bit" \
  || { echo "FAIL - jspawnhelper is not executable"; failures=$((failures + 1)); }

if [ "$failures" -eq 0 ]; then
  echo "smoke test passed"
else
  echo "smoke test FAILED: $failures check(s)"
fi
exit "$failures"
