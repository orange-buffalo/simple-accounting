FROM ubuntu:20.04

RUN apt update \
    && apt-get install software-properties-common -y \
    && apt-add-repository ppa:git-core/ppa -y \
    && apt install -y jq openjdk-11-jdk git wget curl \
    && curl -s https://deb.nodesource.com/setup_16.x | bash - \
    && apt install -y nodejs \
    && apt remove software-properties-common -y \
    && apt autoremove -y \
    && apt autoclean

COPY ci.sh /usr/bin/ci

RUN chmod +x /usr/bin/ci

ENTRYPOINT ["/usr/bin/ci"]
