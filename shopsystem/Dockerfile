FROM openjdk:17-jdk-slim
ARG JAR_FILE=target/shopsysten-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app_localShop.jar
EXPOSE 25565
ENTRYPOINT [ "java", "-jar", "app_localShop.jar" ]