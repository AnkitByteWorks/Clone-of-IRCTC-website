# ==========================================
# Stage 1: Build Java 25 Backend
# ==========================================
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /build

# Copy Maven wrapper and pom.xml first for efficient dependency layer caching
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Ensure wrapper script is executable and resolve dependencies
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy project source code
COPY src src

# Package application jar (skipping tests since CI runs them with Testcontainers)
RUN ./mvnw clean package -DskipTests -B

# ==========================================
# Stage 2: Runtime Image
# ==========================================
FROM eclipse-temurin:25-jre

WORKDIR /app

# Create non-root system user for security
RUN groupadd -r railconnect && useradd -r -g railconnect -d /app -s /sbin/nologin railconnect

# Copy compiled spring boot fat jar from builder
COPY --from=builder /build/target/*.jar app.jar

# Set ownership
RUN chown -R railconnect:railconnect /app

USER railconnect

# Expose Spring Boot web port
EXPOSE 8080

# Configure JVM flags optimized for containers & virtual threads
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
