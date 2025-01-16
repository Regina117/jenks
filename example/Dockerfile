FROM openjdk:17-jdk-slim-bullseye

RUN apt-get update && apt-get -y upgrade

RUN apt-get -y install openssh-client ca-certificates curl maven

# Установка Docker
RUN apt-get update \
    && apt-get -y install apt-transport-https ca-certificates curl software-properties-common \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
    && chmod a+r /etc/apt/keyrings/docker.asc \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo \"$VERSION_CODENAME\") stable" > /et>    && apt-get update \
    && apt-get -y install docker-ce docker-ce-cli containerd.io

# Конфигурация Docker для использования insecure registry
RUN echo '{ "insecure-registries": ["158.160.129.106:8123"] }' > /etc/docker/daemon.json


RUN apt-get clean && rm -rf /var/lib/apt/lists/*