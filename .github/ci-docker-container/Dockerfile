FROM ubuntu:18.04

RUN apt update \
    && apt-get install software-properties-common -y \
    && apt-add-repository ppa:git-core/ppa -y \
    && apt install -y jq openjdk-11-jdk git wget curl \
    && curl -sL https://deb.nodesource.com/setup_12.x | bash - \
    && apt install -y nodejs \
    && apt remove software-properties-common -y \
    && apt autoremove -y \
    && apt autoclean

COPY ci.sh /usr/bin/ci

RUN chmod +x /usr/bin/ci

ENTRYPOINT ["/usr/bin/ci"]
