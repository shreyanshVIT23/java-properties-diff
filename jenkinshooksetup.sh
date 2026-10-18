read -s -P 'Jenkins API token: ' t
printf 'user = "admin:%s"\n' $t >jenkins/.secrets/api-token
set -e t
chmod 600 jenkins/.secrets/api-token
chmod +x scripts/git-hooks/reference-transaction scripts/git-hooks/test-reference-transaction.sh
ln -s ../../java-properties-diff/scripts/git-hooks/reference-transaction ../.git/hooks/reference-transaction
