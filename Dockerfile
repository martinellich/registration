FROM azul/zulu-openjdk-alpine:21.0.1

VOLUME /tmp

COPY target/*.jar app.jar

RUN apk add --no-cache msttcorefonts-installer fontconfig
RUN update-ms-fonts

ENTRYPOINT ["java", "-jar", "app.jar"]
