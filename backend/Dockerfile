# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Lightweight runtime
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=build --chown=appuser:appgroup /app/target/ecommerce-0.0.1-SNAPSHOT.jar app.jar

# Ensure upload directory can be created and written by appuser
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app/uploads
VOLUME /app/uploads

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
