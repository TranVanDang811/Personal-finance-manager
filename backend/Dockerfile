# =====================================================================
# Stage 1: Build Spring Boot executable JAR
# =====================================================================
FROM maven:3.9.9-eclipse-temurin-22-alpine AS builder
WORKDIR /build

COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# =====================================================================
# Stage 2: Minimal non-root Java 22 runtime
# =====================================================================
FROM eclipse-temurin:22-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /app/uploads/avatars /app/uploads/attachments \
    && chown -R appuser:appgroup /app

COPY --from=builder --chown=appuser:appgroup /build/target/personal-finance-manager-*.jar /app/app.jar

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENV SERVER_PORT=8080
ENV UPLOAD_DIR=/app/uploads

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/v1/health >/dev/null 2>&1 || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

