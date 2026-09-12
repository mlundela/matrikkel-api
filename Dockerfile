# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Stage 1 - build the Spring Boot jar.
#
# This stage needs outbound network access to prodtest.matrikkel.no: the
# jaxws-maven-plugin downloads the two WSDLs during generate-sources and
# generates the no.statkart.* classes that the application code imports.
# Without it the compile fails - it is not a cache problem.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build

WORKDIR /build

# Resolve dependencies first so that a source-only change reuses this layer.
# The repo is kept inside the image (not a BuildKit cache mount) because GitHub
# Actions' registry cache restores layers but not cache mounts - this way CI
# re-downloads dependencies only when pom.xml changes. Non-fatal: it is purely a
# warm-up, and `package` below re-resolves anything it missed.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp -Dmaven.repo.local=/build/.m2 dependency:go-offline || true

COPY src/ src/
RUN ./mvnw -B -ntp -Dmaven.repo.local=/build/.m2 clean package -DskipTests

# Split the jar into layers: dependencies change far less often than our code,
# so the big layer stays cached across releases.
RUN java -Djarmode=tools -jar target/matrikkel-api-*.jar \
        extract --layers --launcher --destination extracted

# ---------------------------------------------------------------------------
# Stage 2 - runtime. JRE only, no build tooling, no source, no credentials.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime

# curl is only here for the container healthcheck below.
RUN apk add --no-cache curl \
 && addgroup -S app \
 && adduser -S -G app -h /app app

WORKDIR /app

COPY --from=build --chown=app:app /build/extracted/dependencies/ ./
COPY --from=build --chown=app:app /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /build/extracted/application/ ./

USER app

# MaxRAMPercentage lets the JVM size its heap from the container memory limit.
ENV SERVER_PORT=8081 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8081

# Matrikkel SOAP credentials are supplied at run time, never baked in:
#   docker run -e MATRIKKEL_WS_USERNAME=... -e MATRIKKEL_WS_PASSWORD=... ...
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -fsS "http://localhost:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
