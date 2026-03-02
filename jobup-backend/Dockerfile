# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create uploads directory
RUN mkdir -p uploads/job_attachment uploads/profile_picture

# Copy JAR file from build stage
COPY --from=build /app/target/JobUp-0.0.1-SNAPSHOT.jar app.jar

# Copy uploads directory
COPY --from=build /app/uploads ./uploads

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xmx512m -Xms256m"

# Expose port
EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8083/JobUp/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

