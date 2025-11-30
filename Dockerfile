# Stage 1: Build
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copy Maven configuration
COPY pom.xml .

# Copy source code
COPY src ./src

# Build the application (skip tests for Docker build)
RUN apt-get update && apt-get install -y maven && \
    mvn clean package -DskipTests && \
    # Copy the shaded application JAR (with dependencies) to a stable name
    cp target/discord-currency-bot-*.jar app.jar

# Stage 2: Runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/app.jar app.jar

# Create non-root user for security
RUN groupadd -r botuser && useradd -r -g botuser botuser
USER botuser

# Set default environment variables
ENV DISCORD_BOT_TOKEN=""
ENV DB_URL="jdbc:postgresql://postgres:5432/currency_bot"
ENV DB_USERNAME="postgres"
ENV DB_PASSWORD="postgres"

# Run the bot
ENTRYPOINT ["java", "-jar", "app.jar"]
