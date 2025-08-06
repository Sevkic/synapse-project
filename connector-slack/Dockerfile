FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy the Maven wrapper and pom files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY synapse-core/pom.xml synapse-core/
COPY connector-slack/pom.xml connector-slack/

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY synapse-core/src synapse-core/src
COPY connector-slack/src connector-slack/src

# Build the application
RUN ./mvnw clean package -pl connector-slack -am -DskipTests

# Create runtime image
FROM openjdk:21-jre-slim

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=0 /app/connector-slack/target/connector-slack-*.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]