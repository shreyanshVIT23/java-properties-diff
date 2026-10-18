# Linux build agent reached over SSH (label "linux").
# https://github.com/jenkinsci/docker-ssh-agent
FROM jenkins/ssh-agent:debian-jdk21

# A full Temurin JDK 17 for the build: Temurin's jpackage avoids the "pure virtual method
# called" launcher bug of distro JDK 21 builds (JDK-8312488). The image's own Java only runs
# the Jenkins agent.
COPY --from=eclipse-temurin:17-jdk /opt/java/openjdk /opt/temurin-17

# binutils: objcopy, used by jlink/jpackage. unzip: scripts/smoke-test.sh. git: checkout.
RUN apt-get update \
    && apt-get install -y --no-install-recommends binutils unzip git \
    && rm -rf /var/lib/apt/lists/*

# Builds clone /repo, which is not owned by jenkins (Docker Desktop bind mount); without this
# git refuses it as "dubious ownership". System scope, so it applies to the jenkins user.
# https://git-scm.com/docs/git-config#Documentation/git-config.txt-safedirectory
RUN git config --system --add safe.directory /repo \
    && git config --system --add safe.directory /repo/.git

# Maven's local repository lives on a volume; create it so the volume is owned by jenkins
RUN mkdir -p /home/jenkins/.m2 && chown -R jenkins:jenkins /home/jenkins/.m2
