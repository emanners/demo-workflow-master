# services/worker/Dockerfile

# 1. Build stage
FROM maven:3.8.7-eclipse-temurin-17 AS build
WORKDIR /worker
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -DskipDocker

# 2. Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /worker
COPY --from=build /worker/target/*.jar worker.jar
# No ports exposed (worker is background)
ENTRYPOINT ["java", "-jar", "/worker/worker.jar"]
