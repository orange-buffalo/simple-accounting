FROM openjdk:8-jre-alpine

RUN mkdir -p /opt/app
COPY app.jar /opt/app/app.jar

EXPOSE 9393

ENTRYPOINT ["java", \
            "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", \
            "-Xmx128m", \
            "-jar", "/opt/app/app.jar", \
           "--spring.config.additional-location=file:/data/config/" \
            ]