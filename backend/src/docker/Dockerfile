FROM adoptopenjdk/openjdk11:alpine-jre

RUN apk --no-cache add curl

RUN mkdir -p /opt/app \
    && mkdir -p /data/config
COPY app.jar /opt/app/app.jar

EXPOSE 9393

ENTRYPOINT ["java", \
            "-Xmx128m", \
            "-jar", "/opt/app/app.jar", \
           "--spring.config.additional-location=file:/data/config/" \
            ]

HEALTHCHECK --start-period=10s --interval=3s --timeout=10s --retries=10 \
        CMD curl --silent --fail --request GET http://localhost:9393/actuator/health 2>&1 | grep UP || exit 1
