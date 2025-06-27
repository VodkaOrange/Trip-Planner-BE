# Stage 1: Build the JAR
FROM gradle:8.10-jdk21 AS builder
WORKDIR /app
COPY build.gradle ./
COPY settings.gradle* ./
COPY src ./src
RUN gradle build --no-daemon

# Stage 2: Create the runtime image
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/Trip-Planner-BE-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]