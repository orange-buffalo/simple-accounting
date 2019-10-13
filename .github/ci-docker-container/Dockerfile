FROM ubuntu:18.04

RUN apt update \
    && apt install -y jq openjdk-8-jdk git wget curl \
    && curl -sL https://deb.nodesource.com/setup_12.x | bash - \
    && apt install -y nodejs \
    && apt autoremove \
    && apt autoclean

COPY ci.sh /usr/bin/ci

RUN chmod +x /usr/bin/ci

ENTRYPOINT ["/usr/bin/ci"]