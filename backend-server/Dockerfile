# =============================================================================
# Multi-stage Dockerfile for Finance Tracker Backend
# =============================================================================
# Stage 1 – Build: compile and package the jar using Maven wrapper
# Stage 2 – Run:   copy only the jar into a slim JRE image
# =============================================================================

# ---- Build Stage ----
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Dependencies are re-downloaded only when pom.xml changes.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build the fat jar (skip tests; they run in CI)
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ---- Run Stage ----
FROM eclipse-temurin:21-jre-alpine AS run

WORKDIR /app

# Run as a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
