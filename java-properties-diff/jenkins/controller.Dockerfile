# Jenkins controller with the plugins preinstalled; configured entirely by casc.yaml.
# https://github.com/jenkinsci/docker#preinstalling-plugins
FROM jenkins/jenkins:lts-jdk21

COPY --chown=jenkins:jenkins plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli -f /usr/share/jenkins/ref/plugins.txt
