# Use OpenJDK as the base image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the built Spring Boot JAR file into the container
COPY target/container1-0.0.1-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 6000

# Run the Spring Boot application
CMD ["java", "-jar", "app.jar"]
