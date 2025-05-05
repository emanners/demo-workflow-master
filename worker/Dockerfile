# worker/Dockerfile

# ── Build stage ──────────────────────────────────────────────────────────────
FROM maven:3.8.4-eclipse-temurin-17 AS builder
WORKDIR /workspace

# 1. Copy the *entire* repo in, so the root POM and all modules are present
COPY . .

# 2. Build only the `worker` sub-module (and anything it depends on)
#    -pl = build *this* module
#    -am = also build its module dependencies
RUN mvn clean package -pl worker -am -DskipTests

# ── Run stage ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

# 3. Copy in the built JAR from the builder
COPY --from=builder /workspace/worker/target/worker-*.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]

