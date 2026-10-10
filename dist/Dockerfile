# BusGuard AI — Multi-Stage Dockerfile for Production Deployment
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy project source files
COPY . .

# Compile Java source files to bin directory
RUN mkdir -p bin && javac -d bin $(find src/main/java -name "*.java")

# Package into executable JAR stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy compiled classes, resources, and data
COPY --from=builder /app/bin /app/bin
COPY data /app/data

EXPOSE 8080

# Set Groq API Key environment variable if provided
ENV GROQ_API_KEY="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"

CMD ["java", "-cp", "bin", "com.busguard.BusGuardApp"]
