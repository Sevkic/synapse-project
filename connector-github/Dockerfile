FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy the Maven wrapper and pom files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY synapse-core/pom.xml synapse-core/
COPY connector-github/pom.xml connector-github/

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY synapse-core/src synapse-core/src
COPY connector-github/src connector-github/src

# Build the application
RUN ./mvnw clean package -pl connector-github -am -DskipTests -B

# Create runtime image
FROM eclipse-temurin:21-jre

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=0 /app/connector-github/target/connector-github-*.jar app.jar

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]