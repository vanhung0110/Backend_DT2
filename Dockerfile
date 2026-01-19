# Multi-stage build for the hungdt2 Spring Boot application

# 1) Builder stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace
# Copy only what is necessary for Maven to use the cache well
COPY pom.xml mvnw mvnw.cmd .mvn/ ./
COPY src ./src
RUN mvn -B -DskipTests package

# 2) Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS=""
# Copy jar from builder stage
COPY --from=builder /workspace/target/hungdt2-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-} -jar /app/app.jar"]
