# services/api/Dockerfile

# Stage 1: Build only the services module
FROM maven:3.8.4-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY . .
RUN mvn clean package -pl services -am -DskipTests

# Stage 2: Run the service
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/services/target/services-*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]