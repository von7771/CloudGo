# 多阶段构建：docker build --build-arg MODULE=gateway -t carpool/gateway:latest -f deploy/docker/Dockerfile .
ARG MODULE=gateway
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /src
COPY pom.xml .
COPY common common
COPY gateway gateway
COPY passenger-service passenger-service
COPY driver-service driver-service
COPY trip-service trip-service
COPY admin-service admin-service
COPY ai-assistant-service ai-assistant-service
RUN mvn -pl ${MODULE} -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
ARG MODULE
WORKDIR /app
COPY --from=build /src/${MODULE}/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
