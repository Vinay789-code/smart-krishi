# ===================================================================
# Dockerfile for Smart Krishi – Precision Agriculture Platform
# Optimized Multi-Stage Build for Render Cloud Deployment
# ===================================================================

# Stage 1: Build JAR with Maven & Java 17
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:resolve -B

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Production JRE Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Security: Non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
