FROM azul/zulu-openjdk-alpine:25-jre

COPY target/*.jar app.jar

# Apache POI needs real fonts for sheet.autoSizeColumn() in the Excel export
RUN apk add --no-cache msttcorefonts-installer fontconfig
RUN update-ms-fonts

# Timestamps (LocalDateTime.now()) must be in Swiss local time, the Fly machines run in UTC
ENTRYPOINT ["java", "-Duser.timezone=Europe/Zurich", "-jar", "app.jar"]
