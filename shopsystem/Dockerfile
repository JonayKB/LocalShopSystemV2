# Etapa de compilación
FROM maven:3.9.2-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app_localShop.jar
EXPOSE 56784
ENTRYPOINT ["java", "-jar", "app_localShop.jar"]
