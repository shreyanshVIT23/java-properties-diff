mkdir -p jenkins/.secrets
ssh-keygen -t ed25519 -N '' -C propdiff-jenkins-agent -f jenkins/.secrets/agent_key
read -s -P 'Jenkins admin password: ' pw
printf 'JENKINS_ADMIN_PASSWORD=%s\nAGENT_SSH_PUBKEY=%s\n' $pw (cat jenkins/.secrets/agent_key.pub) > jenkins/.env
set -e pw
chmod 600 jenkins/.env
