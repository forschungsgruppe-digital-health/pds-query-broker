# Build stage — Gradle version matches the repo's wrapper (gradle/wrapper/).
FROM gradle:8.14.2-jdk21 AS build

WORKDIR /workspace
COPY . .
RUN gradle :connectors:pds-example:bootJar --no-daemon

# eclipse-temurin JRE (multi-arch; the alpine variant has no arm64 manifest)
FROM eclipse-temurin:21-jre

LABEL maintainer="Query Broker Project"
LABEL description="PDS example connector - synthetic reference implementation"

WORKDIR /app

COPY --from=build /workspace/connectors/pds-example/build/libs/pds-example-*.jar app.jar

EXPOSE 8082 8083

HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
    CMD bash -c 'exec 3<>/dev/tcp/localhost/8083' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
