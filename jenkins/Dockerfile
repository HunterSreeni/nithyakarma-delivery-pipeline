FROM jenkins/jenkins:2.580.1-lts-jdk21
USER root
# CLIs only: they drive Docker Desktop and kind through the mounted docker.sock
COPY --from=docker:29.8.2-cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker:29.8.2-cli /usr/local/libexec/docker/cli-plugins/docker-buildx /usr/local/libexec/docker/cli-plugins/docker-buildx
COPY --from=registry.k8s.io/kubectl:v1.37.1 /bin/kubectl /usr/local/bin/kubectl
RUN curl -fsSLo /usr/local/bin/kind https://kind.sigs.k8s.io/dl/v0.33.0/kind-linux-amd64 && chmod +x /usr/local/bin/kind
USER jenkins
# plugins baked in = reproducible Jenkins, no clicking in the wizard
RUN jenkins-plugin-cli --plugins workflow-aggregator git docker-workflow junit htmlpublisher pipeline-stage-view