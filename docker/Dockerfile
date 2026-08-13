ARG RUNTIME_IMAGE=container-registry.oracle.com/graalvm/jdk:25.0.4
FROM ${RUNTIME_IMAGE}

ARG JAR_FILE
ARG SERVICE_NAME
ARG RUNTIME_SETUP_COMMAND="true"

LABEL maintainer="guanxiangkai" \
      org.opencontainers.image.title="${SERVICE_NAME}" \
      org.opencontainers.image.vendor="AI Platform"

WORKDIR /app
RUN sh -c "${RUNTIME_SETUP_COMMAND}"
COPY ${JAR_FILE} /app/application.jar

ENV LANG=C.utf8 \
    LC_ALL=C.utf8 \
    TZ=Asia/Shanghai \
    SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=25 -XX:MaxRAMPercentage=70 --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow"

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
    CMD curl --fail --silent --show-error "http://localhost:${SERVER_PORT}/actuator/health" >/dev/null || exit 1
USER 10001:10001
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/application.jar"]
