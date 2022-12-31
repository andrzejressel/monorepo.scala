# See here for image contents: https://github.com/microsoft/vscode-dev-containers/tree/v0.238.0/containers/java-8/.devcontainer/base.Dockerfile

# [Choice] Debian OS version (use bullseye on local arm64/Apple Silicon): buster, bullseye
ARG VERSION=""
ARG VARIANT=""
ARG JAVA_VERSION=""
ARG SBT_VERSION=""
FROM mcr.microsoft.com/vscode/devcontainers/java:${VERSION}-${JAVA_VERSION}-${VARIANT}

RUN su vscode -c "umask 0002 && . /usr/local/sdkman/bin/sdkman-init.sh && sdk install sbt ${SBT_VERSION} && sdk install scalacli"

# [Optional] Uncomment this section to install additional OS packages.
# RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    # && apt-get -y install --no-install-recommends chromium patch bubblewrap

# RUN curl "https://github.com/ocaml/opam/releases/download/2.1.3/opam-2.1.3-arm64-linux" -L -o /tmp/opam

# RUN install /tmp/opam /usr/local/bin/opam

# RUN su vscode -c "opam init && opam install atdgen" 

# [Optional] Uncomment this line to install global node packages.
# RUN su vscode -c "source /usr/local/share/nvm/nvm.sh && npm install -g <your-package-here>" 2>&1