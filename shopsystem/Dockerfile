FROM openjdk:17-jdk-slim
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app_localShop.jar
COPY uploads /uploads
EXPOSE 8080
ENTRYPOINT [ "java", "-jar", "app_localShop.jar" ]