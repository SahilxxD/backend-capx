# Use an official OpenJDK image as a parent image
FROM openjdk:17-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the jar file into the container
COPY target/your-spring-boot-app.jar /app/app.jar

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]

# Expose the port your Spring Boot app is running on
EXPOSE 8080
