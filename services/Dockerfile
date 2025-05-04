# services/api/Dockerfile

# 1. Build stage
FROM maven:3.8.7-eclipse-temurin-17 AS build
WORKDIR /services
# Copy pom first to leverage Docker cache
COPY pom.xml .
# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -DskipDocker

# 2. Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /services
# Copy the Spring Boot fat JAR
COPY --from=build /services/target/*.jar app.jar
# Expose the HTTP port
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/services/app.jar"]