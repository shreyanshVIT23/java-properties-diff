#!/bin/sh
# Integration test: the jenkins user in both containers can read the repository mounted at /repo.
# Docker Desktop bind mounts are not owned by jenkins (uid 1000), so without a safe.directory
# exception git stops with "detected dubious ownership" and branch indexing finds nothing.
#
# Usage (from java-properties-diff/jenkins, with the stack running): sh test-repo-access.sh
set -u

# Git Bash on Windows would rewrite /tmp into a Windows path before docker sees it
export MSYS_NO_PATHCONV=1

failures=0

# check <service>: as the jenkins user, ls-remote the mounted repository and expect a branch
check() {
  if out=$(docker compose exec -T -u jenkins -w /tmp "$1" git ls-remote --heads -- file:///repo 2>&1) \
      && printf '%s\n' "$out" | grep -q 'refs/heads/'; then
    echo "ok   - $1: jenkins can list the branches of /repo"
  else
    echo "FAIL - $1: jenkins cannot read /repo:"
    printf '%s\n' "$out" | sed 's/^/       /'
    failures=$((failures + 1))
  fi
}

check controller
check agent

if [ "$failures" -eq 0 ]; then
  echo "repo access test passed"
else
  echo "repo access test FAILED: $failures check(s)"
fi
exit "$failures"
