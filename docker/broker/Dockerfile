FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Query Broker Project"
LABEL description="Query Broker Service — FHIR messaging over AMQP"

WORKDIR /app

COPY build/libs/broker-*.jar app.jar

EXPOSE 8080 8081

HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
    CMD wget -qO- http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
