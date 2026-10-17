FROM maven:3.9.16-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package


FROM eclipse-temurin:21.0.12_8-jre-alpine-3.24

RUN addgroup -S escaparate \
    && adduser -S escaparate -G escaparate

WORKDIR /app

RUN mkdir -p /data/uploads \
    && chown -R escaparate:escaparate /data/uploads

COPY --from=build --chown=escaparate:escaparate \
    /app/target/escaparate.war /app/app.war

ENV APP_STORAGE_PATH=/data/uploads

USER escaparate

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.war"]