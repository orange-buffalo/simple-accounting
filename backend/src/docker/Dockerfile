FROM adoptopenjdk/openjdk11:alpine-jre

RUN apk --no-cache add curl

RUN mkdir -p /opt/app \
    && mkdir -p /data \
    && addgroup -g 2000 -S app \
    && adduser -u 2000 -S -G app app \
    && chown app:app /data \
    && chown app:app /opt/app

USER app:app

COPY app.jar /opt/app/app.jar

EXPOSE 9393

ENTRYPOINT ["java", \
            "-Xmx128m", \
            "-jar", "/opt/app/app.jar" \
            ]

HEALTHCHECK --start-period=10s --interval=3s --timeout=10s --retries=10 \
        CMD curl --silent --fail --request GET http://localhost:9393/actuator/health 2>&1 | grep UP || exit 1
