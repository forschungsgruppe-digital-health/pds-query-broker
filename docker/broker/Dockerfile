# Build stage — Gradle version matches the repo's wrapper (gradle/wrapper/).
FROM gradle:9.6.1-jdk21 AS build

WORKDIR /workspace
COPY . .
RUN gradle :broker:bootJar --no-daemon

# eclipse-temurin JRE (multi-arch; the alpine variant has no arm64 manifest)
FROM eclipse-temurin:25-jre

LABEL maintainer="Query Broker Project"
LABEL description="Query Broker Service - FHIR messaging over AMQP"

WORKDIR /app

COPY --from=build /workspace/broker/build/libs/broker-*.jar app.jar

EXPOSE 8080 8081

HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
    CMD bash -c 'exec 3<>/dev/tcp/localhost/8081' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
