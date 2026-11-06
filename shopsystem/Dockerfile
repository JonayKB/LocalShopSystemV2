# ---------- Compilación ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
# Primero solo el pom: las dependencias quedan en caché mientras no cambie
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# Los tests (con MySQL en Docker, Testcontainers) se ejecutan en CI antes de construir la imagen
RUN mvn -q -B -DskipTests package

# ---------- Ejecución ----------
# JRE (no JDK): imagen más pequeña y con menos superficie de ataque
FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/*.jar app_localShop.jar
RUN mkdir -p /app/uploads
# Se ejecuta como root a propósito: la app puede servir HTTPS con las claves de Let's Encrypt
# montadas desde el host, que solo puede leer root. Si el TLS lo termina el proxy del host
# (SERVER_SSL_ENABLED=false), se puede añadir un usuario sin privilegios.
EXPOSE 56784
# Sano = /actuator/health responde UP (por HTTPS o HTTP, según SERVER_SSL_ENABLED)
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsk "https://localhost:${SERVER_PORT:-56784}/kiosco/actuator/health" \
     || curl -fs "http://localhost:${SERVER_PORT:-56784}/kiosco/actuator/health" || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app_localShop.jar"]
