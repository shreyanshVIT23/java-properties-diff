# Jenkins controller with the plugins preinstalled; configured entirely by casc.yaml.
# https://github.com/jenkinsci/docker#preinstalling-plugins
FROM jenkins/jenkins:lts-jdk21

COPY --chown=jenkins:jenkins plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli -f /usr/share/jenkins/ref/plugins.txt

# The repository mounted at /repo is not owned by jenkins (Docker Desktop bind mount), so git
# refuses it as "dubious ownership" and branch indexing finds nothing. Trust only that mount.
# https://git-scm.com/docs/git-config#Documentation/git-config.txt-safedirectory
USER root
RUN git config --system --add safe.directory /repo \
    && git config --system --add safe.directory /repo/.git
USER jenkins
