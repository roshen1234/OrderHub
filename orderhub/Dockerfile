# syntax=docker/dockerfile:1

# ============================================================
# Stage 1: build the jar
# ============================================================
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Copy only the files Maven needs to download dependencies. Docker caches this layer, so the
# (slow) download is skipped on later builds unless pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
# If mvnw was saved with Windows line endings, Linux cannot run it. This makes it safe either way.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -B -q dependency:go-offline

# Now the source code. Tests are skipped here: they need Docker themselves (Testcontainers),
# so they run in the CI pipeline instead.
COPY src src
RUN ./mvnw -B -q -DskipTests package

# ============================================================
# Stage 2: a small runtime image (JRE only, no Maven, no source)
# ============================================================
FROM eclipse-temurin:25-jre
WORKDIR /app

# curl is used by the health check below. Run as a normal user, not root.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --uid 1001 spring
COPY --from=build /workspace/target/*.jar app.jar
USER spring

EXPOSE 8080

# "healthy" means the app answers /actuator/health with 200.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fs http://localhost:8080/actuator/health || exit 1

# MaxRAMPercentage lets the JVM size its memory from the container's limit, not the host's.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
